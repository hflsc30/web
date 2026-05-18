package com.base.config.redisson;

import com.base.util.RedissonUtil;
import com.base.util.StrUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 自定义 Spring 缓存管理器，支持 Caffeine L1 + Redis L2 两级缓存。
 * <p>
 * 缓存名称支持多参数：#ttl|enableL1|dataType|maxIdle|maxSize
 *
 * @author base
 */
@Slf4j
@RequiredArgsConstructor
public class MySpringCacheManager implements CacheManager, ApplicationContextAware, InitializingBean {

    private static final int MAX_SPEL_CACHE_SIZE = 1000;
    private static final int MAX_BEAN_CACHE_SIZE = 100;
    private static final String CACHE_CONFIG_DELIMITER = "\\|";

    private final MyCacheConfig myCacheConfig;

    private ApplicationContext applicationContext;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    private volatile BeanResolver beanResolver;
    private final Object beanResolverLock = new Object();

    private final com.github.benmanes.caffeine.cache.Cache<String, String> spelCacheNameCache =
            Caffeine.newBuilder()
                    .maximumSize(MAX_SPEL_CACHE_SIZE)
                    .expireAfterWrite(Duration.ofHours(1))
                    .build();

    private final com.github.benmanes.caffeine.cache.Cache<String, Expression> compiledExpressions =
            Caffeine.newBuilder()
                    .maximumSize(MAX_SPEL_CACHE_SIZE)
                    .build();

    private final com.github.benmanes.caffeine.cache.Cache<String, Object> cachedBeans =
            Caffeine.newBuilder()
                    .maximumSize(MAX_BEAN_CACHE_SIZE)
                    .build();

    private static final ThreadLocal<Object[]> METHOD_ARGS = ThreadLocal.withInitial(() -> new Object[0]);
    private static final ThreadLocal<Method> CURRENT_METHOD = new ThreadLocal<>();

    private volatile boolean beansInitialized;

    @Getter
    private boolean dynamic = true;

    @Setter
    private boolean allowNullValues = true;

    @Setter
    private boolean transactionAware = true;

    @Getter
    @Setter
    private boolean globalL1CacheEnabled = true;

    private final ConcurrentMap<String, CacheConfig> configMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Cache> instanceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        this.beansInitialized = true;
        if (applicationContext != null) {
            getBeanResolver();
            preloadEssentialBeans();
        }
        MyCacheConfig.RedisSpec redisSpec = myCacheConfig.getRedis();
        this.dynamic = redisSpec.isDynamic();
        this.allowNullValues = redisSpec.isAllowNullValues();
        this.transactionAware = redisSpec.isTransactionAware();
        this.globalL1CacheEnabled = redisSpec.isGlobalL1CacheEnabled();
        log.info("Cache Manager initialized: dynamic={}, allowNullValues={}, transactionAware={}, globalL1CacheEnabled={}",
                dynamic, allowNullValues, transactionAware, globalL1CacheEnabled);
    }

    private BeanResolver getBeanResolver() {
        if (beanResolver == null) {
            synchronized (beanResolverLock) {
                if (beanResolver == null && applicationContext != null) {
                    beanResolver = new BeanFactoryResolver(applicationContext);
                }
            }
        }
        return beanResolver;
    }

    private void preloadEssentialBeans() {
        String[] essentialBeanNames = {"securityProperties", "myCacheConfig"};
        for (String beanName : essentialBeanNames) {
            try {
                if (applicationContext.containsBean(beanName)) {
                    cachedBeans.put(beanName, applicationContext.getBean(beanName));
                }
            } catch (Exception e) {
                log.debug("Skip preload bean {}: {}", beanName, e.getMessage());
            }
        }
    }

    public void setCacheNames(@Nullable Collection<String> names) {
        if (names != null) {
            for (String name : names) {
                getCache(name);
            }
            dynamic = false;
        } else {
            dynamic = true;
        }
    }

    public void setConfig(@Nullable Map<String, ? extends CacheConfig> config) {
        if (config != null) {
            this.configMap.putAll(config);
        }
    }

    @NonNull
    protected CacheConfig createDefaultConfig() {
        CacheConfig config = new CacheConfig();
        MyCacheConfig.RedisSpec redisSpec = myCacheConfig.getRedis();
        if (redisSpec.getDefaultTTL() > 0) {
            config.setTtl(redisSpec.getDefaultTTL());
        }
        if (redisSpec.getDefaultMaxIdleTime() > 0) {
            config.setMaxIdleTime(redisSpec.getDefaultMaxIdleTime());
        }
        if (redisSpec.getDefaultMaxSize() > 0) {
            config.setMaxSize(redisSpec.getDefaultMaxSize());
        }
        return config;
    }

    // region SpEL

    /**
     * 设置当前方法上下文（由缓存拦截器调用）。
     * 调用方应在 finally 块中调用 {@link #clearMethodContext()} 防止 ThreadLocal 泄漏。
     */
    public static void setMethodContext(Method method, Object[] args) {
        CURRENT_METHOD.set(method);
        METHOD_ARGS.set(args);
    }

    public static void clearMethodContext() {
        CURRENT_METHOD.remove();
        METHOD_ARGS.remove();
    }

    private StandardEvaluationContext createEvaluationContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(getBeanResolver());
        registerCachedBeans(context);
        addMethodParametersToContext(context);
        return context;
    }

    private void addMethodParametersToContext(StandardEvaluationContext context) {
        Object[] args = METHOD_ARGS.get();
        Method method = CURRENT_METHOD.get();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
            }
            if (method != null) {
                try {
                    Parameter[] parameters = method.getParameters();
                    for (int i = 0; i < parameters.length && i < args.length; i++) {
                        String paramName = parameters[i].getName();
                        if (!paramName.startsWith("arg")) {
                            context.setVariable(paramName, args[i]);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to resolve parameter names for method: {}", method.getName());
                }
            }
        }
    }

    private void registerCachedBeans(StandardEvaluationContext context) {
        try {
            for (Map.Entry<String, Object> entry : cachedBeans.asMap().entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            log.debug("Failed to register cached beans: {}", e.getMessage());
        }
    }

    private String parseSpelCacheNameSafely(String cacheName) {
        if (StrUtil.isBlank(cacheName) || !cacheName.contains("#{")) {
            return cacheName;
        }
        String cached = spelCacheNameCache.getIfPresent(cacheName);
        if (cached != null) {
            return cached;
        }
        if (!beansInitialized || applicationContext == null) {
            log.warn("Beans not fully initialized, using original cache name: {}", cacheName);
            return cacheName;
        }
        try {
            String resolved = parseSpelExpression(cacheName);
            spelCacheNameCache.put(cacheName, resolved);
            return resolved;
        } catch (Exception e) {
            log.warn("Failed to parse SpEL in cache name: {}, error: {}", cacheName, e.getMessage());
            spelCacheNameCache.put(cacheName, cacheName);
            return cacheName;
        }
    }

    private String evaluateSpelExpression(String spelExpression) {
        try {
            Expression expression = compiledExpressions.asMap()
                    .computeIfAbsent(spelExpression, expressionParser::parseExpression);
            StandardEvaluationContext context = createEvaluationContext();
            Object value = expression.getValue(context);
            String result = value != null ? value.toString() : "";
            if (log.isDebugEnabled()) {
                log.debug("SpEL evaluation: '{}' → '{}'", spelExpression, result);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to evaluate SpEL: '{}', error: {}", spelExpression, e.getMessage());
            return "#{" + spelExpression + "}";
        }
    }

    private String parseSpelExpression(String cacheName) {
        String result = cacheName;
        int start = 0;
        while ((start = result.indexOf("#{", start)) != -1) {
            int end = findMatchingBrace(result, start + 2);
            if (end == -1) {
                log.warn("Unmatched brace in SpEL: {}", result);
                break;
            }
            String spelExpr = result.substring(start + 2, end);
            String replacement = evaluateSpelExpression(spelExpr);
            result = result.substring(0, start) + replacement + result.substring(end + 1);
            start += replacement.length();
        }
        return result;
    }

    private int findMatchingBrace(String str, int startPos) {
        int braceCount = 1;
        boolean inString = false;
        char stringDelimiter = 0;
        for (int i = startPos; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!inString && (c == '\'' || c == '"')) {
                inString = true;
                stringDelimiter = c;
                continue;
            }
            if (inString) {
                if (c == stringDelimiter && str.charAt(i - 1) != '\\') {
                    inString = false;
                }
                continue;
            }
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void clearSpelCache() {
        spelCacheNameCache.cleanUp();
        compiledExpressions.cleanUp();
        cachedBeans.cleanUp();
        if (beansInitialized) {
            preloadEssentialBeans();
        }
        log.info("Cleared SpEL expression cache");
    }

    public Map<String, Object> getSpelCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("spelCacheSize", spelCacheNameCache.estimatedSize());
        stats.put("compiledExpressionsSize", compiledExpressions.estimatedSize());
        stats.put("cachedBeansSize", cachedBeans.estimatedSize());
        stats.put("beansInitialized", beansInitialized);
        stats.put("beanResolverInitialized", beanResolver != null);
        return stats;
    }

    public void refreshCachedBeans() {
        cachedBeans.cleanUp();
        if (beansInitialized) {
            preloadEssentialBeans();
            log.info("Refreshed cached beans");
        }
    }

    // endregion

    // region Cache 创建

    private Cache createCacheByDataType(String cacheName, CacheConfig config, boolean enableL1Cache, String dataType) {
        if (StrUtil.isBlank(dataType)) {
            if (config.getMaxIdleTime() == 0 && config.getTtl() == 0 && config.getMaxSize() == 0) {
                return createMap(cacheName, config, enableL1Cache);
            }
            return createMapCache(cacheName, config, enableL1Cache);
        }
	    return switch (dataType.toLowerCase()) {
		    case "string" -> createStringCache(cacheName, config, enableL1Cache);
		    case "list" -> createListCache(cacheName, config, enableL1Cache);
		    case "set" -> createSetCache(cacheName, config, enableL1Cache);
		    case "hash", "map" -> {
			    if (config.getMaxIdleTime() == 0 && config.getTtl() == 0 && config.getMaxSize() == 0) {
				    yield createMap(cacheName, config, enableL1Cache);
			    }
			    yield createMapCache(cacheName, config, enableL1Cache);
		    }
		    case "zset", "sortedset" -> createSortedSetCache(cacheName, config, enableL1Cache);
		    default -> {
			    log.warn("Unsupported data type: {}, falling back to Map", dataType);
			    yield createMap(cacheName, config, enableL1Cache);
		    }
	    };
    }

    private Cache createStringCache(String cacheName, CacheConfig config, boolean enableL1Cache) {
        Cache cache = new StringCache(cacheName, config, allowNullValues);
        cache = applyDecorators(cacheName, cache, enableL1Cache);
        Cache existing = instanceMap.putIfAbsent(cacheName, cache);
        if (existing != null) {
            return existing;
        }
        log.debug("Created String cache: {}, TTL: {}ms, L1: {}", cacheName, config.getTtl(), enableL1Cache);
        return cache;
    }

    private Cache createListCache(String cacheName, CacheConfig config, boolean enableL1Cache) {
        org.redisson.api.RList<Object> list = RedissonUtil.getRawClient().getList(cacheName);
        Cache cache = new ListCache(list, config, allowNullValues);
        cache = applyDecorators(cacheName, cache, enableL1Cache);
        Cache existing = instanceMap.putIfAbsent(cacheName, cache);
        if (existing != null) {
            return existing;
        }
        log.debug("Created List cache: {}, TTL: {}ms, L1: {}", cacheName, config.getTtl(), enableL1Cache);
        return cache;
    }

    private Cache createSetCache(String cacheName, CacheConfig config, boolean enableL1Cache) {
        org.redisson.api.RSet<Object> set = RedissonUtil.getRawClient().getSet(cacheName);
        Cache cache = new SetCache(set, config, allowNullValues);
        cache = applyDecorators(cacheName, cache, enableL1Cache);
        Cache existing = instanceMap.putIfAbsent(cacheName, cache);
        if (existing != null) {
            return existing;
        }
        log.debug("Created Set cache: {}, TTL: {}ms, L1: {}", cacheName, config.getTtl(), enableL1Cache);
        return cache;
    }

    private Cache createSortedSetCache(String cacheName, CacheConfig config, boolean enableL1Cache) {
        org.redisson.api.RScoredSortedSet<Object> sortedSet = RedissonUtil.getRawClient().getScoredSortedSet(cacheName);
        Cache cache = new SortedSetCache(sortedSet, config, allowNullValues);
        cache = applyDecorators(cacheName, cache, enableL1Cache);
        Cache existing = instanceMap.putIfAbsent(cacheName, cache);
        if (existing != null) {
            return existing;
        }
        log.debug("Created SortedSet cache: {}, TTL: {}ms, L1: {}", cacheName, config.getTtl(), enableL1Cache);
        return cache;
    }

    private Cache createMap(String name, CacheConfig config, boolean enableL1Cache) {
        org.redisson.api.RMap<Object, Object> map = RedissonUtil.getRawClient().getMap(name);
        Cache cache = new RedissonCache(map, allowNullValues);
        cache = applyDecorators(name, cache, enableL1Cache);
        Cache existing = instanceMap.putIfAbsent(name, cache);
        if (existing != null) {
            return existing;
        }
        log.debug("Created Map cache: {}, L1: {}", name, enableL1Cache);
        return cache;
    }

    private Cache createMapCache(String name, CacheConfig config, boolean enableL1Cache) {
        org.redisson.api.RMapCache<Object, Object> mapCache = RedissonUtil.getRawClient().getMapCache(name);
        Cache cache = new RedissonCache(mapCache, config, allowNullValues);
        cache = applyDecorators(name, cache, enableL1Cache);
        Cache existing = instanceMap.putIfAbsent(name, cache);
        if (existing != null) {
            return existing;
        }
        if (config.getMaxSize() > 0) {
            mapCache.setMaxSize(config.getMaxSize());
        }
        log.debug("Created MapCache: {}, TTL: {}ms, MaxIdle: {}ms, MaxSize: {}, L1: {}",
                name, config.getTtl(), config.getMaxIdleTime(), config.getMaxSize(), enableL1Cache);
        return cache;
    }

    private Cache applyDecorators(String name, Cache cache, boolean enableL1Cache) {
        if (enableL1Cache && myCacheConfig.isEnabled()) {
            try {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> local =
                        myCacheConfig.createCaffeineCache(name);
                cache = new CaffeineCacheDecorator(name, cache, local, true);
                log.debug("Applied Caffeine L1 cache: {}", name);
            } catch (Exception e) {
                log.warn("Failed to create Caffeine L1 cache: {}, falling back to L2 only", name, e);
            }
        }
        if (transactionAware) {
            cache = new TransactionAwareCacheDecorator(cache);
        }
        return cache;
    }

    // endregion

    // region CacheManager

    @Override
    @Nullable
    public Cache getCache(@NonNull String name) {
        String resolvedName = parseSpelCacheNameSafely(name);
        Cache cache = instanceMap.get(resolvedName);
        if (cache != null) {
            return cache;
        }
        if (!dynamic) {
            return null;
        }
        return createCacheWithResolvedName(resolvedName, name);
    }

    private Cache createCacheWithResolvedName(String resolvedName, String originalName) {
        try {
            String[] parts = resolvedName.split(CACHE_CONFIG_DELIMITER);
            String cacheName = parts[0];
            String ttlStr = parts.length > 1 ? parts[1] : null;
            String enableL1Str = parts.length > 2 ? parts[2] : null;
            String dataTypeStr = parts.length > 3 ? parts[3] : null;
            String maxIdleStr = parts.length > 4 ? parts[4] : null;
            String maxSizeStr = parts.length > 5 ? parts[5] : null;

            CacheConfig config = buildCacheConfig(cacheName, ttlStr, maxIdleStr, maxSizeStr);
            boolean enableL1 = parseL1Flag(enableL1Str);
            Cache cache = createCacheByDataType(cacheName, config, enableL1, dataTypeStr);
            Cache existing = instanceMap.putIfAbsent(resolvedName, cache);
            if (existing != null) {
                return existing;
            }
            log.debug("Created cache: {} (original: {}), TTL: {}ms, L1: {}",
                    resolvedName, originalName, config.getTtl(), enableL1);
            return cache;
        } catch (Exception e) {
            log.error("Failed to create cache: {} (original: {})", resolvedName, originalName, e);
            return null;
        }
    }

    private CacheConfig buildCacheConfig(String cacheName, String ttlStr, String maxIdleStr, String maxSizeStr) {
        CacheConfig config = configMap.getOrDefault(cacheName, createDefaultConfig());
        if (StrUtil.isNotBlank(ttlStr)) {
            try {
                config.setTtl(Long.parseLong(ttlStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid TTL: {}", ttlStr);
            }
        }
        if (StrUtil.isNotBlank(maxIdleStr)) {
            try {
                config.setMaxIdleTime(Long.parseLong(maxIdleStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid MaxIdleTime: {}", maxIdleStr);
            }
        }
        if (StrUtil.isNotBlank(maxSizeStr)) {
            try {
                config.setMaxSize(Integer.parseInt(maxSizeStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid MaxSize: {}", maxSizeStr);
            }
        }
        return config;
    }

    private boolean parseL1Flag(String flag) {
        if ("1".equals(flag) || "true".equalsIgnoreCase(flag) || "yes".equalsIgnoreCase(flag)) {
            return true;
        }
        if ("0".equals(flag) || "false".equalsIgnoreCase(flag) || "no".equalsIgnoreCase(flag)) {
            return false;
        }
        return globalL1CacheEnabled;
    }

    @Override
    @NonNull
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(configMap.keySet());
    }

    // endregion

    // region 管理

    public void clearAll() {
        for (Cache cache : instanceMap.values()) {
            try {
                cache.clear();
            } catch (Exception e) {
                log.warn("Error clearing cache: {}", cache.getName(), e);
            }
        }
        instanceMap.clear();
        log.info("Cleared all cache instances");
    }

    public void clearL1Cache(String cacheName) {
        Cache cache = instanceMap.get(cacheName);
        if (cache instanceof CaffeineCacheDecorator decorator) {
            decorator.clearL1Cache();
        }
    }

    public void clearAllL1Cache() {
        for (Cache cache : instanceMap.values()) {
            if (cache instanceof CaffeineCacheDecorator decorator) {
                decorator.clearL1Cache();
            }
        }
        log.info("Cleared all L1 caches");
    }

    public Map<String, String> getCacheStats() {
        Map<String, String> stats = new ConcurrentHashMap<>();
        for (Map.Entry<String, Cache> entry : instanceMap.entrySet()) {
            String cacheName = entry.getKey();
            Cache cache = entry.getValue();
            if (cache instanceof CaffeineCacheDecorator decorator) {
                stats.put(cacheName, decorator.getL1CacheStats());
            } else {
                stats.put(cacheName, "L2 only");
            }
        }
        return stats;
    }

    public int getCacheCount() {
        return instanceMap.size();
    }

    @Nullable
    public String getCacheInfo(String cacheName) {
        Cache cache = instanceMap.get(cacheName);
        if (cache == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Cache: ").append(cacheName).append("\n");
        sb.append("Type: ");
        if (cache instanceof CaffeineCacheDecorator decorator) {
            sb.append("L1+L2 (Caffeine+Redis)\n");
            sb.append("L1 Enabled: ").append(decorator.isL1CacheEnabled()).append("\n");
            sb.append("L1 Size: ").append(decorator.getL1CacheSize()).append("\n");
            sb.append("Stats: ").append(decorator.getL1CacheStats());
        } else {
            sb.append("L2 only (Redis)");
        }
        return sb.toString();
    }

    // endregion
}

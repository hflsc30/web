package com.base.config.redisson;

import com.base.util.StrUtil;
import org.redisson.config.NameMapper;

/**
 * redis缓存key前缀处理
 */
public class KeyPrefixHandler implements NameMapper {

	private final String keyPrefix;

	public KeyPrefixHandler(String keyPrefix) {
		this.keyPrefix = StrUtil.isBlank(keyPrefix) ? "" : keyPrefix + ":";
	}

	/**
	 * 增加前缀
	 *
	 * @param name key
	 * @return {@link String}
	 */
	@Override
	public String map(String name) {
		if (StrUtil.isBlank(name)) {
			return null;
		}

		// 如果键已经包含前缀，直接返回
		if (StrUtil.isNotBlank(keyPrefix) && name.startsWith(keyPrefix)) {
			return name;
		}

		// 对于包含冒号的复合键，保持其层级结构
		if (StrUtil.isNotBlank(keyPrefix)) {
			// 确保前缀以冒号结尾，以保持层级结构
			String prefix = keyPrefix.endsWith(":") ? keyPrefix : keyPrefix + ":";
			return prefix + name;
		}

		return name;

	}

	/**
	 * 去除前缀
	 *
	 * @param name key
	 * @return {@link String}
	 */
	@Override
	public String unmap(String name) {
		if (StrUtil.isBlank(name)) {
			return null;
		}
		if (StrUtil.isNotBlank(keyPrefix) && name.startsWith(keyPrefix)) {
			return name.substring(keyPrefix.length());
		}
		return name;
	}
}

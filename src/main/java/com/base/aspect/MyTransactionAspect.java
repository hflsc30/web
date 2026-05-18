package com.base.aspect;

import com.base.annotation.MyTransaction;
import com.base.result.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.tm.api.GlobalTransactionContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import java.lang.reflect.Method;

/**
 * @author base
 * @since 2026-05-15
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class MyTransactionAspect {

    @Pointcut("@annotation(com.base.annotation.MyTransaction)")
    public void myTransactionPointcut() {
    }

    @Around("myTransactionPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        MyTransaction annotation = method.getAnnotation(MyTransaction.class);

        String transactionName = getTransactionName(annotation, method);
        String xid = RootContext.getXID();

        if (xid == null) {
            log.warn("未找到 Seata 事务上下文，请确保外层有 @GlobalTransactional: {}", transactionName);
            return joinPoint.proceed();
        }

        log.info("开始执行事务方法: {}, XID: {}", transactionName, xid);

        try {
            Object result = joinPoint.proceed();

            xid = RootContext.getXID();
            if (xid != null && annotation.rollbackOnBusinessFail() && result instanceof R r) {
                if (!r.isSuccess()) {
                    log.warn("业务执行失败，手动回滚事务: {}, XID: {}, 失败原因: {}",
                            transactionName, xid, r.getMsg());

                    try {
                        GlobalTransactionContext.reload(xid).rollback();
                        RootContext.unbind();
                        log.info("事务手动回滚成功: {}, XID: {}", transactionName, xid);
                    } catch (TransactionException e) {
                        log.error("事务手动回滚失败: {}, XID: {}", transactionName, xid, e);
                        throw new RuntimeException("分布式事务手动回滚失败: " + transactionName, e);
                    }

                    return result;
                }
            }

            log.info("事务执行成功: {}, XID: {}", transactionName, xid);
            return result;

        } catch (Throwable throwable) {
            xid = RootContext.getXID();
            if (xid != null) {
                log.error("方法执行异常，由 @GlobalTransactional 自动回滚事务: {}, XID: {}",
                        transactionName, xid, throwable);
            }
            throw throwable;
        }
    }

    private String getTransactionName(MyTransaction annotation, Method method) {
        String transactionName = annotation.value();
        if (transactionName == null || transactionName.isEmpty()) {
            transactionName = annotation.name();
        }
        if (transactionName == null || transactionName.isEmpty()) {
            transactionName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }
        return transactionName;
    }
}

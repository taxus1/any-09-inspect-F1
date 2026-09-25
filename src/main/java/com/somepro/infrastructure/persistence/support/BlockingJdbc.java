package com.somepro.infrastructure.persistence.support;

import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 阻塞 JDBC 调用 → 响应式链路的桥接器（基础设施层共享）。
 *
 * 各仓储适配器原来各自持有一份一模一样的私有 blocking(...)，抽到这里避免重复。
 * 约定不变（见 README「响应式 × 阻塞 JDBC 桥接约定」）：
 * 1. 先在响应式线程上从 Reactor Context 取操作人（切线程后就取不到了）
 * 2. 再切到 boundedElastic 执行 JDBC（绝不能在 Netty event-loop 上跑 JDBC）
 * 3. 把操作人放进 AuditContextHolder，供 MetaObjectHandler 填充 createBy / updateBy
 */
public final class BlockingJdbc {

    private BlockingJdbc() {
    }

    public static <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}

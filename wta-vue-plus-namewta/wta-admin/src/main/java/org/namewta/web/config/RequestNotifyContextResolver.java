package org.namewta.web.config;

import org.namewta.common.notify.model.NotifyContext;
import org.namewta.common.notify.spi.NotifyContextResolver;
import org.namewta.system.api.model.LoginUser;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 在通知同步发送线程中快照当前认证来源和链路标识。
 */
public final class RequestNotifyContextResolver implements NotifyContextResolver {

    private final Supplier<LoginUser> loginUserSupplier;
    private final Supplier<String> traceIdSupplier;

    public RequestNotifyContextResolver(Supplier<LoginUser> loginUserSupplier, Supplier<String> traceIdSupplier) {
        this.loginUserSupplier = Objects.requireNonNull(loginUserSupplier, "loginUserSupplier");
        this.traceIdSupplier = Objects.requireNonNull(traceIdSupplier, "traceIdSupplier");
    }

    @Override
    public NotifyContext resolve() {
        LoginUser loginUser = loginUserSupplier.get();
        return new NotifyContext(loginUser == null ? null : loginUser.getUserId(),
            loginUser == null ? null : loginUser.getClientPk(), traceIdSupplier.get());
    }
}

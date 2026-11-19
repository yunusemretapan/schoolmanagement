package com.schoolos.core.application.behaviors;

import com.schoolos.core.application.mediator.PipelineBehavior;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The one place transactions start (docs/conventions.md, Known traps). A signed-in request is bound to the tenant
 * in its verified token, and only that one (BR-1, spec 0003 Req. 8). A public request starts unbound: Row-Level
 * Security then shows no tenant's rows until a handler binds one explicitly (login, after the password matched).
 */
public final class TransactionBehavior implements PipelineBehavior {

    private final TransactionPort transactions;
    private final TenantSessionPort tenantSession;

    public TransactionBehavior(TransactionPort transactions, TenantSessionPort tenantSession) {
        this.transactions = transactions;
        this.tenantSession = tenantSession;
    }

    @Override
    public <R> R handle(Request<R> request, Supplier<R> next) {
        return transactions.inTransaction(() -> {
            UUID tenantId =
                    CurrentUserHolder.current().map(CurrentUser::tenantId).orElse(null);
            if (tenantId != null) {
                tenantSession.bind(tenantId);
            }
            return next.get();
        });
    }
}

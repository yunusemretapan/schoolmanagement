package com.schoolos.core.application.behaviors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-7: a signed-in request runs bound to the tenant of its verified token, and only that one. */
class TransactionBehaviorTest {

    record Anything() implements Request<String> {}

    private static final UUID TENANT_A = UUID.fromString("01900000-0000-7000-8000-0000000000aa");

    private final List<String> events = new ArrayList<>();

    private final TransactionPort transactions = new TransactionPort() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            events.add("begin");
            try {
                T result = work.get();
                events.add("commit");
                return result;
            } catch (RuntimeException e) {
                events.add("rollback");
                throw e;
            }
        }
    };

    private final TenantSessionPort tenantSession = tenantId -> events.add("bind " + tenantId);
    private final TransactionBehavior behavior = new TransactionBehavior(transactions, tenantSession);

    @AfterEach
    void clearUser() {
        CurrentUserHolder.clear();
    }

    @Test
    void bindsTheTokenTenantInsideTheTransactionBeforeTheHandler() {
        CurrentUserHolder.set(new CurrentUser(
                UUID.randomUUID(), TENANT_A, UUID.randomUUID(), Set.of("ADMIN"), Set.of(Permission.SELF_READ)));

        behavior.handle(new Anything(), () -> {
            events.add("handle");
            return "ok";
        });

        assertThat(events).containsExactly("begin", "bind " + TENANT_A, "handle", "commit");
    }

    @Test
    void publicRequestStartsUnbound() {
        behavior.handle(new Anything(), () -> {
            events.add("handle");
            return "ok";
        });

        assertThat(events).containsExactly("begin", "handle", "commit");
    }

    @Test
    void failureRollsBack() {
        assertThatThrownBy(() -> behavior.handle(new Anything(), () -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(events).containsExactly("begin", "rollback");
    }
}

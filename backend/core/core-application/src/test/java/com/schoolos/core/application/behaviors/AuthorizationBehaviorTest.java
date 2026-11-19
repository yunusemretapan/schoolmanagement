package com.schoolos.core.application.behaviors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.AuthorizationException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.Public;
import com.schoolos.core.security.RequiresPermission;
import com.schoolos.core.security.UnauthenticatedException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-6: default deny. */
class AuthorizationBehaviorTest {

    @Public
    record OpenQuery() implements Request<String> {}

    @RequiresPermission(Permission.SELF_READ)
    record SelfQuery() implements Request<String> {}

    /** No annotation at all: must be refused even for a signed-in user. */
    record UndeclaredQuery() implements Request<String> {}

    private final AuthorizationBehavior behavior = new AuthorizationBehavior();
    private final AtomicBoolean handled = new AtomicBoolean();

    @AfterEach
    void clearUser() {
        CurrentUserHolder.clear();
    }

    private String send(Request<String> request) {
        return behavior.handle(request, () -> {
            handled.set(true);
            return "ok";
        });
    }

    private static CurrentUser user(Permission... permissions) {
        return new CurrentUser(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of("TEACHER"), Set.of(permissions));
    }

    @Test
    void publicRequestPassesWithoutSignIn() {
        assertThat(send(new OpenQuery())).isEqualTo("ok");
    }

    @Test
    void protectedRequestWithoutSignInIsUnauthenticated() {
        assertThatThrownBy(() -> send(new SelfQuery())).isInstanceOf(UnauthenticatedException.class);
        assertThat(handled).isFalse();
    }

    @Test
    void undeclaredRequestIsRefusedEvenWhenSignedIn() {
        CurrentUserHolder.set(user(Permission.values()));

        assertThatThrownBy(() -> send(new UndeclaredQuery())).isInstanceOf(AuthorizationException.class);
        assertThat(handled).isFalse();
    }

    @Test
    void undeclaredRequestWithoutSignInIsUnauthenticated() {
        assertThatThrownBy(() -> send(new UndeclaredQuery())).isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void signedInUserWithoutThePermissionIsForbidden() {
        CurrentUserHolder.set(user(Permission.SESSION_END));

        assertThatThrownBy(() -> send(new SelfQuery())).isInstanceOf(AuthorizationException.class);
        assertThat(handled).isFalse();
    }

    @Test
    void signedInUserWithThePermissionPasses() {
        CurrentUserHolder.set(user(Permission.SELF_READ));

        assertThat(send(new SelfQuery())).isEqualTo("ok");
    }
}

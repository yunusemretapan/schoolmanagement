package com.schoolos.core.application.behaviors;

import com.schoolos.core.application.mediator.PipelineBehavior;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.AuthorizationException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.Public;
import com.schoolos.core.security.RequiresPermission;
import com.schoolos.core.security.UnauthenticatedException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default deny (docs/security.md): a {@link Public} request passes; any other needs a signed-in user holding the
 * permission its {@link RequiresPermission} names. A request with neither annotation is refused, signed in or not.
 */
public final class AuthorizationBehavior implements PipelineBehavior {

    @Override
    public <R> R handle(Request<R> request, Supplier<R> next) {
        Class<?> type = request.getClass();
        if (type.isAnnotationPresent(Public.class)) {
            return next.get();
        }
        Optional<CurrentUser> user = CurrentUserHolder.current();
        if (user.isEmpty()) {
            throw new UnauthenticatedException("Sign-in required for " + type.getSimpleName());
        }
        RequiresPermission required = type.getAnnotation(RequiresPermission.class);
        if (required == null) {
            throw new AuthorizationException(type.getSimpleName() + " declares no permission (default deny)");
        }
        if (!user.get().has(required.value())) {
            throw new AuthorizationException("Missing permission " + required.value());
        }
        return next.get();
    }
}

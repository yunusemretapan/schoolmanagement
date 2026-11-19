package com.schoolos.core.application.behaviors;

import com.schoolos.core.application.mediator.PipelineBehavior;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.crosscutting.exceptions.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Validates every request before its handler (docs/security.md: the server never trusts the client): Jakarta
 * annotations first, then the request's own {@link RequestValidator}s. Any failure → {@link ValidationException}.
 */
public final class ValidationBehavior implements PipelineBehavior {

    private final Validator validator;
    private final List<RequestValidator<?>> validators;

    public ValidationBehavior(Validator validator, List<RequestValidator<?>> validators) {
        this.validator = validator;
        this.validators = List.copyOf(validators);
    }

    @Override
    public <R> R handle(Request<R> request, Supplier<R> next) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<Request<R>> violation : validator.validate(request)) {
            errors.putIfAbsent(violation.getPropertyPath().toString(), codeOf(violation));
        }
        for (RequestValidator<?> custom : validators) {
            if (custom.requestType().isInstance(request)) {
                validateWith(custom, request).forEach(errors::putIfAbsent);
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return next.get();
    }

    @SuppressWarnings("unchecked") // guarded by requestType().isInstance above
    private static <Q extends Request<?>> Map<String, String> validateWith(RequestValidator<Q> custom, Object request) {
        return custom.validate((Q) request);
    }

    /** {@code NotBlank} → {@code NOT_BLANK}: the constraint's name as a stable code. */
    private static String codeOf(ConstraintViolation<?> violation) {
        String name = violation
                .getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }
}

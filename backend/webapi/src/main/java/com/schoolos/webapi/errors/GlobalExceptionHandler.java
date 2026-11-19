package com.schoolos.webapi.errors;

import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.crosscutting.exceptions.ErrorCode;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.crosscutting.exceptions.TooManyRequestsException;
import com.schoolos.core.crosscutting.exceptions.ValidationException;
import com.schoolos.core.security.AuthorizationException;
import com.schoolos.core.security.UnauthenticatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The one place exceptions become HTTP responses: RFC 9457 ProblemDetails with a stable {@code code}.
 * No stack trace, SQL or internal detail ever reaches the client (docs/conventions.md, Error handling).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String CODE = Problems.CODE;
    static final String ERRORS = "errors";

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class, NotFoundException.class})
    public ProblemDetail notFound(Exception exception) {
        return Problems.of(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail business(BusinessException exception) {
        return Problems.of(HttpStatus.UNPROCESSABLE_CONTENT, exception.code());
    }

    /** 400 with field → code; the client builds the message (spec 0003, AC-3 server side). */
    @ExceptionHandler(ValidationException.class)
    public ProblemDetail validation(ValidationException exception) {
        ProblemDetail problem = Problems.of(HttpStatus.BAD_REQUEST, ValidationException.CODE);
        problem.setProperty(ERRORS, exception.fieldErrors());
        return problem;
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ProblemDetail unauthenticated(UnauthenticatedException exception) {
        return Problems.of(HttpStatus.UNAUTHORIZED, UnauthenticatedException.CODE);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ProblemDetail forbidden(AuthorizationException exception) {
        LOG.warn("Request refused: {}", exception.getMessage()); // names a request type, never personal data
        return Problems.of(HttpStatus.FORBIDDEN, AuthorizationException.CODE);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ProblemDetail tooManyRequests(TooManyRequestsException exception) {
        return Problems.of(HttpStatus.TOO_MANY_REQUESTS, exception.code());
    }

    /**
     * Catch-all. Spring MVC's own exceptions (405, 406, 400 ...) implement {@link ErrorResponse} and reach this method
     * too, because it is the most general handler: they keep their status and their headers (405 Allow, 415 Accept),
     * and only a 4xx stays out of the error log; a 5xx is a server fault and is logged as one
     * (docs/conventions.md, Known traps).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception exception) {
        if (exception instanceof ErrorResponse framework) {
            HttpStatusCode status = framework.getStatusCode();
            if (status.is5xxServerError()) {
                LOG.error("Request failed with {}", status.value(), exception);
            } else {
                LOG.debug("Request rejected by Spring MVC with {}", status.value());
            }
            return ResponseEntity.status(status)
                    .headers(framework.getHeaders())
                    .body(Problems.of(status, codeFor(status)));
        }
        LOG.error("Unhandled exception", exception);
        return ResponseEntity.internalServerError()
                .body(Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR));
    }

    private static ErrorCode codeFor(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return new ErrorCode(known == null ? "HTTP_" + status.value() : known.name());
    }
}

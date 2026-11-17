package com.schoolos.webapi.errors;

import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.crosscutting.exceptions.ErrorCode;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
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

    static final String CODE = "code";

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class, NotFoundException.class})
    public ProblemDetail notFound(Exception exception) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail business(BusinessException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, exception.code());
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
            return ResponseEntity.status(status).headers(framework.getHeaders()).body(problem(status, codeFor(status)));
        }
        LOG.error("Unhandled exception", exception);
        return ResponseEntity.internalServerError()
                .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR));
    }

    private static ErrorCode codeFor(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return new ErrorCode(known == null ? "HTTP_" + status.value() : known.name());
    }

    private static ProblemDetail problem(HttpStatusCode status, ErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        HttpStatus known = HttpStatus.resolve(status.value());
        problem.setTitle(known == null ? "HTTP " + status.value() : known.getReasonPhrase());
        problem.setProperty(CODE, code.value());
        return problem;
    }
}

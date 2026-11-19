package com.schoolos.webapi.errors;

import com.schoolos.core.crosscutting.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** RFC 9457 ProblemDetails with the stable {@code code} property (docs/conventions.md, Error handling). */
public final class Problems {

    public static final String CODE = "code";

    private Problems() {}

    public static ProblemDetail of(HttpStatusCode status, ErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        HttpStatus known = HttpStatus.resolve(status.value());
        problem.setTitle(known == null ? "HTTP " + status.value() : known.getReasonPhrase());
        problem.setProperty(CODE, code.value());
        return problem;
    }

    /** The same document as JSON text, for filters that answer before Spring MVC (401 / 403 / CORS). */
    public static String json(HttpStatus status, ErrorCode code) {
        return "{\"type\":\"about:blank\",\"title\":\"" + status.getReasonPhrase() + "\",\"status\":" + status.value()
                + ",\"" + CODE + "\":\"" + code.value() + "\"}";
    }
}

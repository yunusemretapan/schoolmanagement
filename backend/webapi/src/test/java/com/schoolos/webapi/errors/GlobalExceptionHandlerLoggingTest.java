package com.schoolos.webapi.errors;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.server.ResponseStatusException;

/** Review round 2, Low-1: a 5xx is a server fault and must reach the error log; a 4xx must not. */
class GlobalExceptionHandlerLoggingTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attach() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void frameworkServerErrorKeepsItsStatusAndIsLoggedAsError() {
        ProblemDetail problem = new GlobalExceptionHandler()
                .unexpected(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
                .getBody();

        assertThat(problem.getStatus()).isEqualTo(503);
        assertThat(problem.getProperties()).containsEntry("code", "SERVICE_UNAVAILABLE");
        assertThat(appender.list).extracting(ILoggingEvent::getLevel).contains(Level.ERROR);
    }

    @Test
    void frameworkClientErrorIsNotLoggedAsError() {
        new GlobalExceptionHandler().unexpected(new HttpMediaTypeNotAcceptableException("text/csv"));

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).doesNotContain(Level.ERROR);
    }
}

package com.schoolos.webapi.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schoolos.application.features.system.queries.GetSystemStatusQueryHandler;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.webapi.controllers.SystemStatusController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpMediaTypeNotAcceptableException;

/** Unit test (ADR 0003): standalone MockMvc, no Spring context, no network, no database. */
class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SystemStatusController(
                    new Mediator(List.of(new GetSystemStatusQueryHandler(() -> "0.1.0")), List.of())))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void unknownResourceReturnsProblemDetails404() throws Exception {
        String body = mvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/api/v1/does-not-exist"))
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.detail").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // RFC 9457 "instance" echoes the caller's own path; anything beyond these fields would be a leak.
        assertThat(body).doesNotContain("Exception").doesNotContain("at com.");
    }

    @Test
    void unexpectedErrorLeaksNoInternalDetail() {
        ProblemDetail problem = new GlobalExceptionHandler()
                .unexpected(new IllegalStateException("SELECT * FROM students"))
                .getBody();

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isNull();
        assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_ERROR");
        assertThat(String.valueOf(problem)).doesNotContain("SELECT");
    }

    @Test
    void wrongMethodKeepsItsOwnStatus() throws Exception {
        mvc.perform(post("/api/v1/system/status"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.title").value("Method Not Allowed"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.detail").doesNotExist())
                .andExpect(header().string("Allow", "GET"));
    }

    @Test
    void frameworkRejectionsAreNotInternalErrors() {
        ProblemDetail problem = new GlobalExceptionHandler()
                .unexpected(new HttpMediaTypeNotAcceptableException("text/csv"))
                .getBody();

        assertThat(problem.getStatus()).isEqualTo(406);
        assertThat(problem.getProperties()).containsEntry("code", "NOT_ACCEPTABLE");
        assertThat(problem.getDetail()).isNull();
    }
}

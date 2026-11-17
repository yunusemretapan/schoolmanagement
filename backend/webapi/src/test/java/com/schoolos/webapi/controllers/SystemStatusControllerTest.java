package com.schoolos.webapi.controllers;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schoolos.application.features.system.queries.GetSystemStatusQueryHandler;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.webapi.errors.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Unit test (ADR 0003): standalone MockMvc, no Spring context, no network, no database. */
class SystemStatusControllerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SystemStatusController(
                    new Mediator(List.of(new GetSystemStatusQueryHandler(() -> "0.1.0")), List.of())))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void returnsUpAndVersionWithoutAuth() throws Exception {
        mvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("0.1.0"))
                .andExpect(jsonPath("$", aMapWithSize(2)));
    }
}

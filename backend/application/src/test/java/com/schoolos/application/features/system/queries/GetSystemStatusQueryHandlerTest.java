package com.schoolos.application.features.system.queries;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.system.dtos.SystemStatusResponse;
import org.junit.jupiter.api.Test;

class GetSystemStatusQueryHandlerTest {

    @Test
    void reportsUpWithTheRunningVersion() {
        GetSystemStatusQueryHandler handler = new GetSystemStatusQueryHandler(() -> "1.2.3");

        SystemStatusResponse response = handler.handle(new GetSystemStatusQuery());

        assertThat(response).isEqualTo(new SystemStatusResponse("UP", "1.2.3"));
    }
}

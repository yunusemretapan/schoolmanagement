package com.schoolos.webapi.controllers;

import com.schoolos.application.features.system.dtos.SystemStatusResponse;
import com.schoolos.application.features.system.queries.GetSystemStatusQuery;
import com.schoolos.core.application.mediator.Mediator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public status endpoint: the one deliberate exception to default-deny (docs/security.md; spec 0001).
 * Returns status and version only.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    private final Mediator mediator;

    public SystemStatusController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping("/status")
    public SystemStatusResponse status() {
        return mediator.send(new GetSystemStatusQuery());
    }
}

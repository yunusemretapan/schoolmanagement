package com.schoolos.webapi.controllers;

import com.schoolos.application.features.identity.dtos.CurrentUserResponse;
import com.schoolos.application.features.identity.queries.GetCurrentUserQuery;
import com.schoolos.core.application.mediator.Mediator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own account: the home screen's data (spec 0003, Req. 10). */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final Mediator mediator;

    public MeController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public CurrentUserResponse me() {
        return mediator.send(new GetCurrentUserQuery());
    }
}

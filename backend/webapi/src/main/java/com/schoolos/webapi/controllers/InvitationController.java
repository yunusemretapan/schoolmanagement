package com.schoolos.webapi.controllers;

import com.schoolos.application.features.identity.commands.AcceptInvitationCommand;
import com.schoolos.application.features.identity.dtos.AcceptedInvitation;
import com.schoolos.application.features.identity.dtos.InvitationInfo;
import com.schoolos.application.features.identity.queries.GetInvitationQuery;
import com.schoolos.core.application.mediator.Mediator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The invitation link's two open calls (spec 0004, Req. 5, 6). The token travels in the body, never in the address,
 * so it stays out of access logs (plan 0004, R4). The source for the rate limit is the caller's network address.
 */
@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    public record LookupBody(String token) {
        @Override
        public String toString() {
            return "LookupBody[]";
        }
    }

    public record AcceptBody(String token, String password) {
        @Override
        public String toString() {
            return "AcceptBody[]";
        }
    }

    private final Mediator mediator;

    public InvitationController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/lookup")
    public InvitationInfo lookup(@RequestBody LookupBody body, HttpServletRequest request) {
        return mediator.send(new GetInvitationQuery(body.token(), request.getRemoteAddr()));
    }

    @PostMapping("/accept")
    public AcceptedInvitation accept(@RequestBody AcceptBody body, HttpServletRequest request) {
        return mediator.send(new AcceptInvitationCommand(body.token(), body.password(), request.getRemoteAddr()));
    }
}

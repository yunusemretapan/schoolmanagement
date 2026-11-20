package com.schoolos.webapi.controllers;

import com.schoolos.application.features.identity.commands.DeactivateStaffCommand;
import com.schoolos.application.features.identity.commands.InviteStaffCommand;
import com.schoolos.application.features.identity.commands.ReactivateStaffCommand;
import com.schoolos.application.features.identity.commands.RenewInvitationCommand;
import com.schoolos.application.features.identity.commands.UpdateStaffCommand;
import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.application.features.identity.dtos.StaffPage;
import com.schoolos.application.features.identity.queries.GetStaffQuery;
import com.schoolos.application.features.identity.queries.ListStaffQuery;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.domain.identity.Role;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff management for administrators (spec 0004). Who may call is decided in the Mediator (STAFF_MANAGE); another
 * institution's staff member answers 404 like an unknown id (AC-2).
 */
@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    /** Invite and update share one body; {@code roles} holds staff roles only. */
    public record StaffBody(String fullName, String email, String phone, Set<Role> roles) {
        @Override
        public String toString() {
            return "StaffBody[roles=" + roles + "]"; // no personal data in logs
        }
    }

    private final Mediator mediator;

    public StaffController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public StaffPage list(
            @RequestParam(name = "q", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return mediator.send(new ListStaffQuery(search, page));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationLink invite(@RequestBody StaffBody body) {
        return mediator.send(new InviteStaffCommand(body.fullName(), body.email(), body.phone(), body.roles()));
    }

    @GetMapping("/{staffId}")
    public StaffDetail get(@PathVariable UUID staffId) {
        return mediator.send(new GetStaffQuery(staffId));
    }

    @PutMapping("/{staffId}")
    public StaffDetail update(@PathVariable UUID staffId, @RequestBody StaffBody body) {
        return mediator.send(
                new UpdateStaffCommand(staffId, body.fullName(), body.email(), body.phone(), body.roles()));
    }

    @PostMapping("/{staffId}/invitation")
    public InvitationLink renewInvitation(@PathVariable UUID staffId) {
        return mediator.send(new RenewInvitationCommand(staffId));
    }

    @PostMapping("/{staffId}/deactivation")
    public StaffDetail deactivate(@PathVariable UUID staffId) {
        return mediator.send(new DeactivateStaffCommand(staffId));
    }

    @PostMapping("/{staffId}/reactivation")
    public StaffDetail reactivate(@PathVariable UUID staffId) {
        return mediator.send(new ReactivateStaffCommand(staffId));
    }
}

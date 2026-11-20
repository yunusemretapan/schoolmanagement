package com.schoolos.webapi.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.schoolos.application.features.identity.rules.RolePermissions;
import com.schoolos.core.application.behaviors.AuthorizationBehavior;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.webapi.errors.GlobalExceptionHandler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Spec 0004, AC-1 and AC-2 at the HTTP edge: the real AuthorizationBehavior in front of stand-in handlers, so the
 * status codes and bodies are what a client sees.
 */
class StaffControllerTest {

    private static final UUID KNOWN = UUID.fromString("01900000-0000-7000-8000-000000000001");
    private static final UUID FOREIGN = UUID.fromString("01900000-0000-7000-8000-0000000000ff");
    private static final String BODY = "{\"fullName\":\"Ali\",\"email\":\"ali@ornek.test\",\"roles\":[\"TEACHER\"]}";

    private final List<Request<?>> handled = new ArrayList<>();
    private final StaffDetail detail = new StaffDetail(
            KNOWN, "Ali", "ali@ornek.test", null, List.of("TEACHER"), AccountStatus.ACTIVE, null, false, false);

    private final Mediator mediator = new Mediator(
            List.of(
                    handler(ListStaffQuery.class, q -> new StaffPage(List.of(), q.page(), 50, 0)),
                    handler(GetStaffQuery.class, q -> found(q.staffId())),
                    handler(InviteStaffCommand.class, c -> new InvitationLink(KNOWN, "t", Instant.EPOCH)),
                    handler(UpdateStaffCommand.class, c -> found(c.staffId())),
                    handler(RenewInvitationCommand.class, c -> {
                        found(c.staffId());
                        return new InvitationLink(KNOWN, "t", Instant.EPOCH);
                    }),
                    handler(DeactivateStaffCommand.class, c -> found(c.staffId())),
                    handler(ReactivateStaffCommand.class, c -> found(c.staffId()))),
            List.of(new AuthorizationBehavior()));

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new StaffController(mediator))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @AfterEach
    void signOut() {
        CurrentUserHolder.clear();
    }

    private StaffDetail found(UUID id) {
        if (!KNOWN.equals(id)) {
            throw new NotFoundException("Staff"); // unknown and another institution's alike (StaffAccounts.load)
        }
        return detail;
    }

    private <Q extends Request<R>, R> RequestHandler<Q, R> handler(Class<Q> type, Function<Q, R> answer) {
        return new RequestHandler<>() {
            @Override
            public Class<Q> requestType() {
                return type;
            }

            @Override
            public R handle(Q request) {
                handled.add(request);
                return answer.apply(request);
            }
        };
    }

    private static void signInAs(String role) {
        CurrentUserHolder.set(new CurrentUser(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Set.of(role),
                RolePermissions.forRoleNames(Set.of(role))));
    }

    private static List<RequestBuilder> everyStaffCall(UUID id) {
        return List.of(
                get("/api/v1/staff"),
                post("/api/v1/staff").contentType(MediaType.APPLICATION_JSON).content(BODY),
                get("/api/v1/staff/" + id),
                put("/api/v1/staff/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY),
                post("/api/v1/staff/" + id + "/invitation"),
                post("/api/v1/staff/" + id + "/deactivation"),
                post("/api/v1/staff/" + id + "/reactivation"));
    }

    @Test
    void teachersAccountantsAndGuardiansAreRefusedEveryStaffCall() throws Exception {
        for (String role : List.of("TEACHER", "ACCOUNTANT", "GUARDIAN")) {
            signInAs(role);
            for (RequestBuilder call : everyStaffCall(KNOWN)) {
                mvc.perform(call)
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
            }
        }
        assertThat(handled).isEmpty();
    }

    @Test
    void withoutASignedInUserEveryStaffCallIs401() throws Exception {
        for (RequestBuilder call : everyStaffCall(KNOWN)) {
            mvc.perform(call).andExpect(status().isUnauthorized());
        }
        assertThat(handled).isEmpty();
    }

    @Test
    void anAdministratorReachesEveryStaffCall() throws Exception {
        signInAs("ADMIN");

        mvc.perform(get("/api/v1/staff").param("q", "ali").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1));
        mvc.perform(post("/api/v1/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("t"));
        mvc.perform(get("/api/v1/staff/" + KNOWN))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(handled).first().isEqualTo(new ListStaffQuery("ali", 1));
        assertThat(handled.get(1)).isInstanceOf(InviteStaffCommand.class);
    }

    @Test
    void anotherInstitutionsStaffAnswersExactlyLikeAnUnknownId() throws Exception {
        signInAs("ADMIN");
        UUID unknown = UUID.randomUUID();

        for (int i = 2; i < everyStaffCall(FOREIGN).size(); i++) {
            String foreignBody = mvc.perform(everyStaffCall(FOREIGN).get(i))
                    .andExpect(status().isNotFound())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()
                    .replace(FOREIGN.toString(), "<id>");
            String unknownBody = mvc.perform(everyStaffCall(unknown).get(i))
                    .andExpect(status().isNotFound())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()
                    .replace(unknown.toString(), "<id>");
            assertThat(foreignBody).isEqualTo(unknownBody).contains("\"code\":\"NOT_FOUND\"");
        }
    }
}

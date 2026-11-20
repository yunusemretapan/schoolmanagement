package com.schoolos.webapi.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schoolos.application.features.identity.rules.RolePermissions;
import com.schoolos.application.features.institution.commands.CampusCommands;
import com.schoolos.application.features.institution.commands.ClassGroupCommands;
import com.schoolos.application.features.institution.commands.GradeLevelCommands;
import com.schoolos.application.features.institution.commands.TermCommands;
import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.dtos.ClassGroupList;
import com.schoolos.application.features.institution.dtos.ClassGroupView;
import com.schoolos.application.features.institution.queries.AcademicQueries;
import com.schoolos.core.application.behaviors.AuthorizationBehavior;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.webapi.errors.GlobalExceptionHandler;
import java.time.LocalDate;
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
 * Spec 0005, AC-1 and AC-2 at the HTTP edge: the real AuthorizationBehavior in front of stand-in handlers, so the
 * status codes and bodies are what a client sees.
 */
class AcademicStructureControllerTest {

    private static final UUID KNOWN = UUID.fromString("01900000-0000-7000-8000-000000000001");
    private static final String NAME = "{\"name\":\"X\"}";
    private static final String TERM = "{\"name\":\"2026–2027\",\"startsOn\":\"2026-09-14\",\"endsOn\":\"2027-06-26\"}";
    private static final String GROUP = "{\"name\":\"7/A\",\"gradeLevelId\":\"" + KNOWN + "\"}";

    private final List<Request<?>> handled = new ArrayList<>();
    private final AcademicStructure structure = new AcademicStructure(
            KNOWN,
            List.of(new AcademicStructure.TermView(
                    KNOWN, "2026–2027", LocalDate.parse("2026-09-14"), LocalDate.parse("2027-06-26"), true, false)),
            List.of(new AcademicStructure.CampusView(KNOWN, "Merkez", false)),
            List.of());
    private final ClassGroupView group = new ClassGroupView(KNOWN, "7/A", KNOWN, KNOWN, KNOWN);

    private final Mediator mediator = new Mediator(
            List.of(
                    handler(AcademicQueries.GetAcademicStructure.class, q -> structure),
                    handler(AcademicQueries.ListClassGroups.class, q -> {
                        if (q.termId() != null) {
                            found(q.termId());
                        }
                        return new ClassGroupList(KNOWN, List.of(group));
                    }),
                    handler(CampusCommands.CreateCampus.class, c -> structure),
                    handler(CampusCommands.RenameCampus.class, c -> found(c.campusId())),
                    handler(CampusCommands.DeleteCampus.class, c -> found(c.campusId())),
                    handler(TermCommands.CreateTerm.class, c -> structure),
                    handler(TermCommands.UpdateTerm.class, c -> found(c.termId())),
                    handler(TermCommands.ActivateTerm.class, c -> found(c.termId())),
                    handler(TermCommands.DeleteTerm.class, c -> found(c.termId())),
                    handler(GradeLevelCommands.CreateGradeLevel.class, c -> structure),
                    handler(GradeLevelCommands.AddStandardGradeLevels.class, c -> structure),
                    handler(GradeLevelCommands.RenameGradeLevel.class, c -> found(c.gradeLevelId())),
                    handler(GradeLevelCommands.MoveGradeLevel.class, c -> found(c.gradeLevelId())),
                    handler(GradeLevelCommands.DeleteGradeLevel.class, c -> found(c.gradeLevelId())),
                    handler(ClassGroupCommands.CreateClassGroup.class, c -> group),
                    handler(ClassGroupCommands.UpdateClassGroup.class, c -> {
                        found(c.classGroupId());
                        return group;
                    }),
                    handler(ClassGroupCommands.DeleteClassGroup.class, c -> {
                        found(c.classGroupId());
                        return null;
                    })),
            List.of(new AuthorizationBehavior()));

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AcademicStructureController(mediator))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @AfterEach
    void signOut() {
        CurrentUserHolder.clear();
    }

    /** Another institution's record and an unknown id: the same not-found (the repositories see one tenant). */
    private AcademicStructure found(UUID id) {
        if (!KNOWN.equals(id)) {
            throw new NotFoundException("Record");
        }
        return structure;
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

    private static RequestBuilder json(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder call, String body) {
        return call.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static final String BASE = "/api/v1/academic-structure";

    /** Calls that name a record; the first two take none. */
    private static List<RequestBuilder> everyCall(UUID id) {
        return List.of(
                get(BASE),
                json(post(BASE + "/campuses"), NAME),
                json(put(BASE + "/campuses/" + id), NAME),
                delete(BASE + "/campuses/" + id),
                json(put(BASE + "/terms/" + id), TERM),
                post(BASE + "/terms/" + id + "/activation"),
                delete(BASE + "/terms/" + id),
                json(put(BASE + "/grade-levels/" + id), NAME),
                json(post(BASE + "/grade-levels/" + id + "/move"), "{\"direction\":\"UP\"}"),
                delete(BASE + "/grade-levels/" + id),
                get(BASE + "/class-groups").param("termId", id.toString()),
                json(put(BASE + "/class-groups/" + id), GROUP),
                delete(BASE + "/class-groups/" + id));
    }

    private static List<RequestBuilder> creations() {
        return List.of(
                json(post(BASE + "/terms"), TERM),
                json(post(BASE + "/grade-levels"), NAME),
                post(BASE + "/grade-levels/standard"),
                json(post(BASE + "/class-groups"), GROUP));
    }

    @Test
    void teachersAccountantsAndGuardiansAreRefusedEveryCall() throws Exception {
        for (String role : List.of("TEACHER", "ACCOUNTANT", "GUARDIAN")) {
            signInAs(role);
            for (RequestBuilder call : concat(everyCall(KNOWN), creations())) {
                mvc.perform(call)
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
            }
        }
        assertThat(handled).isEmpty();
    }

    @Test
    void withoutASignedInUserEveryCallIs401() throws Exception {
        for (RequestBuilder call : concat(everyCall(KNOWN), creations())) {
            mvc.perform(call).andExpect(status().isUnauthorized());
        }
        assertThat(handled).isEmpty();
    }

    @Test
    void anAdministratorReachesEveryCall() throws Exception {
        signInAs("ADMIN");

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms[0].startsOn").value("2026-09-14"))
                .andExpect(jsonPath("$.terms[0].active").value(true));
        for (RequestBuilder call : creations()) {
            mvc.perform(call).andExpect(status().isCreated());
        }
        mvc.perform(delete(BASE + "/class-groups/" + KNOWN)).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/class-groups"))
                .andExpect(jsonPath("$.items[0].name").value("7/A"));

        assertThat(handled)
                .contains(new TermCommands.CreateTerm(
                        "2026–2027", LocalDate.parse("2026-09-14"), LocalDate.parse("2027-06-26")))
                .contains(new ClassGroupCommands.CreateClassGroup("7/A", KNOWN, null, null))
                .contains(new AcademicQueries.ListClassGroups(null, null));
    }

    @Test
    void anotherInstitutionsRecordAnswersExactlyLikeAnUnknownId() throws Exception {
        signInAs("ADMIN");
        UUID foreign = UUID.fromString("01900000-0000-7000-8000-0000000000ff");
        UUID unknown = UUID.randomUUID();

        for (int i = 2; i < everyCall(foreign).size(); i++) {
            String foreignBody = mvc.perform(everyCall(foreign).get(i))
                    .andExpect(status().isNotFound())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()
                    .replace(foreign.toString(), "<id>");
            String unknownBody = mvc.perform(everyCall(unknown).get(i))
                    .andExpect(status().isNotFound())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()
                    .replace(unknown.toString(), "<id>");
            assertThat(foreignBody).isEqualTo(unknownBody).contains("\"code\":\"NOT_FOUND\"");
        }
    }

    private static List<RequestBuilder> concat(List<RequestBuilder> a, List<RequestBuilder> b) {
        List<RequestBuilder> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }
}

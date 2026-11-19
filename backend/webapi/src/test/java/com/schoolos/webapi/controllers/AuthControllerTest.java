package com.schoolos.webapi.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schoolos.application.features.identity.commands.LoginCommand;
import com.schoolos.application.features.identity.commands.LogoutCommand;
import com.schoolos.application.features.identity.commands.RefreshSessionCommand;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.AuthOutcome;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.domain.identity.ClientType;
import com.schoolos.webapi.errors.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Spec 0003, AC-4 (one answer for every refusal) and the token transport per client (plan 0003, R2). */
class AuthControllerTest {

    private final Mediator mediator = mock(Mediator.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(mediator))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private static String loginBody(String clientType) {
        return "{\"identifier\":\"ayse@ornek.test\",\"password\":\"Ornek-Sifre-2026\",\"clientType\":\"" + clientType
                + "\"}";
    }

    @Test
    void unknownWrongAndLockedLookExactlyAlike() throws Exception {
        when(mediator.send(any(LoginCommand.class)))
                .thenReturn(new AuthOutcome.Refused(IdentityErrorCodes.INVALID_CREDENTIALS));

        List<String> bodies = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            bodies.add(mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("WEB")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("IDENTITY_INVALID_CREDENTIALS"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString());
        }
        assertThat(bodies).containsOnly(bodies.get(0));
        assertThat(bodies.get(0))
                .doesNotContain("ayse")
                .doesNotContain("locked")
                .doesNotContain("exist");
    }

    @Test
    void webLoginPutsTheRefreshTokenOnlyInAnHttpOnlyStrictCookie() throws Exception {
        when(mediator.send(any(LoginCommand.class)))
                .thenReturn(new AuthOutcome.Issued("access-1", 900, "sid.secret", ClientType.WEB));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("WEB")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-1"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(
                                "Set-Cookie",
                                allOf(
                                        containsString("sos_refresh=sid.secret"),
                                        containsString("HttpOnly"),
                                        containsString("Secure"),
                                        containsString("SameSite=Strict"),
                                        containsString("Path=/api/v1/auth"),
                                        containsString("Max-Age=43200"))));
    }

    @Test
    void mobileLoginReturnsTheRefreshTokenInTheBody() throws Exception {
        when(mediator.send(any(LoginCommand.class)))
                .thenReturn(new AuthOutcome.Issued("access-1", 900, "sid.secret", ClientType.MOBILE));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("MOBILE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("sid.secret"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void loginUsesTheCallersAddressAsTheRateLimitSource() throws Exception {
        when(mediator.send(any(LoginCommand.class)))
                .thenReturn(new AuthOutcome.Refused(IdentityErrorCodes.INVALID_CREDENTIALS));

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("WEB"))
                .with(request -> {
                    request.setRemoteAddr("203.0.113.7");
                    return request;
                }));

        ArgumentCaptor<LoginCommand> sent = ArgumentCaptor.forClass(LoginCommand.class);
        verify(mediator).send(sent.capture());
        assertThat(sent.getValue().source()).isEqualTo("203.0.113.7");
        assertThat(sent.getValue().clientType()).isEqualTo(ClientType.WEB);
    }

    @Test
    void webRefreshReadsTheCookieAndRotatesIt() throws Exception {
        when(mediator.send(any(RefreshSessionCommand.class)))
                .thenReturn(new AuthOutcome.Issued("access-2", 900, "sid.next", ClientType.WEB));

        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("sos_refresh", "sid.first")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("sos_refresh=sid.next")));

        ArgumentCaptor<RefreshSessionCommand> sent = ArgumentCaptor.forClass(RefreshSessionCommand.class);
        verify(mediator).send(sent.capture());
        assertThat(sent.getValue().refreshToken()).isEqualTo("sid.first");
    }

    @Test
    void refusedWebRefreshClearsTheCookie() throws Exception {
        when(mediator.send(any(RefreshSessionCommand.class)))
                .thenReturn(new AuthOutcome.Refused(IdentityErrorCodes.SESSION_EXPIRED));

        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("sos_refresh", "sid.old")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("IDENTITY_SESSION_EXPIRED"))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void mobileRefreshReadsTheBody() throws Exception {
        when(mediator.send(any(RefreshSessionCommand.class)))
                .thenReturn(new AuthOutcome.Issued("access-2", 900, "sid.next", ClientType.MOBILE));

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"sid.first\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("sid.next"));
    }

    @Test
    void logoutEndsTheSessionAndClearsTheCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(mediator).send(any(LogoutCommand.class));
    }
}

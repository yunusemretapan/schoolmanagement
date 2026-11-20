package com.schoolos.webapi.config;

import com.schoolos.application.features.identity.commands.CreateSeedUserCommandHandler;
import com.schoolos.application.features.identity.commands.CreateSeedUserCommandValidator;
import com.schoolos.application.features.identity.commands.LoginCommandHandler;
import com.schoolos.application.features.identity.commands.LoginCommandValidator;
import com.schoolos.application.features.identity.commands.LogoutCommandHandler;
import com.schoolos.application.features.identity.commands.ProvisionTenantCommandHandler;
import com.schoolos.application.features.identity.commands.ProvisionTenantCommandValidator;
import com.schoolos.application.features.identity.commands.RefreshSessionCommandHandler;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.SourceRateLimiter;
import com.schoolos.application.features.identity.ports.TenantRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.queries.GetCurrentUserQueryHandler;
import com.schoolos.application.features.identity.rules.SessionActivityChecker;
import com.schoolos.application.features.system.queries.GetSystemStatusQueryHandler;
import com.schoolos.application.ports.AppVersionProvider;
import com.schoolos.core.application.behaviors.AuthorizationBehavior;
import com.schoolos.core.application.behaviors.RequestValidator;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.behaviors.TransactionBehavior;
import com.schoolos.core.application.behaviors.TransactionPort;
import com.schoolos.core.application.behaviors.ValidationBehavior;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.PasswordHasher;
import com.schoolos.core.security.SessionValidator;
import com.schoolos.core.security.TokenService;
import jakarta.validation.Validator;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free application handlers and behaviors into Spring. */
@Configuration
public class MediatorConfig {

    /** The pipeline, in this order: who may ask → is the input valid → one transaction bound to the tenant. */
    @Bean
    Mediator mediator(
            List<RequestHandler<?, ?>> handlers,
            List<RequestValidator<?>> validators,
            Validator validator,
            TransactionPort transactions,
            TenantSessionPort tenantSession) {
        return new Mediator(
                handlers,
                List.of(
                        new AuthorizationBehavior(),
                        new ValidationBehavior(validator, validators),
                        new TransactionBehavior(transactions, tenantSession)));
    }

    @Bean
    GetSystemStatusQueryHandler getSystemStatusQueryHandler(AppVersionProvider versionProvider) {
        return new GetSystemStatusQueryHandler(versionProvider);
    }

    // --- identity (spec 0003) ---

    @Bean
    LoginCommandHandler loginCommandHandler(
            LoginIdentityRepository identities,
            UserRepository users,
            SessionRepository sessions,
            SourceRateLimiter rateLimiter,
            PasswordHasher passwords,
            TokenService tokens,
            TenantSessionPort tenantSession,
            SecretGenerator secrets,
            IdGenerator ids,
            Clock clock) {
        return new LoginCommandHandler(
                identities, users, sessions, rateLimiter, passwords, tokens, tenantSession, secrets, ids, clock);
    }

    @Bean
    RefreshSessionCommandHandler refreshSessionCommandHandler(
            SessionRepository sessions,
            LoginIdentityRepository identities,
            UserRepository users,
            TokenService tokens,
            TenantSessionPort tenantSession,
            SecretGenerator secrets,
            Clock clock) {
        return new RefreshSessionCommandHandler(sessions, identities, users, tokens, tenantSession, secrets, clock);
    }

    @Bean
    LogoutCommandHandler logoutCommandHandler(SessionRepository sessions, Clock clock) {
        return new LogoutCommandHandler(sessions, clock);
    }

    @Bean
    GetCurrentUserQueryHandler getCurrentUserQueryHandler(UserRepository users, TenantRepository tenants) {
        return new GetCurrentUserQueryHandler(users, tenants);
    }

    @Bean
    ProvisionTenantCommandHandler provisionTenantCommandHandler(
            TenantRepository tenants,
            UserRepository users,
            LoginIdentityRepository identities,
            TenantSessionPort tenantSession,
            PasswordHasher passwords,
            IdGenerator ids,
            Clock clock) {
        return new ProvisionTenantCommandHandler(tenants, users, identities, tenantSession, passwords, ids, clock);
    }

    @Bean
    CreateSeedUserCommandHandler createSeedUserCommandHandler(
            UserRepository users,
            LoginIdentityRepository identities,
            TenantSessionPort tenantSession,
            PasswordHasher passwords,
            IdGenerator ids,
            Clock clock) {
        return new CreateSeedUserCommandHandler(users, identities, tenantSession, passwords, ids, clock);
    }

    @Bean
    LoginCommandValidator loginCommandValidator() {
        return new LoginCommandValidator();
    }

    @Bean
    ProvisionTenantCommandValidator provisionTenantCommandValidator() {
        return new ProvisionTenantCommandValidator();
    }

    @Bean
    CreateSeedUserCommandValidator createSeedUserCommandValidator() {
        return new CreateSeedUserCommandValidator();
    }

    @Bean
    SessionValidator sessionValidator(
            SessionRepository sessions,
            LoginIdentityRepository identities,
            UserRepository users,
            TransactionPort transactions,
            TenantSessionPort tenantSession) {
        return new SessionActivityChecker(sessions, identities, users, transactions, tenantSession);
    }
}

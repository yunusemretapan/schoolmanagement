package com.schoolos.webapi.config;

import com.schoolos.application.features.identity.commands.AcceptInvitationCommandHandler;
import com.schoolos.application.features.identity.commands.AcceptInvitationCommandValidator;
import com.schoolos.application.features.identity.commands.CreateSeedUserCommandHandler;
import com.schoolos.application.features.identity.commands.CreateSeedUserCommandValidator;
import com.schoolos.application.features.identity.commands.DeactivateStaffCommandHandler;
import com.schoolos.application.features.identity.commands.InviteStaffCommandHandler;
import com.schoolos.application.features.identity.commands.InviteStaffCommandValidator;
import com.schoolos.application.features.identity.commands.LoginCommandHandler;
import com.schoolos.application.features.identity.commands.LoginCommandValidator;
import com.schoolos.application.features.identity.commands.LogoutCommandHandler;
import com.schoolos.application.features.identity.commands.ProvisionTenantCommandHandler;
import com.schoolos.application.features.identity.commands.ProvisionTenantCommandValidator;
import com.schoolos.application.features.identity.commands.ReactivateStaffCommandHandler;
import com.schoolos.application.features.identity.commands.RefreshSessionCommandHandler;
import com.schoolos.application.features.identity.commands.RenewInvitationCommandHandler;
import com.schoolos.application.features.identity.commands.StaffAccounts;
import com.schoolos.application.features.identity.commands.UpdateStaffCommandHandler;
import com.schoolos.application.features.identity.commands.UpdateStaffCommandValidator;
import com.schoolos.application.features.identity.ports.AuditLog;
import com.schoolos.application.features.identity.ports.InvitationRepository;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.SourceRateLimiter;
import com.schoolos.application.features.identity.ports.StaffDirectory;
import com.schoolos.application.features.identity.ports.TenantLock;
import com.schoolos.application.features.identity.ports.TenantRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.queries.GetCurrentUserQueryHandler;
import com.schoolos.application.features.identity.queries.GetInvitationQueryHandler;
import com.schoolos.application.features.identity.queries.GetStaffQueryHandler;
import com.schoolos.application.features.identity.queries.ListStaffQueryHandler;
import com.schoolos.application.features.identity.rules.IdentityAuditLog;
import com.schoolos.application.features.identity.rules.InvitationGate;
import com.schoolos.application.features.identity.rules.SessionActivityChecker;
import com.schoolos.application.features.identity.rules.StaffRules;
import com.schoolos.application.features.system.queries.GetSystemStatusQueryHandler;
import com.schoolos.application.ports.AppVersionProvider;
import com.schoolos.core.application.behaviors.AuthorizationBehavior;
import com.schoolos.core.application.behaviors.RequestValidator;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.behaviors.TransactionBehavior;
import com.schoolos.core.application.behaviors.TransactionPort;
import com.schoolos.core.application.behaviors.ValidationBehavior;
import com.schoolos.core.application.events.DomainEventPublisher;
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
            Clock clock,
            DomainEventPublisher events) {
        return new ProvisionTenantCommandHandler(
                tenants, users, identities, tenantSession, passwords, ids, clock, events);
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

    // --- staff management (spec 0004) ---

    /** The identity module's audit records go into the shared audit log (spec 0005, plan R1). */
    @Bean
    AuditLog identityAuditLog(com.schoolos.core.application.audit.AuditLog shared) {
        return new IdentityAuditLog(shared);
    }

    @Bean
    StaffRules staffRules(StaffDirectory directory) {
        return new StaffRules(directory);
    }

    @Bean
    StaffAccounts staffAccounts(
            UserRepository users,
            LoginIdentityRepository identities,
            InvitationRepository invitations,
            AuditLog audit,
            StaffRules rules,
            IdGenerator ids,
            Clock clock) {
        return new StaffAccounts(users, identities, invitations, audit, rules, ids, clock);
    }

    @Bean
    InvitationGate invitationGate(
            InvitationRepository invitations, LoginIdentityRepository identities, SourceRateLimiter rateLimiter) {
        return new InvitationGate(invitations, identities, rateLimiter);
    }

    @Bean
    ListStaffQueryHandler listStaffQueryHandler(StaffDirectory directory) {
        return new ListStaffQueryHandler(directory);
    }

    @Bean
    GetStaffQueryHandler getStaffQueryHandler(StaffAccounts staff) {
        return new GetStaffQueryHandler(staff);
    }

    @Bean
    InviteStaffCommandHandler inviteStaffCommandHandler(
            UserRepository users,
            LoginIdentityRepository identities,
            StaffAccounts staff,
            SecretGenerator secrets,
            IdGenerator ids,
            Clock clock) {
        return new InviteStaffCommandHandler(users, identities, staff, secrets, ids, clock);
    }

    @Bean
    UpdateStaffCommandHandler updateStaffCommandHandler(
            UserRepository users,
            LoginIdentityRepository identities,
            InvitationRepository invitations,
            TenantLock tenantLock,
            StaffRules rules,
            StaffAccounts staff,
            Clock clock) {
        return new UpdateStaffCommandHandler(users, identities, invitations, tenantLock, rules, staff, clock);
    }

    @Bean
    RenewInvitationCommandHandler renewInvitationCommandHandler(StaffAccounts staff, SecretGenerator secrets) {
        return new RenewInvitationCommandHandler(staff, secrets);
    }

    @Bean
    DeactivateStaffCommandHandler deactivateStaffCommandHandler(
            LoginIdentityRepository identities,
            SessionRepository sessions,
            InvitationRepository invitations,
            TenantLock tenantLock,
            StaffRules rules,
            StaffAccounts staff,
            Clock clock) {
        return new DeactivateStaffCommandHandler(identities, sessions, invitations, tenantLock, rules, staff, clock);
    }

    @Bean
    ReactivateStaffCommandHandler reactivateStaffCommandHandler(
            LoginIdentityRepository identities, StaffAccounts staff) {
        return new ReactivateStaffCommandHandler(identities, staff);
    }

    @Bean
    GetInvitationQueryHandler getInvitationQueryHandler(
            InvitationGate gate,
            UserRepository users,
            TenantRepository tenants,
            TenantSessionPort tenantSession,
            Clock clock) {
        return new GetInvitationQueryHandler(gate, users, tenants, tenantSession, clock);
    }

    @Bean
    AcceptInvitationCommandHandler acceptInvitationCommandHandler(
            InvitationGate gate,
            InvitationRepository invitations,
            LoginIdentityRepository identities,
            AuditLog audit,
            PasswordHasher passwords,
            TenantSessionPort tenantSession,
            IdGenerator ids,
            Clock clock) {
        return new AcceptInvitationCommandHandler(
                gate, invitations, identities, audit, passwords, tenantSession, ids, clock);
    }

    @Bean
    InviteStaffCommandValidator inviteStaffCommandValidator() {
        return new InviteStaffCommandValidator();
    }

    @Bean
    UpdateStaffCommandValidator updateStaffCommandValidator() {
        return new UpdateStaffCommandValidator();
    }

    @Bean
    AcceptInvitationCommandValidator acceptInvitationCommandValidator() {
        return new AcceptInvitationCommandValidator();
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

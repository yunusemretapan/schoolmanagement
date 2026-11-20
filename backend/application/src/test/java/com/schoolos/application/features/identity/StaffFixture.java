package com.schoolos.application.features.identity;

import com.schoolos.application.features.identity.commands.DeactivateStaffCommandHandler;
import com.schoolos.application.features.identity.commands.InviteStaffCommandHandler;
import com.schoolos.application.features.identity.commands.ReactivateStaffCommandHandler;
import com.schoolos.application.features.identity.commands.RenewInvitationCommandHandler;
import com.schoolos.application.features.identity.commands.StaffAccounts;
import com.schoolos.application.features.identity.commands.UpdateStaffCommandHandler;
import com.schoolos.application.features.identity.queries.GetStaffQueryHandler;
import com.schoolos.application.features.identity.queries.ListStaffQueryHandler;
import com.schoolos.application.features.identity.rules.RolePermissions;
import com.schoolos.application.features.identity.rules.StaffRules;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.domain.identity.ClientType;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.UserAccount;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The staff handlers over {@link IdentityFixture}, with two institutions: Örnek Koleji (an administrator, a teacher)
 * and Başka Kolej (an administrator). Callers are bound the way the web filter and TransactionBehavior do it: the
 * current user is set and the transaction is bound to their tenant.
 */
public final class StaffFixture {

    public static final String PASSWORD = "Ornek-Sifre-2026";

    public final IdentityFixture fx = new IdentityFixture();
    public final StaffRules rules = new StaffRules(fx.staffDirectory());
    public final StaffAccounts staff = new StaffAccounts(
            fx.userRepository(),
            fx.identityRepository(),
            fx.invitationRepository(),
            fx.auditLog(),
            rules,
            fx.ids,
            fx.clock);

    public final InviteStaffCommandHandler invite = new InviteStaffCommandHandler(
            fx.userRepository(), fx.identityRepository(), staff, fx.secrets, fx.ids, fx.clock);
    public final UpdateStaffCommandHandler update = new UpdateStaffCommandHandler(
            fx.userRepository(),
            fx.identityRepository(),
            fx.invitationRepository(),
            fx.tenantLock(),
            rules,
            staff,
            fx.clock);
    public final RenewInvitationCommandHandler renew = new RenewInvitationCommandHandler(staff, fx.secrets);
    public final DeactivateStaffCommandHandler deactivate = new DeactivateStaffCommandHandler(
            fx.identityRepository(),
            fx.sessionRepository(),
            fx.invitationRepository(),
            fx.tenantLock(),
            rules,
            staff,
            fx.clock);
    public final ReactivateStaffCommandHandler reactivate =
            new ReactivateStaffCommandHandler(fx.identityRepository(), staff);
    public final ListStaffQueryHandler list = new ListStaffQueryHandler(fx.staffDirectory());
    public final GetStaffQueryHandler get = new GetStaffQueryHandler(staff);

    public final UserAccount admin =
            fx.seedUser("Örnek Koleji", "Kurucu Ada", "kurucu@ornek.test", null, PASSWORD, Role.ADMIN);
    public final UserAccount teacher =
            fx.seedUserIn(admin.tenantId(), "Öğretmen Ali", "ali@ornek.test", "+905321234567", PASSWORD, Role.TEACHER);
    public final UserAccount otherAdmin =
            fx.seedUser("Başka Kolej", "Başka Kurucu", "kurucu@baska.test", null, PASSWORD, Role.ADMIN);

    /** Signs {@code user} in and binds the transaction to their tenant, as one request would. */
    public void actAs(UserAccount user) {
        CurrentUserHolder.set(new CurrentUser(
                user.id(),
                user.tenantId(),
                UUID.randomUUID(),
                names(user.roles()),
                RolePermissions.forRoleNames(names(user.roles()))));
        fx.tenantSession().bind(user.tenantId());
    }

    public static void signOut() {
        CurrentUserHolder.clear();
    }

    public UserAccount seedStaff(String fullName, String email, Role... roles) {
        return fx.seedUserIn(admin.tenantId(), fullName, email, null, PASSWORD, roles);
    }

    public LoginIdentity identity(UserAccount user) {
        return fx.identities.get(user.id());
    }

    public UserAccount user(UUID id) {
        return fx.users.get(id);
    }

    public UUID openSession(UserAccount user, ClientType type) {
        UUID id = fx.ids.next();
        fx.sessions.put(
                id,
                new Session(id, user.id(), user.tenantId(), type, "h", fx.clock.instant(), fx.clock.instant(), null));
        return id;
    }

    public static Set<Role> roles(Role... roles) {
        return Set.copyOf(Arrays.asList(roles));
    }

    private static Set<String> names(Set<Role> roles) {
        return roles.stream().map(Role::name).collect(Collectors.toUnmodifiableSet());
    }
}

package com.schoolos.webapi.dev;

import com.schoolos.application.features.identity.commands.CreateSeedUserCommand;
import com.schoolos.application.features.identity.commands.ProvisionTenantCommand;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.ProvisionedAccount;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.domain.identity.Role;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Sample users for local development and testing only (spec 0003, Req. 1 / AC-1): one per Faz 1 role, one with two
 * roles, and a second institution to see isolation. Only in the {@code dev} profile, so a production configuration
 * never creates them. Idempotent: an existing sample (identifier taken) is skipped on the next start.
 */
@Component
@Profile(DevSeedRunner.PROFILE)
public class DevSeedRunner implements ApplicationRunner {

    public static final String PROFILE = "dev";

    private static final Logger LOG = LoggerFactory.getLogger(DevSeedRunner.class);

    private final Mediator mediator;
    private final String password;

    public DevSeedRunner(Mediator mediator, @Value("${app.dev-seed.password}") String password) {
        this.mediator = mediator;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        CurrentUserHolder.runAs(CurrentUser.operator(), () -> {
            seed();
            return null;
        });
    }

    private void seed() {
        ProvisionedAccount ornek;
        try {
            ornek = mediator.send(new ProvisionTenantCommand(
                    "Örnek Koleji", "Kurucu Örnek", "kurucu@ornek.test", "05320000001", password));
        } catch (BusinessException e) {
            if (!IdentityErrorCodes.IDENTIFIER_TAKEN.equals(e.code())) {
                throw e;
            }
            LOG.info("Dev seed already present; skipping");
            return;
        }
        user(ornek, "Öğretmen Örnek", "ogretmen@ornek.test", "05320000002", Set.of(Role.TEACHER));
        user(ornek, "Veli Örnek", "veli@ornek.test", "05320000003", Set.of(Role.GUARDIAN));
        user(ornek, "Muhasebe Örnek", "muhasebe@ornek.test", "05320000004", Set.of(Role.ACCOUNTANT));
        user(ornek, "Öğretmen Veli Örnek", "ogretmen.veli@ornek.test", null, Set.of(Role.TEACHER, Role.GUARDIAN));
        mediator.send(new ProvisionTenantCommand(
                "Deneme Kurs Merkezi", "Kurucu Deneme", "kurucu@deneme.test", "05320000009", password));
        LOG.info("Dev seed created (institution {})", ornek.tenantId());
    }

    private void user(ProvisionedAccount tenant, String name, String email, String phone, Set<Role> roles) {
        mediator.send(new CreateSeedUserCommand(tenant.tenantId(), name, email, phone, password, roles));
    }
}

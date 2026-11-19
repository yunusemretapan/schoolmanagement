package com.schoolos.webapi.operator;

import com.schoolos.application.features.identity.commands.ProvisionTenantCommand;
import com.schoolos.application.features.identity.dtos.ProvisionedAccount;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Operator tool (spec 0003, Req. 1): creates an institution and its founder from the command line, with no user
 * interface and no HTTP endpoint. The password comes from an environment variable, never from an argument (it
 * would land in shell history and process lists):
 *
 * <pre>
 * PROVISION_FOUNDER_PASSWORD=... java -jar webapi.jar --spring.main.web-application-type=none \
 *     --provision-tenant --institution-name="Örnek Koleji" --founder-name="Ad Soyad" \
 *     --email=kurucu@ornek.com.tr --phone=05320000000
 * </pre>
 */
@Component
public class ProvisionTenantRunner implements ApplicationRunner {

    static final String TRIGGER = "provision-tenant";
    static final String PASSWORD_VARIABLE = "PROVISION_FOUNDER_PASSWORD";

    private static final Logger LOG = LoggerFactory.getLogger(ProvisionTenantRunner.class);

    private final Mediator mediator;
    private final Environment environment;

    public ProvisionTenantRunner(Mediator mediator, Environment environment) {
        this.mediator = mediator;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(TRIGGER)) {
            return;
        }
        ProvisionTenantCommand command = new ProvisionTenantCommand(
                single(args, "institution-name"),
                single(args, "founder-name"),
                single(args, "email"),
                single(args, "phone"),
                environment.getProperty(PASSWORD_VARIABLE));
        ProvisionedAccount created = CurrentUserHolder.runAs(CurrentUser.operator(), () -> mediator.send(command));
        LOG.info("Provisioned tenant {} with founder user {}", created.tenantId(), created.userId());
    }

    private static String single(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}

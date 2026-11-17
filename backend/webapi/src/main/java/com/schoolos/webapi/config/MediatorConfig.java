package com.schoolos.webapi.config;

import com.schoolos.application.features.system.queries.GetSystemStatusQueryHandler;
import com.schoolos.application.ports.AppVersionProvider;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.core.application.mediator.PipelineBehavior;
import com.schoolos.core.application.mediator.RequestHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free application handlers and behaviors into Spring. */
@Configuration
public class MediatorConfig {

    @Bean
    GetSystemStatusQueryHandler getSystemStatusQueryHandler(AppVersionProvider versionProvider) {
        return new GetSystemStatusQueryHandler(versionProvider);
    }

    @Bean
    Mediator mediator(List<RequestHandler<?, ?>> handlers, List<PipelineBehavior> behaviors) {
        return new Mediator(handlers, behaviors);
    }
}

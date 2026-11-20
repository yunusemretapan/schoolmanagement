package com.schoolos.webapi.config;

import com.schoolos.core.application.events.DomainEventHandler;
import com.schoolos.core.application.events.DomainEventPublisher;
import com.schoolos.core.application.events.SynchronousDomainEventPublisher;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** In-process, synchronous domain events (ADR 0006): every {@link DomainEventHandler} bean is a subscriber. */
@Configuration
class DomainEventsConfig {

    @Bean
    DomainEventPublisher domainEventPublisher(List<DomainEventHandler<?>> handlers) {
        return new SynchronousDomainEventPublisher(handlers);
    }
}

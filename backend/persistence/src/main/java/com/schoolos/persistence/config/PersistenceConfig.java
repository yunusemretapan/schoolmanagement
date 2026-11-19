package com.schoolos.persistence.config;

import com.schoolos.core.application.behaviors.TransactionPort;
import java.util.function.Supplier;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JPA wiring for the persistence module. Transactions start only here, through {@link TransactionPort}, which
 * TransactionBehavior calls (docs/conventions.md, Known traps: no {@code @Transactional} in services).
 */
@Configuration
@EntityScan("com.schoolos.persistence")
public class PersistenceConfig {

    @Bean
    TransactionPort transactionPort(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return new TransactionPort() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                return template.execute(status -> work.get());
            }
        };
    }
}

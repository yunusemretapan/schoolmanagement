package com.schoolos.persistence.identity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.domain.identity.LoginIdentity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

/**
 * Spec 0004 review 1, L-4: when two invitations with one identifier race past the business check, the unique index
 * decides and the loser gets IDENTITY_IDENTIFIER_TAKEN, not a 500. The index itself is observed on PostgreSQL in VERIFY
 * (ADR 0003: unit tests only).
 */
class LoginIdentitiesTest {

    private final EntityManager em = mock(EntityManager.class);
    private final JpaIdentityRepositories.LoginIdentities identities = new JpaIdentityRepositories.LoginIdentities();

    LoginIdentitiesTest() throws ReflectiveOperationException {
        var field = JpaIdentityRepositories.LoginIdentities.class.getDeclaredField("em");
        field.setAccessible(true);
        field.set(identities, em);
    }

    private static LoginIdentity invited() {
        return LoginIdentity.invited(UUID.randomUUID(), UUID.randomUUID(), "yeni@ornek.test", null);
    }

    private void flushFailsOn(String constraint) {
        doThrow(new ConstraintViolationException("duplicate", new SQLException("23505"), constraint))
                .when(em)
                .flush();
    }

    @Test
    void aTakenEmailOrPhoneIndexBecomesTheIdentifierTakenRefusal() {
        for (String index : new String[] {"login_identities_email_key", "login_identities_phone_key"}) {
            flushFailsOn(index);

            assertThatThrownBy(() -> identities.create(invited()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).code())
                    .isEqualTo(IdentityErrorCodes.IDENTIFIER_TAKEN);
        }
    }

    @Test
    void anyOtherFailureIsNotDisguised() {
        flushFailsOn("login_identities_has_identifier");
        assertThatThrownBy(() -> identities.create(invited())).isInstanceOf(ConstraintViolationException.class);

        doThrow(new PersistenceException("connection lost")).when(em).flush();
        assertThatThrownBy(() -> identities.create(invited())).isInstanceOf(PersistenceException.class);
    }

    @Test
    void theIdentityIsPersistedBeforeTheFlush() {
        doThrow(new IllegalStateException("persist first")).when(em).persist(any());

        assertThatThrownBy(() -> identities.create(invited())).hasMessage("persist first");
    }
}

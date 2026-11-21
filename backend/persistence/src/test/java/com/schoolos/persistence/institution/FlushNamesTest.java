package com.schoolos.persistence.institution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

/**
 * Spec 0005, Req. 8 / plan 0005, R3–R4 (review 1, L-3): when two administrators race past the name check, the folded
 * name index decides and the loser gets ACADEMIC_NAME_TAKEN (422), not a 500. The indexes are read from the migration,
 * so a renamed index fails here; the index itself is observed on PostgreSQL in VERIFY (ADR 0003: unit tests only).
 */
class FlushNamesTest {

    private static final Pattern NAME_INDEX = Pattern.compile("create unique index (\\w+_name_uq)");

    private final EntityManager em = mock(EntityManager.class);

    private static List<String> nameIndexesOfV4() throws IOException {
        try (InputStream in = FlushNamesTest.class.getResourceAsStream("/db/migration/V4__academic_structure.sql")) {
            assertThat(in).isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return NAME_INDEX.matcher(sql).results().map(m -> m.group(1)).toList();
        }
    }

    private void flushFailsOn(String constraint) {
        PersistenceException wrapped = new PersistenceException(
                "could not execute statement",
                new ConstraintViolationException("duplicate", new SQLException("23505"), constraint));
        doThrow(wrapped).when(em).flush();
    }

    @Test
    void everyNameIndexOfTheMigrationBecomesTheNameTakenRefusal() throws IOException {
        List<String> indexes = nameIndexesOfV4();
        assertThat(indexes)
                .containsExactlyInAnyOrder(
                        "campuses_name_uq", "terms_name_uq", "grade_levels_name_uq", "class_groups_name_uq");

        for (String index : indexes) {
            flushFailsOn(index);

            assertThatThrownBy(() -> JpaInstitutionRepositories.flushNames(em))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).code())
                    .isEqualTo(InstitutionErrorCodes.NAME_TAKEN);
        }
    }

    @Test
    void anyOtherFailureIsNotDisguised() {
        flushFailsOn("class_groups_campus_fk");
        assertThatThrownBy(() -> JpaInstitutionRepositories.flushNames(em))
                .isInstanceOf(PersistenceException.class)
                .isNotInstanceOf(BusinessException.class);

        doThrow(new PersistenceException("connection lost")).when(em).flush();
        assertThatThrownBy(() -> JpaInstitutionRepositories.flushNames(em)).hasMessage("connection lost");
    }
}

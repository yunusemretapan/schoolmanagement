package com.schoolos.domain.institution;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Spec 0005, Req. 5 / AC-5: the standard list. */
class StandardGradeLevelsTest {

    @Test
    void fourteenLevelsInOrder() {
        assertThat(StandardGradeLevels.NAMES).hasSize(14);
        assertThat(StandardGradeLevels.NAMES)
                .startsWith("Anaokulu", "1. sınıf", "2. sınıf")
                .endsWith("11. sınıf", "12. sınıf", "Hazırlık");
    }
}

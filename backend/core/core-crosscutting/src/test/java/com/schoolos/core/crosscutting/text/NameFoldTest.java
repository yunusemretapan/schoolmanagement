package com.schoolos.core.crosscutting.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Spec 0005, Req. 7 / AC-7, plan R4: one folding for every name comparison, in memory and in queries. */
class NameFoldTest {

    @Test
    void theTurkishILettersAndCaseAllFoldAlike() {
        assertThat(NameFold.fold("Işıl İrmak")).isEqualTo("işil irmak");
        assertThat(NameFold.fold("7/A")).isEqualTo(NameFold.fold("7/a"));
        assertThat(NameFold.fold("HAZIRLIK")).isEqualTo(NameFold.fold("Hazırlık"));
        assertThat(NameFold.fold("İSTANBUL ŞUBESİ")).isEqualTo("istanbul şubesi");
    }

    @Test
    void theQueryExpressionAppliesTheSameReplacementsBeforeLowerCasing() {
        assertThat(NameFold.jpql("c.name"))
                .isEqualTo("lower(replace(replace(replace(c.name, 'İ', 'i'), 'I', 'i'), 'ı', 'i'))");
    }
}

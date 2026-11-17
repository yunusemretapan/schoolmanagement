package com.schoolos.core.crosscutting.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void acceptsUpperSnakeCase() {
        assertThat(new ErrorCode("BILLING_INSTALLMENT_ALREADY_PAID").value())
                .isEqualTo("BILLING_INSTALLMENT_ALREADY_PAID");
    }

    @Test
    void rejectsAnythingElse() {
        assertThatThrownBy(() -> new ErrorCode("notFound")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorCode(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notFoundExceptionCarriesNotFoundCode() {
        assertThat(new NotFoundException("x").code()).isEqualTo(ErrorCode.NOT_FOUND);
    }
}

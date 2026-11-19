package com.schoolos.core.application.behaviors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.crosscutting.exceptions.ValidationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

class ValidationBehaviorTest {

    record Greet(@NotBlank String name, String nickname) implements Request<String> {}

    private static final Validator VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    private static final RequestValidator<Greet> NO_SHOUTING = new RequestValidator<>() {
        @Override
        public Class<Greet> requestType() {
            return Greet.class;
        }

        @Override
        public Map<String, String> validate(Greet request) {
            return "BOB".equals(request.nickname()) ? Map.of("nickname", "SHOUTING") : Map.of();
        }
    };

    private final ValidationBehavior behavior = new ValidationBehavior(VALIDATOR, List.of(NO_SHOUTING));

    @Test
    void annotationViolationsBecomeStableFieldCodes() {
        assertThatThrownBy(() -> behavior.handle(new Greet(" ", null), () -> "ok"))
                .isInstanceOfSatisfying(
                        ValidationException.class,
                        e -> assertThat(e.fieldErrors()).containsExactly(Map.entry("name", "NOT_BLANK")));
    }

    @Test
    void requestValidatorsAddTheirCodes() {
        assertThatThrownBy(() -> behavior.handle(new Greet("Ayşe", "BOB"), () -> "ok"))
                .isInstanceOfSatisfying(
                        ValidationException.class,
                        e -> assertThat(e.fieldErrors()).containsExactly(Map.entry("nickname", "SHOUTING")));
    }

    @Test
    void validRequestReachesTheHandler() {
        assertThat(behavior.handle(new Greet("Ayşe", "Ayşo"), () -> "ok")).isEqualTo("ok");
    }
}

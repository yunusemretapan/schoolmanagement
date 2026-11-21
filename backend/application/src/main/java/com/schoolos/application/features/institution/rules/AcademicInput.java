package com.schoolos.application.features.institution.rules;

import com.schoolos.domain.institution.AcademicNames;
import com.schoolos.domain.institution.Term;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Field rules of the academic structure (spec 0005, Req. 4, 7): names are measured after trimming; codes are the
 * same constraint names the annotations use ({@code NOT_BLANK}, {@code SIZE}, {@code NOT_NULL}) plus the date rules.
 */
public final class AcademicInput {

    /** The end is not after the start. */
    public static final String END_NOT_AFTER_START = "END_NOT_AFTER_START";
    /** Before 2000-01-01 or after 2100-12-31. */
    public static final String OUT_OF_RANGE = "OUT_OF_RANGE";
    /** Longer than 731 days. */
    public static final String TOO_LONG = "TOO_LONG";

    private AcademicInput() {}

    public static Map<String, String> name(String field, String value, int max) {
        Map<String, String> errors = new HashMap<>();
        String name = AcademicNames.normalize(value);
        if (name == null || name.isEmpty()) {
            errors.put(field, "NOT_BLANK");
        } else if (name.length() > max) {
            errors.put(field, "SIZE");
        }
        return errors;
    }

    public static Map<String, String> dates(LocalDate startsOn, LocalDate endsOn) {
        Map<String, String> errors = new HashMap<>();
        if (startsOn == null) {
            errors.put("startsOn", "NOT_NULL");
        } else if (startsOn.isBefore(Term.EARLIEST) || startsOn.isAfter(Term.LATEST)) {
            errors.put("startsOn", OUT_OF_RANGE);
        }
        if (endsOn == null) {
            errors.put("endsOn", "NOT_NULL");
        } else if (endsOn.isBefore(Term.EARLIEST) || endsOn.isAfter(Term.LATEST)) {
            errors.put("endsOn", OUT_OF_RANGE);
        }
        if (errors.isEmpty()) {
            if (!endsOn.isAfter(startsOn)) {
                errors.put("endsOn", END_NOT_AFTER_START);
            } else if (Term.lengthInDays(startsOn, endsOn) > Term.MAX_DAYS) {
                errors.put("endsOn", TOO_LONG);
            }
        }
        return errors;
    }
}

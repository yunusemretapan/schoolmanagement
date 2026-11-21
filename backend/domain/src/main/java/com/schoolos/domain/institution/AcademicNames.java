package com.schoolos.domain.institution;

/** Name rules of the academic structure (spec 0005, Req. 7): trimmed, never blank, at most 60 or 20 characters. */
public final class AcademicNames {

    /** Campus, term and grade level names. */
    public static final int MAX_LENGTH = 60;

    public static final int CLASS_GROUP_MAX_LENGTH = 20;

    private AcademicNames() {}

    /** The stored form: leading and trailing white space removed; {@code null} stays {@code null}. */
    public static String normalize(String name) {
        return name == null ? null : name.strip();
    }
}

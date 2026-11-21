package com.schoolos.application.features.institution.constants;

import com.schoolos.core.crosscutting.exceptions.ErrorCode;

/** Stable codes of the institution feature (docs/conventions.md, Naming; plan 0005, R6). */
public final class InstitutionErrorCodes {

    /** Another record of the same kind (and, for a class group, the same campus and term) has this name (Req. 7). */
    public static final ErrorCode NAME_TAKEN = new ErrorCode("ACADEMIC_NAME_TAKEN");
    /** The dates share at least one day with another term (Req. 4). */
    public static final ErrorCode TERM_OVERLAP = new ErrorCode("ACADEMIC_TERM_OVERLAP");
    /** The institution's last campus cannot be deleted (Req. 3, 9). */
    public static final ErrorCode LAST_CAMPUS = new ErrorCode("ACADEMIC_LAST_CAMPUS");
    /** The active term cannot be deleted (Req. 9). */
    public static final ErrorCode ACTIVE_TERM = new ErrorCode("ACADEMIC_ACTIVE_TERM");
    /** Class groups belong to this campus, term or grade level (Req. 9). */
    public static final ErrorCode IN_USE = new ErrorCode("ACADEMIC_IN_USE");
    /** The institution already has 50 grade levels (Req. 5). */
    public static final ErrorCode GRADE_LEVEL_LIMIT = new ErrorCode("ACADEMIC_GRADE_LEVEL_LIMIT");
    /** The standard list is only offered while there are no grade levels (Req. 5). */
    public static final ErrorCode GRADE_LEVELS_NOT_EMPTY = new ErrorCode("ACADEMIC_GRADE_LEVELS_NOT_EMPTY");
    /** The first level cannot move up, the last cannot move down (Req. 5). */
    public static final ErrorCode CANNOT_MOVE = new ErrorCode("ACADEMIC_CANNOT_MOVE");
    /** A class group needs a term; the institution has none yet (Req. 6). */
    public static final ErrorCode NO_TERM = new ErrorCode("ACADEMIC_NO_TERM");

    private InstitutionErrorCodes() {}
}

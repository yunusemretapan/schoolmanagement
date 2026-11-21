package com.schoolos.domain.institution;

/** What an academic-structure audit record says was done (spec 0005, Req. 10). */
public enum AcademicAuditAction {
    CAMPUS_CREATED,
    CAMPUS_RENAMED,
    CAMPUS_DELETED,
    TERM_CREATED,
    TERM_CHANGED,
    TERM_ACTIVATED,
    TERM_DELETED,
    GRADE_LEVEL_CREATED,
    GRADE_LEVEL_RENAMED,
    GRADE_LEVEL_MOVED,
    GRADE_LEVEL_DELETED,
    CLASS_GROUP_CREATED,
    CLASS_GROUP_CHANGED,
    CLASS_GROUP_DELETED
}

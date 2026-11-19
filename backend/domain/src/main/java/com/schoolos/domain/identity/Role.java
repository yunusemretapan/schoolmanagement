package com.schoolos.domain.identity;

/** Faz 1 roles (docs/domain.md, Rol). A user may hold several (e.g. teacher and guardian). */
public enum Role {
    /** Kurucu / Yönetici. */
    ADMIN,
    /** Öğretmen. */
    TEACHER,
    /** Veli. */
    GUARDIAN,
    /** Muhasebe / İdari personel. */
    ACCOUNTANT
}

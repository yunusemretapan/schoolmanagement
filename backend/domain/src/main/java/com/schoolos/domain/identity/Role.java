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
    ACCOUNTANT;

    /** Personel rolleri: the ones staff management assigns; Veli is linked through student records (spec 0004). */
    public boolean isStaff() {
        return this != GUARDIAN;
    }
}

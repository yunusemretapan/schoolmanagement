package com.schoolos.domain.identity;

/** Hesap durumu (spec 0004, Req. 3): only {@link #ACTIVE} accounts sign in. */
public enum AccountStatus {
    /** Davet bekliyor: no password yet; the invitation link sets it (spec 0004, Req. 6). */
    INVITED,
    ACTIVE,
    /** Pasif: refused at sign-in and on every request; never deleted (spec 0004, Req. 8). */
    INACTIVE
}

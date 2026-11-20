package com.schoolos.application.features.identity.dtos;

import com.schoolos.domain.identity.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The staff detail screen (spec 0004, UX). {@code invitationExpiresAt} is set while a usable link exists;
 * {@code self} and {@code lastActiveAdmin} let the screen explain a blocked control (Req. 9) before the server refuses.
 */
public record StaffDetail(
        UUID id,
        String fullName,
        String email,
        String phone,
        List<String> roles,
        AccountStatus status,
        Instant invitationExpiresAt,
        boolean self,
        boolean lastActiveAdmin) {

    public StaffDetail {
        roles = List.copyOf(roles);
    }
}

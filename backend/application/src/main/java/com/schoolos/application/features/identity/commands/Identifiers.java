package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.rules.EmailNormalizer;
import com.schoolos.application.features.identity.rules.PhoneNumberNormalizer;
import com.schoolos.domain.identity.Role;
import java.util.Collection;
import java.util.stream.Collectors;

/** Normalized e-mail and phone (null when blank) and role lists for audit details. Inputs were validated first. */
final class Identifiers {

    private Identifiers() {}

    static String email(String raw) {
        return raw == null || raw.isBlank()
                ? null
                : EmailNormalizer.normalize(raw).orElseThrow();
    }

    static String phone(String raw) {
        return raw == null || raw.isBlank()
                ? null
                : PhoneNumberNormalizer.normalize(raw).orElseThrow();
    }

    /** "ADMIN,TEACHER": role names only, never a person's data (spec 0004, Req. 10). */
    static String roleList(Collection<Role> roles) {
        return roles.stream().sorted().map(Role::name).collect(Collectors.joining(","));
    }
}

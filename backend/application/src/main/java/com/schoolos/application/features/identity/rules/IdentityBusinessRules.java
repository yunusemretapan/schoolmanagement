package com.schoolos.application.features.identity.rules;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import java.util.HashMap;
import java.util.Map;

/** Rules for creating an account (spec 0003, Req. 1 and 3; AC-1). */
public final class IdentityBusinessRules {

    public static final int MIN_PASSWORD_LENGTH = 10;

    private final LoginIdentityRepository identities;

    public IdentityBusinessRules(LoginIdentityRepository identities) {
        this.identities = identities;
    }

    /** An e-mail or phone belongs to one account platform-wide, whichever institution asks. */
    public void ensureIdentifiersFree(String normalizedEmail, String normalizedPhone) {
        boolean taken = (normalizedEmail != null
                        && identities.findByEmail(normalizedEmail).isPresent())
                || (normalizedPhone != null
                        && identities.findByPhone(normalizedPhone).isPresent());
        if (taken) {
            throw new BusinessException(IdentityErrorCodes.IDENTIFIER_TAKEN, "Identifier already in use");
        }
    }

    /** Field checks shared by every account-creating command: at least one valid identifier, a long password. */
    public static Map<String, String> checkNewAccount(String email, String phone, String password) {
        Map<String, String> errors = new HashMap<>();
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();
        if (!hasEmail && !hasPhone) {
            errors.put("email", IdentityErrorCodes.IDENTIFIER_REQUIRED);
        }
        if (hasEmail && EmailNormalizer.normalize(email).isEmpty()) {
            errors.put("email", IdentityErrorCodes.INVALID_IDENTIFIER);
        }
        if (hasPhone && PhoneNumberNormalizer.normalize(phone).isEmpty()) {
            errors.put("phone", IdentityErrorCodes.INVALID_IDENTIFIER);
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            errors.put("password", IdentityErrorCodes.PASSWORD_TOO_SHORT);
        }
        return errors;
    }
}

package com.schoolos.application.features.identity.dtos;

/** After the password is set: the identifier the sign-in screen is filled with (spec 0004, Req. 6, G2). */
public record AcceptedInvitation(String identifier) {}

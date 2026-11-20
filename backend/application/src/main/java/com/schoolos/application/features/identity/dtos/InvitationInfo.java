package com.schoolos.application.features.identity.dtos;

/** The invitation screen's header: whom the link invites and to which institution (spec 0004, UX). */
public record InvitationInfo(String fullName, String institutionName) {}

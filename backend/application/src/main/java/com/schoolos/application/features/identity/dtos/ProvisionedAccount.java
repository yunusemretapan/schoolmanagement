package com.schoolos.application.features.identity.dtos;

import java.util.UUID;

/** Ids of what the operator tool created (ids only: no personal data in logs). */
public record ProvisionedAccount(UUID tenantId, UUID userId) {}

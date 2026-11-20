package com.schoolos.application.features.identity.events;

import com.schoolos.core.application.events.DomainEvent;
import java.util.UUID;

/**
 * A new institution was opened (spec 0003, Req. 1). Published inside the provisioning transaction, with the tenant
 * bound; spec 0005 creates the institution's first campus from it (Req. 3, ADR 0006).
 */
public record TenantProvisioned(UUID tenantId, String institutionName) implements DomainEvent {}

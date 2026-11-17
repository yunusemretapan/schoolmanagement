package com.schoolos.application.features.system.dtos;

/** Only status and version: no tenant, person or configuration data (spec 0001, Requirement 1). */
public record SystemStatusResponse(String status, String version) {}

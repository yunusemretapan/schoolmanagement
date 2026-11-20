package com.schoolos.application.features.institution.dtos;

import java.util.List;
import java.util.UUID;

/**
 * The class groups of one term (the active one unless another was asked for), by grade level order, then name
 * (spec 0005, Req. 6). {@code termId} is {@code null} while the institution has no term.
 */
public record ClassGroupList(UUID termId, List<ClassGroupView> items) {}

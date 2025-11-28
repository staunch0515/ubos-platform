package com.ubos.common.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthContext {
    private final String userId;
    private final String username;
    private final String role; // e.g., ADMIN, FINANCE, STAFF
    private final String tenantId; // For multi-tenant support
}
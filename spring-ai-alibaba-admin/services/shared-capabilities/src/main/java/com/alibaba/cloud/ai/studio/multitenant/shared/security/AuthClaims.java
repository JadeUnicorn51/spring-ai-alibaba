package com.alibaba.cloud.ai.studio.multitenant.shared.security;

import java.util.Set;

public record AuthClaims(String tenantId, String userId, String scope, Set<String> roleCodes) {
}

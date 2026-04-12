package com.alibaba.cloud.ai.studio.multitenant.shared.context;

import java.util.Set;

public record RequestContext(String tenantId, String userId, String scope, Set<String> roleCodes, String traceId) {

    public static RequestContext anonymous(String traceId) {
        return new RequestContext(null, null, "anonymous", Set.of(), traceId);
    }

}

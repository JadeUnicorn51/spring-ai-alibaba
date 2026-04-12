package com.alibaba.cloud.ai.studio.multitenant.platform.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cloud.ai.studio.multitenant.shared.api.ApiResponse;
import com.alibaba.cloud.ai.studio.multitenant.shared.context.RequestContext;
import com.alibaba.cloud.ai.studio.multitenant.shared.context.RequestContextHolder;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        RequestContext context = RequestContextHolder.get()
            .orElseGet(() -> RequestContext.anonymous(UUID.randomUUID().toString()));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service", "platform-service");
        payload.put("tenantId", context.tenantId());
        payload.put("userId", context.userId());
        payload.put("scope", context.scope());
        payload.put("traceId", context.traceId());
        return ApiResponse.success(payload);
    }

}

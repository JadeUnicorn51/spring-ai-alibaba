package com.alibaba.cloud.ai.studio.multitenant.shared.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.alibaba.cloud.ai.studio.multitenant.shared.context.RequestContext;
import com.alibaba.cloud.ai.studio.multitenant.shared.context.RequestContextHolder;

@Component
public class TenantRequestContextFilter extends OncePerRequestFilter {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    public static final String HEADER_USER_ID = "X-User-Id";

    public static final String HEADER_SCOPE = "X-Scope";

    public static final String HEADER_ROLE_CODES = "X-Role-Codes";

    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = valueOrDefault(request.getHeader(HEADER_TRACE_ID), UUID.randomUUID().toString());
        RequestContext requestContext = new RequestContext(normalize(request.getHeader(HEADER_TENANT_ID)),
                normalize(request.getHeader(HEADER_USER_ID)), valueOrDefault(request.getHeader(HEADER_SCOPE), "tenant"),
                splitRoleCodes(request.getHeader(HEADER_ROLE_CODES)), traceId);

        RequestContextHolder.set(requestContext);
        response.setHeader(HEADER_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        }
        finally {
            RequestContextHolder.clear();
        }
    }

    private Set<String> splitRoleCodes(String roleCodesHeader) {
        if (!StringUtils.hasText(roleCodesHeader)) {
            return Set.of();
        }
        return Arrays.stream(roleCodesHeader.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String valueOrDefault(String source, String defaultValue) {
        String normalized = normalize(source);
        return normalized == null ? defaultValue : normalized;
    }

}

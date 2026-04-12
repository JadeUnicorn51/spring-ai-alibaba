package com.alibaba.cloud.ai.studio.multitenant.shared.context;

import java.util.Optional;

import org.springframework.core.NamedThreadLocal;

public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> HOLDER = new NamedThreadLocal<>("multi-tenant-request-context");

    private RequestContextHolder() {
    }

    public static void set(RequestContext requestContext) {
        HOLDER.set(requestContext);
    }

    public static Optional<RequestContext> get() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static void clear() {
        HOLDER.remove();
    }

}

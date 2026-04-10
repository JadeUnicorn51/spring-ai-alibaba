/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.studio.core.context;

import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.TenantContext;

/**
 * A holder class for managing tenant context in a thread-local manner.
 * Provides methods to set, get and clear the tenant context for the current thread.
 *
 * @since 1.0.0
 */
public class TenantContextHolder {

    private static final ThreadLocal<TenantContext> TENANT_CONTEXT = new ThreadLocal<>();

    /**
     * Sets the tenant context for the current thread
     * @param context the tenant context to be set
     */
    public static void setContext(TenantContext context) {
        TENANT_CONTEXT.set(context);
    }

    /**
     * Gets the tenant context for the current thread
     * @return the current thread's tenant context
     */
    public static TenantContext getContext() {
        return TENANT_CONTEXT.get();
    }

    /**
     * Gets the tenant ID from current context
     * @return tenant ID or null if not set
     */
    public static String getTenantId() {
        TenantContext ctx = TENANT_CONTEXT.get();
        return ctx != null ? ctx.getTenantId() : null;
    }

    /**
     * Gets the account ID from current context
     * @return account ID or null if not set
     */
    public static String getAccountId() {
        TenantContext ctx = TENANT_CONTEXT.get();
        return ctx != null ? ctx.getAccountId() : null;
    }

    /**
     * Gets the workspace ID from current context
     * @return workspace ID or null if not set
     */
    public static String getWorkspaceId() {
        TenantContext ctx = TENANT_CONTEXT.get();
        return ctx != null ? ctx.getWorkspaceId() : null;
    }

    /**
     * Check if current context is platform level (SUPER_ADMIN)
     */
    public static boolean isPlatformLevel() {
        TenantContext ctx = TENANT_CONTEXT.get();
        return ctx != null && ctx.isPlatformLevel();
    }

    /**
     * Clear the tenant context for the current thread
     */
    public static void clear() {
        TENANT_CONTEXT.remove();
    }

    /**
     * Initialize tenant context from request context
     * @param requestContext the request context
     */
    public static void initFromRequestContext(RequestContext requestContext) {
        TenantContext ctx = new TenantContext();
        ctx.setTenantId(requestContext.getTenantId());
        ctx.setAccountId(requestContext.getAccountId());
        ctx.setWorkspaceId(requestContext.getWorkspaceId());
        ctx.setAccountType(requestContext.getAccountType());
        ctx.setRequestId(requestContext.getRequestId());
        setContext(ctx);
    }

}

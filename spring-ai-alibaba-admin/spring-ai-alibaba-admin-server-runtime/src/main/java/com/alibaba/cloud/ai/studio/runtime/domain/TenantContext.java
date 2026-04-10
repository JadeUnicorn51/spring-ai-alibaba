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

package com.alibaba.cloud.ai.studio.runtime.domain;

import com.alibaba.cloud.ai.studio.runtime.enums.AccountType;
import lombok.Data;

import java.io.Serializable;

/**
 * Tenant context for multi-tenancy support.
 * Stores tenant-specific information in ThreadLocal.
 *
 * @since 1.0.0
 */
@Data
public class TenantContext implements Serializable {

    /** Tenant identifier - null means platform level (SUPER_ADMIN) */
    private String tenantId;

    /** Account identifier */
    private String accountId;

    /** Workspace identifier */
    private String workspaceId;

    /** Account type */
    private AccountType accountType;

    /** Request ID for tracing */
    private String requestId;

    /**
     * Check if current context is platform level (SUPER_ADMIN)
     */
    public boolean isPlatformLevel() {
        return tenantId == null && AccountType.SUPER_ADMIN == accountType;
    }

    /**
     * Check if current context is tenant level
     */
    public boolean isTenantLevel() {
        return tenantId != null;
    }

}

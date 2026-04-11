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

package com.alibaba.cloud.ai.studio.core.base.service;

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantGovernanceAuditLog;

/**
 * Service for tenant governance audit logs.
 *
 * @since 1.0.0
 */
public interface TenantGovernanceAuditLogService {

	/**
	 * Records one tenant governance audit log.
	 * @param tenantId tenant id
	 * @param operation governance operation
	 * @param targetAccountId target account id
	 * @param details request details
	 */
	void recordTenantAdminAudit(String tenantId, String operation, String targetAccountId, Object details);

	/**
	 * Lists tenant governance audit logs.
	 * @param tenantId tenant id
	 * @param query pagination query
	 * @return paged logs
	 */
	PagingList<TenantGovernanceAuditLog> listTenantAdminAudits(String tenantId, BaseQuery query);

}

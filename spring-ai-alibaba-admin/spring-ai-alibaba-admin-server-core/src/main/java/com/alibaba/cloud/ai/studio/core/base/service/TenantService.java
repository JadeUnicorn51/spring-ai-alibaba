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

import com.alibaba.cloud.ai.studio.core.base.entity.TenantEntity;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.Tenant;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantCreateRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantQuotaDTO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Tenant management service interface.
 * Provides operations for tenant CRUD, quota management, and billing.
 *
 * @since 1.0.0
 */
public interface TenantService extends IService<TenantEntity> {

	/**
	 * Creates a new tenant
	 * @param request Tenant creation request
	 * @return Tenant ID
	 */
	String createTenant(TenantCreateRequest request);

	/**
	 * Updates tenant information
	 * @param tenantId Tenant ID
	 * @param tenant Updated tenant information
	 */
	void updateTenant(String tenantId, Tenant tenant);

	/**
	 * Updates tenant quota
	 * @param tenantId Tenant ID
	 * @param quota New quota settings
	 */
	void updateTenantQuota(String tenantId, TenantQuotaDTO quota);

	/**
	 * Gets tenant by ID
	 * @param tenantId Tenant ID
	 * @return Tenant details
	 */
	Tenant getTenant(String tenantId);

	/**
	 * Lists tenants with pagination
	 * @param query Query parameters
	 * @return Paginated list of tenants
	 */
	PagingList<Tenant> listTenants(BaseQuery query);

	/**
	 * Enables a tenant
	 * @param tenantId Tenant ID
	 */
	void enableTenant(String tenantId);

	/**
	 * Disables a tenant
	 * @param tenantId Tenant ID
	 */
	void disableTenant(String tenantId);

	/**
	 * Checks if tenant quota is exceeded
	 * @param tenantId Tenant ID
	 * @param resourceType Resource type (users, apps, workspaces)
	 * @param count Current count
	 * @return true if quota would be exceeded
	 */
	boolean isQuotaExceeded(String tenantId, String resourceType, int count);

}

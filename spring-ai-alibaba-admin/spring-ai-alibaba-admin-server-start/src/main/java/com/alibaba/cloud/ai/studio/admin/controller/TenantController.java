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

package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.core.base.service.TenantService;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.Tenant;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantCreateRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantQuotaDTO;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.context.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * Tenant management controller for platform administrators.
 * Provides RESTful APIs for CRUD operations on tenants and quota management.
 *
 * @since 1.0.0
 */
@RestController
@Tag(name = "tenant management")
@RequestMapping("/admin/v1/tenants")
public class TenantController {

	private final TenantService tenantService;

	public TenantController(TenantService tenantService) {
		this.tenantService = tenantService;
	}

	/**
	 * Creates a new tenant
	 * @param request Tenant creation request
	 * @return Tenant ID
	 */
	@PostMapping
	@Operation(summary = "Create tenant", description = "Creates a new tenant with specified quota settings")
	public Result<String> createTenant(@RequestBody TenantCreateRequest request) {
		validatePlatformAdmin();

		if (request == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("request"));
		}

		String tenantId = tenantService.createTenant(request);
		return Result.success(getRequestId(), tenantId);
	}

	/**
	 * Gets tenant by ID
	 * @param tenantId Tenant ID
	 * @return Tenant details
	 */
	@GetMapping("/{tenantId}")
	@Operation(summary = "Get tenant", description = "Gets tenant details by ID")
	public Result<Tenant> getTenant(@PathVariable("tenantId") String tenantId) {
		validatePlatformAdmin();

		Tenant tenant = tenantService.getTenant(tenantId);
		if (tenant == null) {
			throw new BizException(ErrorCode.TENANT_NOT_FOUND.toError());
		}
		return Result.success(getRequestId(), tenant);
	}

	/**
	 * Lists all tenants with pagination
	 * @param query Query parameters
	 * @return Paginated list of tenants
	 */
	@GetMapping
	@Operation(summary = "List tenants", description = "Lists all tenants with pagination")
	public Result<PagingList<Tenant>> listTenants(BaseQuery query) {
		validatePlatformAdmin();

		PagingList<Tenant> tenants = tenantService.listTenants(query);
		return Result.success(getRequestId(), tenants);
	}

	/**
	 * Updates tenant information
	 * @param tenantId Tenant ID
	 * @param tenant Updated tenant information
	 * @return Success status
	 */
	@PutMapping("/{tenantId}")
	@Operation(summary = "Update tenant", description = "Updates tenant basic information")
	public Result<Void> updateTenant(@PathVariable("tenantId") String tenantId,
			@RequestBody Tenant tenant) {
		validatePlatformAdmin();

		tenantService.updateTenant(tenantId, tenant);
		return Result.success(getRequestId(), null);
	}

	/**
	 * Updates tenant quota settings
	 * @param tenantId Tenant ID
	 * @param quota New quota settings
	 * @return Success status
	 */
	@PutMapping("/{tenantId}/quota")
	@Operation(summary = "Update tenant quota", description = "Updates tenant quota settings (max users, apps, etc.)")
	public Result<Void> updateQuota(@PathVariable("tenantId") String tenantId,
			@RequestBody TenantQuotaDTO quota) {
		validatePlatformAdmin();

		tenantService.updateTenantQuota(tenantId, quota);
		return Result.success(getRequestId(), null);
	}

	/**
	 * Enables a tenant
	 * @param tenantId Tenant ID
	 * @return Success status
	 */
	@PostMapping("/{tenantId}/enable")
	@Operation(summary = "Enable tenant", description = "Enables an inactive tenant")
	public Result<Void> enableTenant(@PathVariable("tenantId") String tenantId) {
		validatePlatformAdmin();

		tenantService.enableTenant(tenantId);
		return Result.success(getRequestId(), null);
	}

	/**
	 * Disables a tenant
	 * @param tenantId Tenant ID
	 * @return Success status
	 */
	@PostMapping("/{tenantId}/disable")
	@Operation(summary = "Disable tenant", description = "Disables an active tenant")
	public Result<Void> disableTenant(@PathVariable("tenantId") String tenantId) {
		validatePlatformAdmin();

		tenantService.disableTenant(tenantId);
		return Result.success(getRequestId(), null);
	}

	/**
	 * Validates that the current user is a platform administrator
	 */
	private void validatePlatformAdmin() {
		if (!TenantContextHolder.isPlatformLevel()) {
			throw new BizException(ErrorCode.PERMISSION_DENIED.toError());
		}
	}

	/**
	 * Gets the current request ID
	 */
	private String getRequestId() {
		var requestContext = RequestContextHolder.getRequestContext();
		return requestContext != null ? requestContext.getRequestId() : null;
	}

}

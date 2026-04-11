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

import com.alibaba.cloud.ai.studio.core.base.service.AccountService;
import com.alibaba.cloud.ai.studio.core.base.service.TenantService;
import com.alibaba.cloud.ai.studio.core.base.service.TenantGovernanceAuditLogService;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Account;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.Tenant;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantAdminCreateRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantAdminResetPasswordRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantCreateRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantGovernanceAuditLog;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantQuotaDTO;
import com.alibaba.cloud.ai.studio.runtime.enums.AccountStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.AccountType;
import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.context.TenantContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

	private final AccountService accountService;

	private final TenantGovernanceAuditLogService tenantGovernanceAuditLogService;

	public TenantController(TenantService tenantService, AccountService accountService,
			TenantGovernanceAuditLogService tenantGovernanceAuditLogService) {
		this.tenantService = tenantService;
		this.accountService = accountService;
		this.tenantGovernanceAuditLogService = tenantGovernanceAuditLogService;
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
	 * Lists tenant administrator accounts under the specified tenant.
	 * @param tenantId Tenant ID
	 * @param query Query parameters
	 * @return Paginated list of tenant admins
	 */
	@GetMapping("/{tenantId}/admins")
	@Operation(summary = "List tenant admins",
			description = "Lists tenant administrator accounts under a tenant")
	public Result<PagingList<Account>> listTenantAdmins(@PathVariable("tenantId") String tenantId,
			BaseQuery query) {
		validatePlatformAdmin();

		Tenant tenant = tenantService.getTenant(tenantId);
		if (tenant == null) {
			throw new BizException(ErrorCode.TENANT_NOT_FOUND.toError());
		}

		PagingList<Account> accounts = accountService.listTenantAdmins(tenantId, query);
		return Result.success(getRequestId(), accounts);
	}

	/**
	 * Lists tenant governance audit logs under the specified tenant.
	 * @param tenantId Tenant ID
	 * @param query Query parameters
	 * @return Paginated list of governance audit logs
	 */
	@GetMapping("/{tenantId}/admin-audits")
	@Operation(summary = "List tenant admin audits",
			description = "Lists governance audit logs for tenant administrator operations")
	public Result<PagingList<TenantGovernanceAuditLog>> listTenantAdminAudits(
			@PathVariable("tenantId") String tenantId, BaseQuery query) {
		validatePlatformAdmin();
		validateTenantExists(tenantId);

		PagingList<TenantGovernanceAuditLog> logs = tenantGovernanceAuditLogService.listTenantAdminAudits(tenantId,
				query);
		return Result.success(getRequestId(), logs);
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
	 * Creates a tenant administrator account under the specified tenant.
	 * @param tenantId Tenant ID
	 * @param request Tenant admin creation request
	 * @return Created account ID
	 */
	@PostMapping("/{tenantId}/admins")
	@Operation(summary = "Create tenant admin", description = "Creates a tenant administrator account for the tenant")
	public Result<String> createTenantAdmin(@PathVariable("tenantId") String tenantId,
			@RequestBody TenantAdminCreateRequest request) {
		long start = System.currentTimeMillis();
		validatePlatformAdmin();

		if (request == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("request"));
		}
		if (StringUtils.isBlank(request.getUsername())) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("username"));
		}
		if (StringUtils.isBlank(request.getPassword())) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("password"));
		}

		Tenant tenant = tenantService.getTenant(tenantId);
		if (tenant == null) {
			throw new BizException(ErrorCode.TENANT_NOT_FOUND.toError());
		}
		if (!CommonStatus.NORMAL.getStatus().equals(tenant.getStatus())) {
			throw new BizException(ErrorCode.TENANT_DISABLED.toError());
		}

		Account account = new Account();
		account.setUsername(request.getUsername());
		account.setPassword(request.getPassword());
		account.setNickname(request.getNickname());
		account.setEmail(request.getEmail());
		account.setMobile(request.getMobile());
		account.setType(AccountType.TENANT_ADMIN);
		account.setTenantId(tenantId);

		String accountId = accountService.createAccount(account);
		auditTenantAdminAction("createTenantAdmin", start,
				Map.of("tenantId", tenantId, "accountId", accountId, "username", request.getUsername()));
		return Result.success(getRequestId(), accountId);
	}

	/**
	 * Enables a tenant administrator account.
	 * @param tenantId Tenant ID
	 * @param accountId Account ID
	 * @return Success status
	 */
	@PostMapping("/{tenantId}/admins/{accountId}/enable")
	@Operation(summary = "Enable tenant admin", description = "Enables a tenant administrator account")
	public Result<Void> enableTenantAdmin(@PathVariable("tenantId") String tenantId,
			@PathVariable("accountId") String accountId) {
		long start = System.currentTimeMillis();
		validatePlatformAdmin();
		validateTenantExists(tenantId);

		accountService.updateTenantAdminStatus(tenantId, accountId, AccountStatus.NORMAL);
		auditTenantAdminAction("enableTenantAdmin", start,
				Map.of("tenantId", tenantId, "accountId", accountId, "status", AccountStatus.NORMAL.getValue()));
		return Result.success(getRequestId(), null);
	}

	/**
	 * Disables a tenant administrator account.
	 * @param tenantId Tenant ID
	 * @param accountId Account ID
	 * @return Success status
	 */
	@PostMapping("/{tenantId}/admins/{accountId}/disable")
	@Operation(summary = "Disable tenant admin", description = "Disables a tenant administrator account")
	public Result<Void> disableTenantAdmin(@PathVariable("tenantId") String tenantId,
			@PathVariable("accountId") String accountId) {
		long start = System.currentTimeMillis();
		validatePlatformAdmin();
		validateTenantExists(tenantId);

		accountService.updateTenantAdminStatus(tenantId, accountId, AccountStatus.DISABLED);
		auditTenantAdminAction("disableTenantAdmin", start,
				Map.of("tenantId", tenantId, "accountId", accountId, "status", AccountStatus.DISABLED.getValue()));
		return Result.success(getRequestId(), null);
	}

	/**
	 * Resets password of a tenant administrator account.
	 * @param tenantId Tenant ID
	 * @param accountId Account ID
	 * @param request Reset password request
	 * @return Success status
	 */
	@PutMapping("/{tenantId}/admins/{accountId}/password")
	@Operation(summary = "Reset tenant admin password", description = "Resets password of a tenant administrator account")
	public Result<Void> resetTenantAdminPassword(@PathVariable("tenantId") String tenantId,
			@PathVariable("accountId") String accountId,
			@RequestBody TenantAdminResetPasswordRequest request) {
		long start = System.currentTimeMillis();
		validatePlatformAdmin();
		validateTenantExists(tenantId);

		if (request == null || StringUtils.isBlank(request.getNewPassword())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("newPassword"));
		}

		accountService.resetTenantAdminPassword(tenantId, accountId, request.getNewPassword());
		auditTenantAdminAction("resetTenantAdminPassword", start,
				Map.of("tenantId", tenantId, "accountId", accountId));
		return Result.success(getRequestId(), null);
	}

	/**
	 * Deletes (soft delete) a tenant administrator account.
	 * @param tenantId Tenant ID
	 * @param accountId Account ID
	 * @return Success status
	 */
	@DeleteMapping("/{tenantId}/admins/{accountId}")
	@Operation(summary = "Delete tenant admin", description = "Deletes a tenant administrator account")
	public Result<Void> deleteTenantAdmin(@PathVariable("tenantId") String tenantId,
			@PathVariable("accountId") String accountId) {
		long start = System.currentTimeMillis();
		validatePlatformAdmin();
		validateTenantExists(tenantId);

		accountService.deleteTenantAdmin(tenantId, accountId);
		auditTenantAdminAction("deleteTenantAdmin", start, Map.of("tenantId", tenantId, "accountId", accountId));
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

	private void validateTenantExists(String tenantId) {
		Tenant tenant = tenantService.getTenant(tenantId);
		if (tenant == null) {
			throw new BizException(ErrorCode.TENANT_NOT_FOUND.toError());
		}
	}

	private void auditTenantAdminAction(String action, long start, Object details) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LogUtils.monitor(context, "PlatformTenantAdminGovernance", action, start, LogUtils.SUCCESS, details, null);
		if (details instanceof Map<?, ?> detailMap) {
			Object tenantIdObj = detailMap.get("tenantId");
			String tenantId = tenantIdObj == null ? null : String.valueOf(tenantIdObj);
			Object targetAccountIdObj = detailMap.get("accountId");
			String targetAccountId = targetAccountIdObj == null ? null : String.valueOf(targetAccountIdObj);
			tenantGovernanceAuditLogService.recordTenantAdminAudit(tenantId, action, targetAccountId, detailMap);
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

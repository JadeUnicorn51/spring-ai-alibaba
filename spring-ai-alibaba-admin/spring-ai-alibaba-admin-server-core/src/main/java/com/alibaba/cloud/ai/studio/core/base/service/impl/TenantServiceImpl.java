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

package com.alibaba.cloud.ai.studio.core.base.service.impl;

import com.alibaba.cloud.ai.studio.core.base.entity.TenantEntity;
import com.alibaba.cloud.ai.studio.core.base.mapper.TenantMapper;
import com.alibaba.cloud.ai.studio.core.base.service.TenantService;
import com.alibaba.cloud.ai.studio.core.context.TenantContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.Tenant;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantCreateRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantQuotaDTO;
import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tenant service implementation for multi-tenant platform management.
 *
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl extends ServiceImpl<TenantMapper, TenantEntity> implements TenantService {

	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	@Override
	public String createTenant(TenantCreateRequest request) {
		// Validate platform admin permission
		validatePlatformAdmin();

		if (request.getName() == null || request.getName().isBlank()) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}

		String tenantId = IdGenerator.idStr();

		TenantEntity entity = new TenantEntity();
		entity.setTenantId(tenantId);
		entity.setName(request.getName());
		entity.setDescription(request.getDescription());
		entity.setStatus(CommonStatus.NORMAL.getStatus());
		entity.setMaxUsers(request.getMaxUsers());
		entity.setMaxApps(request.getMaxApps());
		entity.setMaxWorkspaces(request.getMaxWorkspaces());
		entity.setMaxStorageGb(request.getMaxStorageGb());
		entity.setMaxApiCallsPerDay(request.getMaxApiCallsPerDay());
		entity.setGmtCreate(new Date());
		entity.setGmtModified(new Date());

		if (request.getExpireDate() != null && !request.getExpireDate().isBlank()) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
				entity.setExpireDate(sdf.parse(request.getExpireDate()));
			}
			catch (ParseException e) {
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("expire_date"));
			}
		}

		this.save(entity);
		return tenantId;
	}

	@Override
	public void updateTenant(String tenantId, Tenant tenant) {
		validatePlatformAdmin();

		TenantEntity entity = getByTenantId(tenantId);
		if (entity == null) {
			throw new BizException(ErrorCode.TENANT_NOT_FOUND.toError());
		}

		if (tenant.getName() != null) {
			entity.setName(tenant.getName());
		}
		if (tenant.getDescription() != null) {
			entity.setDescription(tenant.getDescription());
		}
		entity.setGmtModified(new Date());

		this.updateById(entity);
	}

	@Override
	public void updateTenantQuota(String tenantId, TenantQuotaDTO quota) {
		validatePlatformAdmin();

		TenantEntity entity = getByTenantId(tenantId);
		if (entity == null) {
			throw new BizException(ErrorCode.TENANT_NOT_FOUND.toError());
		}

		if (quota.getMaxUsers() != null) {
			entity.setMaxUsers(quota.getMaxUsers());
		}
		if (quota.getMaxApps() != null) {
			entity.setMaxApps(quota.getMaxApps());
		}
		if (quota.getMaxWorkspaces() != null) {
			entity.setMaxWorkspaces(quota.getMaxWorkspaces());
		}
		if (quota.getMaxStorageGb() != null) {
			entity.setMaxStorageGb(quota.getMaxStorageGb());
		}
		if (quota.getMaxApiCallsPerDay() != null) {
			entity.setMaxApiCallsPerDay(quota.getMaxApiCallsPerDay());
		}
		entity.setGmtModified(new Date());

		this.updateById(entity);
	}

	@Override
	public Tenant getTenant(String tenantId) {
		TenantEntity entity = getByTenantId(tenantId);
		if (entity == null) {
			return null;
		}
		return toTenantDTO(entity);
	}

	@Override
	public PagingList<Tenant> listTenants(BaseQuery query) {
		validatePlatformAdmin();

		LambdaQueryWrapper<TenantEntity> queryWrapper = new LambdaQueryWrapper<>();
		if (query.getName() != null && !query.getName().isBlank()) {
			queryWrapper.like(TenantEntity::getName, query.getName());
		}
		queryWrapper.orderByDesc(TenantEntity::getGmtCreate);

		Page<TenantEntity> page = new Page<>(query.getCurrent(), query.getSize());
		IPage<TenantEntity> pageResult = this.page(page, queryWrapper);

		List<Tenant> tenants;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			tenants = new ArrayList<>();
		}
		else {
			tenants = pageResult.getRecords().stream().map(this::toTenantDTO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), tenants);
	}

	@Override
	public void enableTenant(String tenantId) {
		validatePlatformAdmin();

		TenantEntity entity = getByTenantId(tenantId);
		if (entity == null) {
			throw new BizException(ErrorCode.TENANT_NOT_FOUND.toError());
		}

		entity.setStatus(CommonStatus.NORMAL.getStatus());
		entity.setGmtModified(new Date());
		this.updateById(entity);
	}

	@Override
	public void disableTenant(String tenantId) {
		validatePlatformAdmin();

		TenantEntity entity = getByTenantId(tenantId);
		if (entity == null) {
			throw new BizException(ErrorCode.TENANT_NOT_FOUND.toError());
		}

		entity.setStatus(CommonStatus.DELETED.getStatus());
		entity.setGmtModified(new Date());
		this.updateById(entity);
	}

	@Override
	public boolean isQuotaExceeded(String tenantId, String resourceType, int count) {
		TenantEntity entity = getByTenantId(tenantId);
		if (entity == null) {
			return false;
		}

		return switch (resourceType) {
			case "users" -> entity.getMaxUsers() != null && count >= entity.getMaxUsers();
			case "apps" -> entity.getMaxApps() != null && count >= entity.getMaxApps();
			case "workspaces" -> entity.getMaxWorkspaces() != null && count >= entity.getMaxWorkspaces();
			default -> false;
		};
	}

	/**
	 * Gets tenant entity by tenant ID
	 * @param tenantId Tenant ID
	 * @return Tenant entity or null
	 */
	private TenantEntity getByTenantId(String tenantId) {
		LambdaQueryWrapper<TenantEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(TenantEntity::getTenantId, tenantId);
		return this.getOne(queryWrapper);
	}

	/**
	 * Converts tenant entity to DTO
	 * @param entity Tenant entity
	 * @return Tenant DTO
	 */
	private Tenant toTenantDTO(TenantEntity entity) {
		if (entity == null) {
			return null;
		}
		return BeanCopierUtils.copy(entity, Tenant.class);
	}

	/**
	 * Validates that the current user is a platform administrator
	 */
	private void validatePlatformAdmin() {
		if (!TenantContextHolder.isPlatformLevel()) {
			throw new BizException(ErrorCode.PERMISSION_DENIED.toError());
		}
	}

}

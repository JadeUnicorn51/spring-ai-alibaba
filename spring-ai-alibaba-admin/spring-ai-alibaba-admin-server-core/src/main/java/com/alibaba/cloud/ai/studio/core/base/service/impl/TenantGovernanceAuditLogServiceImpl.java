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

import com.alibaba.cloud.ai.studio.core.base.entity.TenantGovernanceAuditLogEntity;
import com.alibaba.cloud.ai.studio.core.base.mapper.TenantGovernanceAuditLogMapper;
import com.alibaba.cloud.ai.studio.core.base.service.TenantGovernanceAuditLogService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.tenant.TenantGovernanceAuditLog;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service implementation for tenant governance audit logs.
 *
 * @since 1.0.0
 */
@Service
public class TenantGovernanceAuditLogServiceImpl
		extends ServiceImpl<TenantGovernanceAuditLogMapper, TenantGovernanceAuditLogEntity>
		implements TenantGovernanceAuditLogService {

	@Override
	public void recordTenantAdminAudit(String tenantId, String operation, String targetAccountId, Object details) {
		if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(operation)) {
			return;
		}

		RequestContext context = RequestContextHolder.getRequestContext();

		TenantGovernanceAuditLogEntity entity = new TenantGovernanceAuditLogEntity();
		entity.setTenantId(tenantId);
		entity.setOperation(operation);
		entity.setTargetAccountId(targetAccountId);
		entity.setDetails(details == null ? null : JsonUtils.toJson(details));
		entity.setOperatorAccountId(context == null ? null : context.getAccountId());
		entity.setRequestId(context == null ? null : context.getRequestId());
		entity.setGmtCreate(new Date());
		this.save(entity);
	}

	@Override
	public PagingList<TenantGovernanceAuditLog> listTenantAdminAudits(String tenantId, BaseQuery query) {
		if (StringUtils.isBlank(tenantId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("tenant_id"));
		}

		BaseQuery safeQuery = query == null ? new BaseQuery() : query;

		LambdaQueryWrapper<TenantGovernanceAuditLogEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(TenantGovernanceAuditLogEntity::getTenantId, tenantId);
		if (StringUtils.isNotBlank(safeQuery.getName())) {
			queryWrapper.and(wrapper -> wrapper.like(TenantGovernanceAuditLogEntity::getOperation, safeQuery.getName())
				.or()
				.like(TenantGovernanceAuditLogEntity::getOperatorAccountId, safeQuery.getName())
				.or()
				.like(TenantGovernanceAuditLogEntity::getTargetAccountId, safeQuery.getName()));
		}
		queryWrapper.orderByDesc(TenantGovernanceAuditLogEntity::getGmtCreate)
			.orderByDesc(TenantGovernanceAuditLogEntity::getId);

		Page<TenantGovernanceAuditLogEntity> page = new Page<>(safeQuery.getCurrent(), safeQuery.getSize());
		IPage<TenantGovernanceAuditLogEntity> pageResult = this.page(page, queryWrapper);

		List<TenantGovernanceAuditLog> logs;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			logs = new ArrayList<>();
		}
		else {
			logs = pageResult.getRecords().stream().map(this::toDomain).toList();
		}

		return new PagingList<>(safeQuery.getCurrent(), safeQuery.getSize(), pageResult.getTotal(), logs);
	}

	private TenantGovernanceAuditLog toDomain(TenantGovernanceAuditLogEntity entity) {
		return BeanCopierUtils.copy(entity, TenantGovernanceAuditLog.class);
	}

}

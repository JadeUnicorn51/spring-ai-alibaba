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

package com.alibaba.cloud.ai.studio.core.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Entity for tenant governance audit logs.
 *
 * @since 1.0.0
 */
@Data
@TableName("tenant_governance_audit_log")
public class TenantGovernanceAuditLogEntity {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("operation")
	private String operation;

	@TableField("operator_account_id")
	private String operatorAccountId;

	@TableField("target_account_id")
	private String targetAccountId;

	@TableField("request_id")
	private String requestId;

	@TableField("details")
	private String details;

	@TableField("gmt_create")
	private Date gmtCreate;

}

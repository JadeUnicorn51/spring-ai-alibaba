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

package com.alibaba.cloud.ai.studio.runtime.domain.tenant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Platform governance audit log for tenant administration actions.
 *
 * @since 1.0.0
 */
@Data
public class TenantGovernanceAuditLog implements Serializable {

	/** Primary key */
	private Long id;

	/** Tenant identifier */
	@JsonProperty("tenant_id")
	private String tenantId;

	/** Governance action name */
	private String operation;

	/** Operator account id */
	@JsonProperty("operator_account_id")
	private String operatorAccountId;

	/** Target account id */
	@JsonProperty("target_account_id")
	private String targetAccountId;

	/** Request id */
	@JsonProperty("request_id")
	private String requestId;

	/** Operation details in JSON */
	private String details;

	/** Creation time */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

}

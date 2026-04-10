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
 * Tenant domain object representing a tenant in the multi-tenant platform.
 *
 * @since 1.0.0
 */
@Data
public class Tenant implements Serializable {

	/** Tenant identifier */
	@JsonProperty("tenant_id")
	private String tenantId;

	/** Tenant name */
	private String name;

	/** Tenant description */
	private String description;

	/** Tenant status (1=active, 0=inactive) */
	private Integer status;

	/** Maximum number of users allowed */
	@JsonProperty("max_users")
	private Integer maxUsers;

	/** Maximum number of applications allowed */
	@JsonProperty("max_apps")
	private Integer maxApps;

	/** Maximum number of workspaces allowed */
	@JsonProperty("max_workspaces")
	private Integer maxWorkspaces;

	/** Maximum storage in GB */
	@JsonProperty("max_storage_gb")
	private Long maxStorageGb;

	/** Maximum API calls per day */
	@JsonProperty("max_api_calls_per_day")
	private Long maxApiCallsPerDay;

	/** Tenant expiration date */
	@JsonProperty("expire_date")
	private Date expireDate;

	/** Creation timestamp */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

}

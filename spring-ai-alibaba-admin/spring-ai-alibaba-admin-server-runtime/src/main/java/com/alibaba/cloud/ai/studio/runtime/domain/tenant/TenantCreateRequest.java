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

/**
 * Request object for creating a new tenant.
 *
 * @since 1.0.0
 */
@Data
public class TenantCreateRequest {

	/** Tenant name (required) */
	private String name;

	/** Tenant description */
	private String description;

	/** Maximum number of users allowed (default: 10) */
	@JsonProperty("max_users")
	private Integer maxUsers = 10;

	/** Maximum number of applications allowed (default: 50) */
	@JsonProperty("max_apps")
	private Integer maxApps = 50;

	/** Maximum number of workspaces allowed (default: 5) */
	@JsonProperty("max_workspaces")
	private Integer maxWorkspaces = 5;

	/** Maximum storage in GB (default: 100) */
	@JsonProperty("max_storage_gb")
	private Long maxStorageGb = 100L;

	/** Maximum API calls per day (default: 10000) */
	@JsonProperty("max_api_calls_per_day")
	private Long maxApiCallsPerDay = 10000L;

	/** Tenant expiration date (optional) */
	@JsonProperty("expire_date")
	private String expireDate;

}

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

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * Tenant entity for multi-tenancy support.
 *
 * @since 1.0.0
 */
@Data
@TableName("tenant")
public class TenantEntity {

    /** Primary key */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** Unique tenant identifier */
    @TableField("tenant_id")
    private String tenantId;

    /** Tenant name */
    private String name;

    /** Tenant description */
    private String description;

    /** Status: 0-disabled, 1-normal */
    private Integer status;

    /** Max users quota */
    @TableField("max_users")
    private Integer maxUsers;

    /** Max applications quota */
    @TableField("max_apps")
    private Integer maxApps;

    /** Max workspaces quota */
    @TableField("max_workspaces")
    private Integer maxWorkspaces;

    /** Max storage quota in GB */
    @TableField("max_storage_gb")
    private Long maxStorageGb;

    /** Max API calls per day quota */
    @TableField("max_api_calls_per_day")
    private Long maxApiCallsPerDay;

    /** Expiration date */
    @TableField("expire_date")
    private Date expireDate;

    /** Creation timestamp */
    @TableField("gmt_create")
    private Date gmtCreate;

    /** Last modification timestamp */
    @TableField("gmt_modified")
    private Date gmtModified;

    /** Creator uid */
    private String creator;

    /** Modifier uid */
    private String modifier;

}

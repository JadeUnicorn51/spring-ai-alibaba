/*
 * Copyright 2026 the original author or authors.
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

import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Skill entity for persistent reusable skill bundles.
 *
 * @since 1.0.0.3
 */
@Data
@TableName("skill")
public class SkillEntity {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("skill_id")
	private String skillId;

	@TableField("workspace_id")
	private String workspaceId;

	/** Tenant identifier - null means platform level. */
	@TableField("tenant_id")
	private String tenantId;

	private CommonStatus status;

	private String name;

	private String description;

	private String instruction;

	/** Enabled flag stored as tinyint(0/1). */
	private Integer enabled;

	@TableField("tool_ids")
	private String toolIds;

	@TableField("mcp_server_ids")
	private String mcpServerIds;

	@TableField("agent_component_ids")
	private String agentComponentIds;

	@TableField("workflow_component_ids")
	private String workflowComponentIds;

	@TableField("gmt_create")
	private Date gmtCreate;

	@TableField("gmt_modified")
	private Date gmtModified;

	private String creator;

	private String modifier;

}


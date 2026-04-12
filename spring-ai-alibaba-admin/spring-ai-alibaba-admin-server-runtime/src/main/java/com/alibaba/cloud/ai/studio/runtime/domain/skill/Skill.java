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

package com.alibaba.cloud.ai.studio.runtime.domain.skill;

import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Skill resource for reusable tool bundles.
 *
 * @since 1.0.0.3
 */
@Data
public class Skill implements Serializable {

	/** Unique skill id. */
	@JsonProperty("skill_id")
	private String skillId;

	/** Skill status. */
	private CommonStatus status;

	/** Skill name. */
	private String name;

	/** Skill description. */
	private String description;

	/** Optional prompt instruction patch. */
	private String instruction;

	/** Whether this skill is enabled. */
	private Boolean enabled;

	/** Bound plugin tool ids. */
	@JsonProperty("tool_ids")
	private List<String> toolIds;

	/** Bound MCP server ids. */
	@JsonProperty("mcp_server_ids")
	private List<String> mcpServerIds;

	/** Bound agent component ids. */
	@JsonProperty("agent_component_ids")
	private List<String> agentComponentIds;

	/** Bound workflow component ids. */
	@JsonProperty("workflow_component_ids")
	private List<String> workflowComponentIds;

	/** Creation time. */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modified time. */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/** Creator account id. */
	private String creator;

	/** Last modifier account id. */
	private String modifier;

	/** Workspace id. */
	@JsonProperty("workspace_id")
	private String workspaceId;

}


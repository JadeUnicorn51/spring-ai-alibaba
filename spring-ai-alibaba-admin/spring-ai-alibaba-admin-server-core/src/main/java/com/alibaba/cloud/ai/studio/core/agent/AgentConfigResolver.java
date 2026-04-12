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

package com.alibaba.cloud.ai.studio.core.agent;

import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.enums.agent.AgentExecutionMode;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolve and normalize agent runtime configuration.
 *
 * @since 1.0.0.3
 */
@Component
public class AgentConfigResolver {

	private static final int REACT_DEFAULT_MAX_ITERATIONS = 6;

	public AgentConfig resolve(AgentConfig sourceConfig) {
		return resolveInternal(sourceConfig, false);
	}

	public AgentConfig resolveForReact(AgentConfig sourceConfig) {
		return resolveInternal(sourceConfig, true);
	}

	public boolean isReactAgentMode(AgentConfig config) {
		AgentConfig safeConfig = resolve(config);
		return safeConfig.getExecutionMode() == AgentExecutionMode.REACT_AGENT;
	}

	private AgentConfig resolveInternal(AgentConfig sourceConfig, boolean forceReactMode) {
		AgentConfig config = sourceConfig == null ? new AgentConfig() : deepCopy(sourceConfig);
		AgentExecutionMode executionMode = config.getExecutionMode();
		if (executionMode == null) {
			executionMode = AgentExecutionMode.BASIC_TOOL_LOOP;
		}
		if (forceReactMode) {
			executionMode = AgentExecutionMode.REACT_AGENT;
		}
		config.setExecutionMode(executionMode);

		if (executionMode == AgentExecutionMode.REACT_AGENT) {
			Integer maxIterations = config.getMaxIterations();
			if (maxIterations == null || maxIterations <= 0) {
				config.setMaxIterations(REACT_DEFAULT_MAX_ITERATIONS);
			}
		}

		mergeSkills(config);
		return config;
	}

	private AgentConfig deepCopy(AgentConfig sourceConfig) {
		return JsonUtils.fromJson(JsonUtils.toJson(sourceConfig), AgentConfig.class);
	}

	private void mergeSkills(AgentConfig config) {
		List<AgentConfig.Skill> skills = config.getSkills();
		if (CollectionUtils.isEmpty(skills)) {
			return;
		}

		LinkedHashMap<String, AgentConfig.Tool> mergedTools = new LinkedHashMap<>();
		if (!CollectionUtils.isEmpty(config.getTools())) {
			for (AgentConfig.Tool tool : config.getTools()) {
				if (tool == null || StringUtils.isBlank(tool.getId())) {
					continue;
				}
				String key = tool.getId() + ":" + StringUtils.defaultString(tool.getType(), "plugin");
				mergedTools.putIfAbsent(key, tool);
			}
		}

		LinkedHashMap<String, AgentConfig.McpServer> mergedMcpServers = new LinkedHashMap<>();
		if (!CollectionUtils.isEmpty(config.getMcpServers())) {
			for (AgentConfig.McpServer server : config.getMcpServers()) {
				if (server == null || StringUtils.isBlank(server.getId())) {
					continue;
				}
				String key = server.getId() + ":" + StringUtils.defaultString(server.getType(), "mcp");
				mergedMcpServers.putIfAbsent(key, server);
			}
		}

		LinkedHashMap<String, String> mergedAgentComponents = new LinkedHashMap<>();
		if (!CollectionUtils.isEmpty(config.getAgentComponents())) {
			for (String code : config.getAgentComponents()) {
				if (StringUtils.isNotBlank(code)) {
					mergedAgentComponents.putIfAbsent(code, code);
				}
			}
		}

		LinkedHashMap<String, String> mergedWorkflowComponents = new LinkedHashMap<>();
		if (!CollectionUtils.isEmpty(config.getWorkflowComponents())) {
			for (String code : config.getWorkflowComponents()) {
				if (StringUtils.isNotBlank(code)) {
					mergedWorkflowComponents.putIfAbsent(code, code);
				}
			}
		}

		List<String> skillInstructions = new ArrayList<>();
		for (AgentConfig.Skill skill : skills) {
			if (skill == null || Boolean.FALSE.equals(skill.getEnabled())) {
				continue;
			}

			if (!CollectionUtils.isEmpty(skill.getToolIds())) {
				for (String toolId : skill.getToolIds()) {
					if (StringUtils.isBlank(toolId)) {
						continue;
					}
					AgentConfig.Tool tool = new AgentConfig.Tool();
					tool.setId(toolId);
					tool.setType("plugin");
					String key = toolId + ":plugin";
					mergedTools.putIfAbsent(key, tool);
				}
			}

			if (!CollectionUtils.isEmpty(skill.getMcpServerIds())) {
				for (String serverId : skill.getMcpServerIds()) {
					if (StringUtils.isBlank(serverId)) {
						continue;
					}
					AgentConfig.McpServer server = new AgentConfig.McpServer();
					server.setId(serverId);
					server.setType("mcp");
					String key = serverId + ":mcp";
					mergedMcpServers.putIfAbsent(key, server);
				}
			}

			if (!CollectionUtils.isEmpty(skill.getAgentComponentIds())) {
				for (String code : skill.getAgentComponentIds()) {
					if (StringUtils.isNotBlank(code)) {
						mergedAgentComponents.putIfAbsent(code, code);
					}
				}
			}

			if (!CollectionUtils.isEmpty(skill.getWorkflowComponentIds())) {
				for (String code : skill.getWorkflowComponentIds()) {
					if (StringUtils.isNotBlank(code)) {
						mergedWorkflowComponents.putIfAbsent(code, code);
					}
				}
			}

			if (StringUtils.isNotBlank(skill.getInstruction())) {
				String title = StringUtils.defaultIfBlank(skill.getName(), skill.getId());
				if (StringUtils.isBlank(title)) {
					title = "skill";
				}
				skillInstructions.add(String.format("[%s]%n%s", title, skill.getInstruction()));
			}
		}

		config.setTools(new ArrayList<>(mergedTools.values()));
		config.setMcpServers(new ArrayList<>(mergedMcpServers.values()));
		config.setAgentComponents(new ArrayList<>(mergedAgentComponents.values()));
		config.setWorkflowComponents(new ArrayList<>(mergedWorkflowComponents.values()));

		if (!skillInstructions.isEmpty()) {
			String skillInstructionSection = "Enabled skills:\n" + String.join("\n\n", skillInstructions);
			String originalInstruction = StringUtils.defaultString(config.getInstructions());
			if (StringUtils.isBlank(originalInstruction)) {
				config.setInstructions(skillInstructionSection);
			}
			else {
				config.setInstructions(originalInstruction + "\n\n" + skillInstructionSection);
			}
		}
	}

}

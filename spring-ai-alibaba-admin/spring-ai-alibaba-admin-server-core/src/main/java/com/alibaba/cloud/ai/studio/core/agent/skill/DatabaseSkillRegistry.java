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

package com.alibaba.cloud.ai.studio.core.agent.skill;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.service.SkillService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.api.OpenApiUtils;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDetail;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpTool;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ToolCallSchema;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.Skill;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Tool;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Database-backed implementation of {@link SkillRegistry}.
 *
 * @since 1.0.0.3
 */
@Component
public class DatabaseSkillRegistry implements SkillRegistry {

	private static final int PAGE_SIZE = 200;

	private static final String DEFAULT_SYSTEM_PROMPT_TEMPLATE = """
			## Skills System
			
			You have access to a dynamic skills registry.
			
			### Available Skills
			
			{skills_list}
			
			### How to Use Skills
			
			{skills_load_instructions}
			
			Always call `read_skill` first, then follow the returned instructions and use corresponding tools.
			""";

	private static final String DEFAULT_SOURCE = "project";

	private final SkillService skillService;

	private final PluginService pluginService;

	private final McpServerService mcpServerService;

	private final AppComponentManager appComponentManager;

	private final SystemPromptTemplate systemPromptTemplate = SystemPromptTemplate.builder()
		.template(DEFAULT_SYSTEM_PROMPT_TEMPLATE)
		.build();

	private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();

	public DatabaseSkillRegistry(SkillService skillService, PluginService pluginService,
			McpServerService mcpServerService, AppComponentManager appComponentManager) {
		this.skillService = skillService;
		this.pluginService = pluginService;
		this.mcpServerService = mcpServerService;
		this.appComponentManager = appComponentManager;
	}

	/**
	 * Creates a request-safe snapshot registry for the current workspace.
	 * This avoids ThreadLocal request-context lookup during interceptor/tool execution.
	 * @return immutable snapshot-backed skill registry
	 */
	public SkillRegistry snapshotForCurrentWorkspace() {
		String workspaceKey = resolveWorkspaceKey();
		Snapshot snapshot = ensureSnapshot();
		return new SnapshotSkillRegistry(workspaceKey, snapshot);
	}

	@Override
	public Optional<SkillMetadata> get(String name) {
		if (StringUtils.isBlank(name)) {
			return Optional.empty();
		}
		Snapshot snapshot = ensureSnapshot();
		return Optional.ofNullable(snapshot.byName().get(name));
	}

	@Override
	public Optional<SkillMetadata> getByPath(String path) {
		if (StringUtils.isBlank(path)) {
			return Optional.empty();
		}
		Snapshot snapshot = ensureSnapshot();
		return Optional.ofNullable(snapshot.byPath().get(path));
	}

	@Override
	public List<SkillMetadata> listAll() {
		return ensureSnapshot().list();
	}

	@Override
	public boolean contains(String name) {
		return get(name).isPresent();
	}

	@Override
	public int size() {
		return ensureSnapshot().list().size();
	}

	@Override
	public void reload() {
		String workspaceKey = resolveWorkspaceKey();
		snapshots.put(workspaceKey, loadSnapshot());
	}

	@Override
	public String readSkillContent(String name) throws IOException {
		Optional<SkillMetadata> metadataOptional = get(name);
		if (metadataOptional.isEmpty()) {
			throw new IllegalStateException("Skill not found: " + name);
		}
		String content = metadataOptional.get().getFullContent();
		if (StringUtils.isBlank(content)) {
			throw new IllegalStateException("Skill content is empty: " + name);
		}
		return content;
	}

	@Override
	public String getSkillLoadInstructions() {
		return """
				- Use `read_skill` with `skill_name` as the primary selector.
				- `skill_path` is optional compatibility alias, not a real filesystem path.
				- Skills are loaded from structured records and rendered as SKILL.md content dynamically.
				""";
	}

	@Override
	public String getRegistryType() {
		return "dynamic";
	}

	@Override
	public SystemPromptTemplate getSystemPromptTemplate() {
		return systemPromptTemplate;
	}

	private Snapshot ensureSnapshot() {
		String workspaceKey = resolveWorkspaceKey();
		return snapshots.computeIfAbsent(workspaceKey, key -> loadSnapshot());
	}

	private Snapshot loadSnapshot() {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (context == null || StringUtils.isBlank(context.getWorkspaceId())) {
			return Snapshot.empty();
		}

		List<SkillMetadata> metadataList = new ArrayList<>();
		BaseQuery query = new BaseQuery();
		query.setCurrent(1);
		query.setSize(PAGE_SIZE);
		while (true) {
			PagingList<Skill> page = skillService.listSkills(query);
			if (page == null || CollectionUtils.isEmpty(page.getRecords())) {
				break;
			}

			for (Skill skill : page.getRecords()) {
				if (skill == null || StringUtils.isBlank(skill.getSkillId()) || StringUtils.isBlank(skill.getName())) {
					continue;
				}
				if (Boolean.FALSE.equals(skill.getEnabled())) {
					continue;
				}
				metadataList.add(toMetadata(skill));
			}

			long expected = (long) query.getCurrent() * query.getSize();
			if (expected >= page.getTotal()) {
				break;
			}
			query.setCurrent(query.getCurrent() + 1);
		}
		return Snapshot.of(metadataList);
	}

	private SkillMetadata toMetadata(Skill skill) {
		String skillPath = "skill://id/" + skill.getSkillId();
		List<String> allowedToolNames = resolveAllowedToolNames(skill);
		return SkillMetadata.builder()
			.name(skill.getName())
			.description(StringUtils.defaultIfBlank(skill.getDescription(), "No description"))
			.skillPath(skillPath)
			.source(DEFAULT_SOURCE)
			.fullContent(resolveSkillMarkdown(skill, allowedToolNames))
			.allowedTools(allowedToolNames)
			.build();
	}

	private String resolveSkillMarkdown(Skill skill, List<String> allowedToolNames) {
		String instruction = StringUtils.trimToEmpty(skill.getInstruction());
		if (looksLikeSkillMarkdown(instruction)) {
			return instruction;
		}
		return buildSkillContent(skill, instruction, allowedToolNames);
	}

	private boolean looksLikeSkillMarkdown(String content) {
		if (StringUtils.isBlank(content)) {
			return false;
		}
		String normalized = content.trim();
		return normalized.startsWith("---")
				&& (normalized.contains("\nname:") || normalized.contains("\nname :"));
	}

	private String buildSkillContent(Skill skill, String instruction, List<String> allowedToolNames) {
		StringBuilder content = new StringBuilder();
		content.append("---\n")
			.append("name: ")
			.append(skill.getName())
			.append("\n")
			.append("description: ")
			.append(StringUtils.defaultIfBlank(skill.getDescription(), "No description"))
			.append("\n")
			.append("---\n\n");

		if (StringUtils.isNotBlank(instruction)) {
			content.append("## Instructions\n\n").append(instruction).append("\n\n");
		}

		appendAllowedTools(content, allowedToolNames);

		content.append("## Notes\n\n")
			.append("- Bindings are activated progressively after `read_skill`.\n")
			.append("- Use only methods listed in Allowed Tools for this skill.\n");
		return content.toString();
	}

	private void appendAllowedTools(StringBuilder content, List<String> toolNames) {
		if (CollectionUtils.isEmpty(toolNames)) {
			return;
		}
		content.append("## Allowed Tools\n\n");
		for (String name : toolNames) {
			content.append("- ").append(name).append("\n");
		}
		content.append("\n");
	}

	private List<String> resolveAllowedToolNames(Skill skill) {
		LinkedHashMap<String, String> names = new LinkedHashMap<>();
		resolvePluginToolNames(skill.getToolIds()).forEach(name -> names.putIfAbsent(name, name));
		resolveMcpToolNames(skill.getMcpServerIds()).forEach(name -> names.putIfAbsent(name, name));
		resolveComponentToolNames(skill.getAgentComponentIds()).forEach(name -> names.putIfAbsent(name, name));
		resolveComponentToolNames(skill.getWorkflowComponentIds()).forEach(name -> names.putIfAbsent(name, name));
		return new ArrayList<>(names.values());
	}

	private List<String> resolvePluginToolNames(List<String> toolIds) {
		if (CollectionUtils.isEmpty(toolIds)) {
			return List.of();
		}
		try {
			List<Tool> tools = pluginService.getTools(toolIds);
			if (CollectionUtils.isEmpty(tools)) {
				return List.of();
			}
			return tools.stream()
				.map(tool -> OpenApiUtils.buildToolCallSchema(tool))
				.map(ToolCallSchema::getName)
				.filter(StringUtils::isNotBlank)
				.distinct()
				.toList();
		}
		catch (Exception e) {
			return List.of();
		}
	}

	private List<String> resolveMcpToolNames(List<String> mcpServerIds) {
		if (CollectionUtils.isEmpty(mcpServerIds)) {
			return List.of();
		}
		try {
			List<McpServerDetail> servers = mcpServerService
				.listByCodes(McpQuery.builder().needTools(true).serverCodes(mcpServerIds).build());
			if (CollectionUtils.isEmpty(servers)) {
				return List.of();
			}
			return servers.stream()
				.filter(server -> !CollectionUtils.isEmpty(server.getTools()))
				.flatMap(server -> server.getTools().stream())
				.map(McpTool::getName)
				.filter(StringUtils::isNotBlank)
				.distinct()
				.toList();
		}
		catch (Exception e) {
			return List.of();
		}
	}

	private List<String> resolveComponentToolNames(List<String> componentIds) {
		if (CollectionUtils.isEmpty(componentIds)) {
			return List.of();
		}
		try {
			Map<String, ToolCallSchema> schemaMap = appComponentManager.getToolCallSchema(componentIds);
			if (CollectionUtils.isEmpty(schemaMap)) {
				return List.of();
			}
			return schemaMap.values()
				.stream()
				.map(ToolCallSchema::getName)
				.filter(StringUtils::isNotBlank)
				.distinct()
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			return List.of();
		}
	}

	private String resolveWorkspaceKey() {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (context == null || StringUtils.isBlank(context.getWorkspaceId())) {
			return "-";
		}
		return context.getWorkspaceId();
	}

	private record Snapshot(List<SkillMetadata> list, Map<String, SkillMetadata> byName, Map<String, SkillMetadata> byPath) {

		private static Snapshot empty() {
			return new Snapshot(List.of(), Map.of(), Map.of());
		}

		private static Snapshot of(List<SkillMetadata> metadataList) {
			if (CollectionUtils.isEmpty(metadataList)) {
				return empty();
			}

			Map<String, SkillMetadata> byName = new LinkedHashMap<>();
			Map<String, SkillMetadata> byPath = new LinkedHashMap<>();
			for (SkillMetadata metadata : metadataList) {
				byName.putIfAbsent(metadata.getName(), metadata);
				byPath.putIfAbsent(metadata.getSkillPath(), metadata);
			}
			return new Snapshot(Collections.unmodifiableList(new ArrayList<>(metadataList)),
					Collections.unmodifiableMap(byName), Collections.unmodifiableMap(byPath));
		}
	}

	private class SnapshotSkillRegistry implements SkillRegistry {

		private final String workspaceKey;

		private final Snapshot snapshot;

		private SnapshotSkillRegistry(String workspaceKey, Snapshot snapshot) {
			this.workspaceKey = workspaceKey;
			this.snapshot = snapshot == null ? Snapshot.empty() : snapshot;
		}

		@Override
		public Optional<SkillMetadata> get(String name) {
			if (StringUtils.isBlank(name)) {
				return Optional.empty();
			}
			return Optional.ofNullable(snapshot.byName().get(name));
		}

		@Override
		public Optional<SkillMetadata> getByPath(String path) {
			if (StringUtils.isBlank(path)) {
				return Optional.empty();
			}
			return Optional.ofNullable(snapshot.byPath().get(path));
		}

		@Override
		public List<SkillMetadata> listAll() {
			return snapshot.list();
		}

		@Override
		public boolean contains(String name) {
			return get(name).isPresent();
		}

		@Override
		public int size() {
			return snapshot.list().size();
		}

		@Override
		public void reload() {
			RequestContext context = RequestContextHolder.getRequestContext();
			if (context == null || !StringUtils.equals(context.getWorkspaceId(), workspaceKey)) {
				return;
			}
			snapshots.put(workspaceKey, loadSnapshot());
		}

		@Override
		public String readSkillContent(String name) throws IOException {
			Optional<SkillMetadata> metadataOptional = get(name);
			if (metadataOptional.isEmpty()) {
				throw new IllegalStateException("Skill not found: " + name);
			}
			String content = metadataOptional.get().getFullContent();
			if (StringUtils.isBlank(content)) {
				throw new IllegalStateException("Skill content is empty: " + name);
			}
			return content;
		}

		@Override
		public String getSkillLoadInstructions() {
			return DatabaseSkillRegistry.this.getSkillLoadInstructions();
		}

		@Override
		public String getRegistryType() {
			return DatabaseSkillRegistry.this.getRegistryType();
		}

		@Override
		public SystemPromptTemplate getSystemPromptTemplate() {
			return DatabaseSkillRegistry.this.getSystemPromptTemplate();
		}

	}

}

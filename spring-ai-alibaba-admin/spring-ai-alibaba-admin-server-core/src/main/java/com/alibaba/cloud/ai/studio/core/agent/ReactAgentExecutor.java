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

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.studio.core.agent.skill.DatabaseSkillRegistry;
import com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager;
import com.alibaba.cloud.ai.studio.core.base.manager.DocumentRetrieverManager;
import com.alibaba.cloud.ai.studio.core.base.manager.FileManager;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.service.ToolExecutionService;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.model.llm.ModelFactory;
import com.alibaba.cloud.ai.studio.core.agent.tool.CompositeToolCallbackProvider;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ToolCall;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.Usage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * ReactAgent executor implementation based on spring-ai-alibaba-agent-framework.
 *
 * @since 1.0.0.3
 */
@Service
@Qualifier("reactAgentExecutor")
public class ReactAgentExecutor extends BasicAgentExecutor {

	private final DatabaseSkillRegistry databaseSkillRegistry;

	public ReactAgentExecutor(ToolExecutionService toolExecutionService, PluginService pluginService,
			McpServerService mcpServerService, AppComponentManager appComponentManager,
			DocumentRetrieverManager documentRetrieverManager, ChatMemory chatMemory, CommonConfig commonConfig,
			ModelFactory modelFactory, FileManager fileManager, DatabaseSkillRegistry databaseSkillRegistry) {
		super(toolExecutionService, pluginService, mcpServerService, appComponentManager, documentRetrieverManager,
				chatMemory, commonConfig, modelFactory, fileManager);
		this.databaseSkillRegistry = databaseSkillRegistry;
	}

	@Override
	public Flux<AgentResponse> streamExecute(AgentContext context, AgentRequest request) {
		AgentConfig config = context.getConfig();
		ToolCallingChatOptions chatOptions = buildChatOptions(config);
		CompositeToolCallbackProvider toolCallbackProvider = buildToolCallbackProvider(config, request.getExtraPrams());
		List<Message> messages = buildMessages(context);
		ChatClient.Builder chatClientBuilder = buildChatClient(context, chatOptions, toolCallbackProvider);

		ReactAgent reactAgent = buildReactAgent(context, chatOptions, toolCallbackProvider, chatClientBuilder);
		RunnableConfig runnableConfig = buildRunnableConfig(context);
		try {
			return reactAgent.stream(messages, runnableConfig)
				.concatMap(output -> convertStreamOutput(output, toolCallbackProvider, config));
		}
		catch (GraphRunnerException e) {
			return Flux.error(e);
		}
	}

	@Override
	public AgentResponse execute(AgentContext context, AgentRequest request) {
		AgentConfig config = context.getConfig();
		ToolCallingChatOptions chatOptions = buildChatOptions(config);
		CompositeToolCallbackProvider toolCallbackProvider = buildToolCallbackProvider(config, request.getExtraPrams());
		List<Message> messages = buildMessages(context);
		ChatClient.Builder chatClientBuilder = buildChatClient(context, chatOptions, toolCallbackProvider);

		ReactAgent reactAgent = buildReactAgent(context, chatOptions, toolCallbackProvider, chatClientBuilder);
		RunnableConfig runnableConfig = buildRunnableConfig(context);
		Optional<NodeOutput> outputOptional;
		try {
			outputOptional = reactAgent.invokeAndGetOutput(messages, runnableConfig);
		}
		catch (GraphRunnerException e) {
			throw new RuntimeException("react agent execute failed", e);
		}
		Assert.state(outputOptional.isPresent(), "react agent output can not be null");

		NodeOutput output = outputOptional.get();
		AgentResponse response = convertFinalOutput(output, config, toolCallbackProvider);
		Assert.notNull(response, "agent response can not be null");
		return response;
	}

	private ReactAgent buildReactAgent(AgentContext context, ToolCallingChatOptions chatOptions,
			CompositeToolCallbackProvider toolCallbackProvider, ChatClient.Builder chatClientBuilder) {
		ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
		Builder builder = ReactAgent.builder()
			.name(buildAgentName(context))
			.chatClient(chatClientBuilder.build())
			.compileConfig(buildCompileConfigWithMemorySaver())
			.chatOptions(chatOptions);

		if (callbacks != null && callbacks.length > 0) {
			builder.tools(Arrays.asList(callbacks));
		}

		List<Hook> hooks = new ArrayList<>();
		SkillsAgentHook skillsAgentHook = buildSkillsAgentHook(context.getConfig(), toolCallbackProvider);
		if (skillsAgentHook != null) {
			hooks.add(skillsAgentHook);
		}

		int maxIterations = getMaxToolIterations(context.getConfig());
		if (maxIterations < Integer.MAX_VALUE) {
			hooks.add(ModelCallLimitHook.builder()
				.runLimit(maxIterations + 1)
				.exitBehavior(ModelCallLimitHook.ExitBehavior.END)
				.build());
		}

		if (!hooks.isEmpty()) {
			builder.hooks(hooks);
		}

		return builder.build();
	}

	private SkillsAgentHook buildSkillsAgentHook(AgentConfig config,
			CompositeToolCallbackProvider defaultToolCallbackProvider) {
		List<AgentConfig.Skill> skills = config.getSkills();
		if (CollectionUtils.isEmpty(skills)) {
			return null;
		}

		Map<String, List<ToolCallback>> groupedTools = buildGroupedSkillTools(skills, defaultToolCallbackProvider);
		SkillRegistry runtimeSkillRegistry = databaseSkillRegistry.snapshotForCurrentWorkspace();
		SkillsAgentHook.Builder hookBuilder = SkillsAgentHook.builder()
			.skillRegistry(runtimeSkillRegistry)
			.autoReload(false);
		if (!groupedTools.isEmpty()) {
			hookBuilder.groupedTools(groupedTools);
		}
		return hookBuilder.build();
	}

	private Map<String, List<ToolCallback>> buildGroupedSkillTools(List<AgentConfig.Skill> skills,
			CompositeToolCallbackProvider defaultToolCallbackProvider) {
		Map<String, List<ToolCallback>> groupedTools = new java.util.LinkedHashMap<>();
		for (AgentConfig.Skill skill : skills) {
			if (skill == null || Boolean.FALSE.equals(skill.getEnabled())) {
				continue;
			}

			List<ToolCallback> callbacks = buildSkillToolCallbacks(skill, defaultToolCallbackProvider.getExtraParams());
			String skillName = StringUtils.trimToNull(skill.getName());
			if (StringUtils.isNotBlank(skillName) && !callbacks.isEmpty()) {
				groupedTools.put(skill.getName(), callbacks);
			}
			String skillId = StringUtils.trimToNull(skill.getId());
			if (StringUtils.isNotBlank(skillId) && !callbacks.isEmpty()) {
				groupedTools.putIfAbsent(skillId, callbacks);
			}
		}
		return groupedTools;
	}

	private List<ToolCallback> buildSkillToolCallbacks(AgentConfig.Skill skill, Map<String, Object> extraParams) {
		AgentConfig skillConfig = new AgentConfig();
		skillConfig.setTools(toPluginTools(skill.getToolIds()));
		skillConfig.setMcpServers(toMcpServers(skill.getMcpServerIds()));
		skillConfig.setAgentComponents(normalizeIds(skill.getAgentComponentIds()));
		skillConfig.setWorkflowComponents(normalizeIds(skill.getWorkflowComponentIds()));

		CompositeToolCallbackProvider provider = buildToolCallbackProvider(skillConfig, extraParams);
		ToolCallback[] callbacks = provider.getToolCallbacks();
		if (callbacks == null || callbacks.length == 0) {
			return List.of();
		}
		return Arrays.asList(callbacks);
	}

	private List<AgentConfig.Tool> toPluginTools(List<String> toolIds) {
		List<String> normalizedToolIds = normalizeIds(toolIds);
		if (normalizedToolIds.isEmpty()) {
			return List.of();
		}

		List<AgentConfig.Tool> tools = new ArrayList<>();
		for (String toolId : normalizedToolIds) {
			AgentConfig.Tool tool = new AgentConfig.Tool();
			tool.setId(toolId);
			tool.setType("plugin");
			tools.add(tool);
		}
		return tools;
	}

	private List<AgentConfig.McpServer> toMcpServers(List<String> serverIds) {
		List<String> normalizedServerIds = normalizeIds(serverIds);
		if (normalizedServerIds.isEmpty()) {
			return List.of();
		}

		List<AgentConfig.McpServer> servers = new ArrayList<>();
		for (String serverId : normalizedServerIds) {
			AgentConfig.McpServer server = new AgentConfig.McpServer();
			server.setId(serverId);
			server.setType("mcp");
			servers.add(server);
		}
		return servers;
	}

	private List<String> normalizeIds(List<String> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return List.of();
		}
		return ids.stream().filter(StringUtils::isNotBlank).distinct().toList();
	}

	private CompileConfig buildCompileConfigWithMemorySaver() {
		BaseCheckpointSaver memorySaver = createMemorySaver();
		SaverConfig saverConfig = buildSaverConfig(memorySaver);
		return CompileConfig.builder().saverConfig(saverConfig).releaseThread(false).build();
	}

	private BaseCheckpointSaver createMemorySaver() {
		try {
			// graph-core 1.1.x path: MemorySaver.builder().build()
			Method builderMethod = findMethod(MemorySaver.class, "builder");
			if (builderMethod != null) {
				Object builder = builderMethod.invoke(null);
				Method buildMethod = findMethod(builder.getClass(), "build");
				if (buildMethod != null) {
					Object saver = buildMethod.invoke(builder);
					if (saver instanceof BaseCheckpointSaver checkpointSaver) {
						return checkpointSaver;
					}
				}
			}

			// graph-core 1.0.x path: new MemorySaver()
			Constructor<MemorySaver> constructor = MemorySaver.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException("failed to create memory saver", e);
		}
	}

	private SaverConfig buildSaverConfig(BaseCheckpointSaver saver) {
		try {
			Object builder = SaverConfig.builder();
			Class<?> builderClass = builder.getClass();

			// Compatible with graph-core 1.1.x: register(BaseCheckpointSaver)
			Method registerWithoutType = findMethod(builderClass, "register", BaseCheckpointSaver.class);
			if (registerWithoutType != null) {
				registerWithoutType.invoke(builder, saver);
				return (SaverConfig) builderClass.getMethod("build").invoke(builder);
			}

			// Compatible with graph-core 1.0.x: register(String, BaseCheckpointSaver)
			Method registerWithType = findMethod(builderClass, "register", String.class, BaseCheckpointSaver.class);
			if (registerWithType != null) {
				registerWithType.invoke(builder, "memory", saver);
				return (SaverConfig) builderClass.getMethod("build").invoke(builder);
			}

			throw new IllegalStateException("SaverConfig.Builder has no compatible register method");
		}
		catch (Exception e) {
			throw new RuntimeException("failed to build SaverConfig with memory saver", e);
		}
	}

	private Method findMethod(Class<?> targetClass, String name, Class<?>... parameterTypes) {
		try {
			return targetClass.getMethod(name, parameterTypes);
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	private RunnableConfig buildRunnableConfig(AgentContext context) {
		return RunnableConfig.builder().threadId(buildThreadId(context)).build();
	}

	private String buildThreadId(AgentContext context) {
		return String.format("%s_%s", StringUtils.defaultIfBlank(context.getAppId(), "app"),
				StringUtils.defaultIfBlank(context.getConversationId(), "conversation"));
	}

	private String buildAgentName(AgentContext context) {
		return String.format("admin_react_%s", StringUtils.defaultIfBlank(context.getAppId(), "app"));
	}

	private Mono<AgentResponse> convertStreamOutput(NodeOutput output, CompositeToolCallbackProvider toolCallbackProvider,
			AgentConfig config) {
		if (!(output instanceof StreamingOutput<?> streamingOutput)) {
			return Mono.empty();
		}

		OutputType outputType = streamingOutput.getOutputType();
		Message message = streamingOutput.message();
		if (message == null || outputType == null) {
			return Mono.empty();
		}

		if (outputType == OutputType.AGENT_MODEL_STREAMING && message instanceof AssistantMessage assistantMessage) {
			String reasoningContent = extractReasoningContent(assistantMessage);
			if (!assistantMessage.hasToolCalls() && (StringUtils.isNotBlank(assistantMessage.getText())
					|| StringUtils.isNotBlank(reasoningContent))) {
				return Mono.just(createAssistantResponse(config, assistantMessage, toolCallbackProvider,
						AgentStatus.IN_PROGRESS, output.tokenUsage()));
			}
			return Mono.empty();
		}

		if (outputType == OutputType.AGENT_MODEL_FINISHED && message instanceof AssistantMessage assistantMessage) {
			AgentStatus status = assistantMessage.hasToolCalls() ? AgentStatus.IN_PROGRESS
					: resolveFinalStatus(assistantMessage);
			return Mono.just(createAssistantResponse(config, assistantMessage, toolCallbackProvider, status,
					output.tokenUsage()));
		}

		if (outputType == OutputType.AGENT_TOOL_FINISHED && message instanceof ToolResponseMessage toolResponseMessage) {
			return Mono.just(createToolResultResponse(config, toolResponseMessage, toolCallbackProvider));
		}

		return Mono.empty();
	}

	private AgentResponse convertFinalOutput(NodeOutput output, AgentConfig config,
			CompositeToolCallbackProvider toolCallbackProvider) {
		OverAllState state = output.state();
		Object value = state == null ? null : state.value("messages").orElse(null);
		if (!(value instanceof List<?> rawMessages) || rawMessages.isEmpty()) {
			return AgentResponse.builder()
				.model(config.getModel())
				.status(AgentStatus.FAILED)
				.message(ChatMessage.builder().role(MessageRole.ASSISTANT).content("").build())
				.build();
		}

		List<Message> messages = rawMessages.stream()
			.filter(Message.class::isInstance)
			.map(Message.class::cast)
			.toList();
		AssistantMessage finalAssistant = findLastAssistantMessage(messages);

		ChatMessage.ChatMessageBuilder messageBuilder = ChatMessage.builder()
			.role(MessageRole.ASSISTANT)
			.content(finalAssistant == null ? "" : finalAssistant.getText())
			.reasoningContent(extractReasoningContent(finalAssistant));

		List<ToolCall> mergedToolCalls = collectToolCalls(messages, toolCallbackProvider);
		if (!CollectionUtils.isEmpty(mergedToolCalls)) {
			messageBuilder.toolCalls(mergedToolCalls);
		}

		AgentResponse.AgentResponseBuilder responseBuilder = AgentResponse.builder()
			.model(config.getModel())
			.message(messageBuilder.build())
			.status(finalAssistant == null ? AgentStatus.FAILED : resolveFinalStatus(finalAssistant));

		appendUsage(responseBuilder, output.tokenUsage());
		return responseBuilder.build();
	}

	private AgentResponse createAssistantResponse(AgentConfig config, AssistantMessage assistantMessage,
			CompositeToolCallbackProvider toolCallbackProvider, AgentStatus status,
			org.springframework.ai.chat.metadata.Usage usage) {
		ChatMessage.ChatMessageBuilder messageBuilder = ChatMessage.builder()
			.role(MessageRole.ASSISTANT)
			.content(StringUtils.defaultString(assistantMessage.getText()))
			.reasoningContent(extractReasoningContent(assistantMessage));

		if (assistantMessage.hasToolCalls()) {
			List<ToolCall> toolCalls = convertToolCall(assistantMessage.getToolCalls(), toolCallbackProvider);
			if (!CollectionUtils.isEmpty(toolCalls)) {
				messageBuilder.toolCalls(toolCalls);
			}
		}

		AgentResponse.AgentResponseBuilder responseBuilder = AgentResponse.builder()
			.model(config.getModel())
			.status(status)
			.message(messageBuilder.build());
		appendUsage(responseBuilder, usage);
		return responseBuilder.build();
	}

	private AgentResponse createToolResultResponse(AgentConfig config, ToolResponseMessage toolResponseMessage,
			CompositeToolCallbackProvider toolCallbackProvider) {
		List<ToolCall> toolCalls = convertToolResult(toolResponseMessage, toolCallbackProvider);
		return AgentResponse.builder()
			.model(config.getModel())
			.status(AgentStatus.IN_PROGRESS)
			.message(ChatMessage.builder().role(MessageRole.ASSISTANT).content("").toolCalls(toolCalls).build())
			.build();
	}

	private List<ToolCall> collectToolCalls(List<Message> messages, CompositeToolCallbackProvider toolCallbackProvider) {
		List<ToolCall> toolCalls = new ArrayList<>();
		for (Message message : messages) {
			if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
				toolCalls.addAll(convertToolCall(assistantMessage.getToolCalls(), toolCallbackProvider));
			}
			else if (message instanceof ToolResponseMessage toolResponseMessage) {
				toolCalls.addAll(convertToolResult(toolResponseMessage, toolCallbackProvider));
			}
		}
		return toolCalls;
	}

	private AssistantMessage findLastAssistantMessage(List<Message> messages) {
		for (int i = messages.size() - 1; i >= 0; i--) {
			Message message = messages.get(i);
			if (message instanceof AssistantMessage assistantMessage) {
				return assistantMessage;
			}
		}
		return null;
	}

	private String extractReasoningContent(AssistantMessage assistantMessage) {
		if (assistantMessage == null) {
			return null;
		}

		// Provider-specific assistant message subclasses (for example DeepSeek/ZhiPu)
		// expose reasoning through getReasoningContent() instead of metadata.
		String fromGetter = invokeReasoningGetter(assistantMessage);
		if (StringUtils.isNotBlank(fromGetter)) {
			return fromGetter;
		}

		Map<String, Object> metadata = assistantMessage.getMetadata();
		if (metadata == null || metadata.isEmpty()) {
			return null;
		}

		String fromCamelKey = normalizeReasoningContent(metadata.get("reasoningContent"));
		if (StringUtils.isNotBlank(fromCamelKey)) {
			return fromCamelKey;
		}

		String fromSnakeKey = normalizeReasoningContent(metadata.get("reasoning_content"));
		if (StringUtils.isNotBlank(fromSnakeKey)) {
			return fromSnakeKey;
		}

		return null;
	}

	private String invokeReasoningGetter(AssistantMessage assistantMessage) {
		try {
			Method method = assistantMessage.getClass().getMethod("getReasoningContent");
			Object value = method.invoke(assistantMessage);
			return normalizeReasoningContent(value);
		}
		catch (NoSuchMethodException e) {
			return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	private String normalizeReasoningContent(Object rawValue) {
		if (rawValue == null) {
			return null;
		}

		if (rawValue instanceof String stringValue) {
			return StringUtils.isBlank(stringValue) ? null : stringValue;
		}

		if (rawValue instanceof Collection<?> values) {
			StringBuilder builder = new StringBuilder();
			for (Object value : values) {
				String part = normalizeReasoningContent(value);
				if (StringUtils.isNotBlank(part)) {
					builder.append(part);
				}
			}
			String merged = builder.toString();
			return StringUtils.isBlank(merged) ? null : merged;
		}

		if (rawValue instanceof Map<?, ?> valueMap) {
			String text = normalizeReasoningContent(valueMap.get("text"));
			if (StringUtils.isNotBlank(text)) {
				return text;
			}
			String content = normalizeReasoningContent(valueMap.get("content"));
			if (StringUtils.isNotBlank(content)) {
				return content;
			}
			String camel = normalizeReasoningContent(valueMap.get("reasoningContent"));
			if (StringUtils.isNotBlank(camel)) {
				return camel;
			}
			String snake = normalizeReasoningContent(valueMap.get("reasoning_content"));
			if (StringUtils.isNotBlank(snake)) {
				return snake;
			}
			return null;
		}

		String textValue = String.valueOf(rawValue);
		return StringUtils.isBlank(textValue) ? null : textValue;
	}

	private AgentStatus resolveFinalStatus(AssistantMessage assistantMessage) {
		if (assistantMessage == null) {
			return AgentStatus.FAILED;
		}

		String text = StringUtils.defaultString(assistantMessage.getText());
		if (text.startsWith("Model call limits exceeded")) {
			return AgentStatus.INCOMPLETE;
		}
		return AgentStatus.COMPLETED;
	}

	private void appendUsage(AgentResponse.AgentResponseBuilder responseBuilder,
			org.springframework.ai.chat.metadata.Usage usage) {
		org.springframework.ai.chat.metadata.Usage safeUsage = usage == null ? new EmptyUsage() : usage;
		int promptTokens = safeUsage.getPromptTokens() == null ? 0 : safeUsage.getPromptTokens().intValue();
		int completionTokens = safeUsage.getCompletionTokens() == null ? 0 : safeUsage.getCompletionTokens().intValue();
		int totalTokens = safeUsage.getTotalTokens() == null ? 0 : safeUsage.getTotalTokens().intValue();

		if (promptTokens > 0 || completionTokens > 0 || totalTokens > 0) {
			responseBuilder.usage(Usage.builder()
				.promptTokens(promptTokens)
				.completionTokens(completionTokens)
				.totalTokens(totalTokens)
				.build());
		}
	}

}

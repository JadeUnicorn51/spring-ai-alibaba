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

package com.alibaba.cloud.ai.studio.core.agent.memory;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@HookPositions({ HookPosition.BEFORE_MODEL })
public class LongTermMemoryHook extends MessagesModelHook {

	private static final String MEMORY_HEADER = "## Long-term Memory";

	private final Store fallbackStore;

	public LongTermMemoryHook(Store fallbackStore) {
		this.fallbackStore = fallbackStore;
	}

	@Override
	public String getName() {
		return "long_term_memory";
	}

	@Override
	public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
		if (previousMessages == null || previousMessages.isEmpty()) {
			return new AgentCommand(previousMessages);
		}

		String userId = config.metadata("user_id").map(String.class::cast).orElse(null);
		if (StringUtils.isBlank(userId)) {
			return new AgentCommand(previousMessages);
		}

		Store store = config.store() == null ? fallbackStore : config.store();
		if (store == null) {
			return new AgentCommand(previousMessages);
		}

		List<String> namespace = buildNamespace(config);
		Optional<StoreItem> itemOpt = store.getItem(namespace, userId);
		if (itemOpt.isEmpty()) {
			return new AgentCommand(previousMessages);
		}

		Map<String, Object> profileData = itemOpt.get().getValue();
		if (profileData == null || profileData.isEmpty()) {
			return new AgentCommand(previousMessages);
		}

		String memoryText = buildMemoryText(profileData, userId);
		if (containsMemory(previousMessages)) {
			return new AgentCommand(previousMessages);
		}

		List<Message> updated = new ArrayList<>(previousMessages);
		boolean merged = false;
		for (int i = 0; i < updated.size(); i++) {
			Message message = updated.get(i);
			if (message instanceof SystemMessage systemMessage) {
				String text = StringUtils.defaultString(systemMessage.getText());
				updated.set(i, new SystemMessage(text + "\n\n" + memoryText));
				merged = true;
				break;
			}
		}
		if (!merged) {
			updated.add(0, new SystemMessage(memoryText));
		}

		return new AgentCommand(updated);
	}

	private List<String> buildNamespace(RunnableConfig config) {
		String workspaceId = config.metadata("workspace_id").map(String.class::cast).orElse(null);
		String tenantId = config.metadata("tenant_id").map(String.class::cast).orElse(null);
		if (StringUtils.isBlank(workspaceId)) {
			RequestContext context = RequestContextHolder.getRequestContext();
			workspaceId = context == null ? null : context.getWorkspaceId();
			tenantId = StringUtils.defaultIfBlank(tenantId, context == null ? null : context.getTenantId());
		}

		if (StringUtils.isBlank(workspaceId) && StringUtils.isBlank(tenantId)) {
			return List.of("user_profiles");
		}
		if (StringUtils.isBlank(tenantId)) {
			return List.of("workspace", workspaceId, "user_profiles");
		}
		if (StringUtils.isBlank(workspaceId)) {
			return List.of("tenant", tenantId, "user_profiles");
		}
		return List.of("tenant", tenantId, "workspace", workspaceId, "user_profiles");
	}

	private String buildMemoryText(Map<String, Object> profileData, String userId) {
		String json = JsonUtils.toJson(profileData);
		return MEMORY_HEADER + "\nuser_id=" + userId + "\n" + json;
	}

	private boolean containsMemory(List<Message> messages) {
		for (Message message : messages) {
			if (message instanceof SystemMessage systemMessage) {
				String text = StringUtils.defaultString(systemMessage.getText());
				if (StringUtils.contains(text, MEMORY_HEADER)) {
					return true;
				}
			}
		}
		return false;
	}

}

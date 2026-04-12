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

package com.alibaba.cloud.ai.studio.core.agent.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Normalize system prompts for ReactAgent model calls.
 *
 * Merge all SystemMessage entries from request messages into request.systemMessage,
 * and remove SystemMessage entries from messages list. This avoids multi-system
 * ambiguity when downstream interceptors (such as SkillsInterceptor) append prompts.
 */
public class SystemMessageMergeInterceptor extends ModelInterceptor {

	private static final String NAME = "SystemMessageMergeInterceptor";

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		if (request == null) {
			return handler.call(null);
		}

		List<String> mergedSections = new ArrayList<>();
		appendIfPresent(mergedSections, request.getSystemMessage());

		List<Message> originalMessages = request.getMessages();
		if (CollectionUtils.isEmpty(originalMessages)) {
			if (mergedSections.isEmpty()) {
				return handler.call(request);
			}
			ModelRequest normalized = ModelRequest.builder(request)
				.systemMessage(new SystemMessage(joinSections(mergedSections)))
				.build();
			return handler.call(normalized);
		}

		List<Message> filteredMessages = new ArrayList<>(originalMessages.size());
		boolean removedSystemMessage = false;
		for (Message message : originalMessages) {
			if (message instanceof SystemMessage systemMessage) {
				appendIfPresent(mergedSections, systemMessage);
				removedSystemMessage = true;
				continue;
			}
			filteredMessages.add(message);
		}

		if (!removedSystemMessage && mergedSections.isEmpty()) {
			return handler.call(request);
		}

		ModelRequest.Builder builder = ModelRequest.builder(request).messages(filteredMessages);
		if (mergedSections.isEmpty()) {
			builder.systemMessage(null);
		}
		else {
			builder.systemMessage(new SystemMessage(joinSections(mergedSections)));
		}
		return handler.call(builder.build());
	}

	@Override
	public String getName() {
		return NAME;
	}

	private void appendIfPresent(List<String> sections, SystemMessage systemMessage) {
		if (systemMessage == null) {
			return;
		}
		String text = StringUtils.trimToNull(systemMessage.getText());
		if (text != null) {
			sections.add(text);
		}
	}

	private String joinSections(List<String> sections) {
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		for (String section : sections) {
			if (StringUtils.isNotBlank(section)) {
				unique.add(section);
			}
		}
		return String.join("\n\n", unique);
	}

}


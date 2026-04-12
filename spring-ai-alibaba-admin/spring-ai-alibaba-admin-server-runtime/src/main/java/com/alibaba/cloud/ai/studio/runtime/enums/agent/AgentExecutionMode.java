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

package com.alibaba.cloud.ai.studio.runtime.enums.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Agent execution mode.
 *
 * @since 1.0.0.3
 */
public enum AgentExecutionMode {

	/** Legacy chat-client tool loop mode. */
	BASIC_TOOL_LOOP("basic_tool_loop"),

	/** ReactAgent + skills mode. */
	REACT_AGENT("react_agent");

	private final String value;

	AgentExecutionMode(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static AgentExecutionMode fromValue(String value) {
		if (value == null) {
			return BASIC_TOOL_LOOP;
		}

		for (AgentExecutionMode mode : values()) {
			if (mode.value.equalsIgnoreCase(value)) {
				return mode;
			}
		}

		return BASIC_TOOL_LOOP;
	}

}

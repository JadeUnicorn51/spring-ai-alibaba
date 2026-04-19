/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.studio.admin.desktoplocal.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Resolved desktop-local model defaults with source metadata.
 * @param chat resolved chat default
 * @param embedding resolved embedding default
 */
public record EffectiveModelDefaultsDTO(ResolvedModel chat, ResolvedModel embedding) {

	public enum Source {

		KB, WORKSPACE, LOCAL_PROFILE, FALLBACK_ENABLED_MODEL, UNRESOLVED

	}

	public record ResolvedModel(String provider, @JsonProperty("model_id") String modelId,
			Map<String, Object> parameters, Source source, String message) {
	}

}

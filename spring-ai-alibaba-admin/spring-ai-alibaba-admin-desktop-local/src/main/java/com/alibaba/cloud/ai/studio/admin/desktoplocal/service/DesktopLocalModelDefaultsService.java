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

package com.alibaba.cloud.ai.studio.admin.desktoplocal.service;

import com.alibaba.cloud.ai.studio.admin.desktoplocal.DesktopLocalConstants;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.ModelDefaultsDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalSettingRepository;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Service for desktop-local model default settings.
 */
@Service
public class DesktopLocalModelDefaultsService {

	private static final Set<String> ALLOWED_CHAT_PARAMETER_KEYS = Set.of("temperature", "max_tokens", "top_p");

	private final DesktopLocalSettingRepository settingRepository;

	public DesktopLocalModelDefaultsService(DesktopLocalSettingRepository settingRepository) {
		this.settingRepository = settingRepository;
	}

	public ModelDefaultsDTO getModelDefaults() {
		return settingRepository
			.findValue(DesktopLocalConstants.LOCAL_PROFILE_ID, DesktopLocalConstants.SETTING_KEY_MODEL_DEFAULTS)
			.map(value -> JsonUtils.fromJson(value, ModelDefaultsDTO.class))
			.orElseGet(() -> new ModelDefaultsDTO(null, null));
	}

	public ModelDefaultsDTO saveModelDefaults(ModelDefaultsDTO modelDefaults, String operator) {
		if (modelDefaults == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("model_defaults"));
		}
		validateModelDefaults(modelDefaults);

		String settingValue = JsonUtils.toJson(modelDefaults);
		settingRepository.upsertValue(DesktopLocalConstants.LOCAL_PROFILE_ID,
				DesktopLocalConstants.SETTING_KEY_MODEL_DEFAULTS, settingValue, operator);
		return modelDefaults;
	}

	public void validateModelDefaults(ModelDefaultsDTO modelDefaults) {
		ModelDefaultsDTO.ChatDefaults chat = modelDefaults.chat();
		if (chat != null) {
			validateModelRef("chat", chat.provider(), chat.modelId());
			validateChatParameters(chat.parameters());
		}

		ModelDefaultsDTO.EmbeddingDefaults embedding = modelDefaults.embedding();
		if (embedding != null) {
			validateModelRef("embedding", embedding.provider(), embedding.modelId());
		}
	}

	private void validateModelRef(String section, String provider, String modelId) {
		if (isBlank(provider) != isBlank(modelId)) {
			throw new BizException(ErrorCode.INVALID_PARAMS
				.toError(section, "provider and model_id must be both set or both empty"));
		}
	}

	private void validateChatParameters(Map<String, Object> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return;
		}
		for (String key : parameters.keySet()) {
			if (!ALLOWED_CHAT_PARAMETER_KEYS.contains(key)) {
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("parameters", "unsupported key: " + key));
			}
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}

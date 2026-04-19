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
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalWorkspaceRepository;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for workspace-scoped desktop-local model defaults.
 */
@Service
public class DesktopLocalWorkspaceModelDefaultsService {

	private static final String MODEL_DEFAULTS_KEY = "model_defaults";

	private final DesktopLocalWorkspaceRepository workspaceRepository;

	private final DesktopLocalModelDefaultsService modelDefaultsService;

	public DesktopLocalWorkspaceModelDefaultsService(DesktopLocalWorkspaceRepository workspaceRepository,
			DesktopLocalModelDefaultsService modelDefaultsService) {
		this.workspaceRepository = workspaceRepository;
		this.modelDefaultsService = modelDefaultsService;
	}

	public ModelDefaultsDTO getWorkspaceModelDefaults(String workspaceId) {
		Map<String, Object> config = loadWorkspaceConfig(workspaceId);
		Object value = config.get(MODEL_DEFAULTS_KEY);
		if (value == null) {
			return new ModelDefaultsDTO(null, null);
		}
		if (value instanceof Map<?, ?> map) {
			return JsonUtils.fromMap(toStringObjectMap(map), ModelDefaultsDTO.class);
		}
		throw new BizException(ErrorCode.INVALID_JSON.toError());
	}

	public ModelDefaultsDTO saveWorkspaceModelDefaults(String workspaceId, ModelDefaultsDTO modelDefaults) {
		if (modelDefaults == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("model_defaults"));
		}
		modelDefaultsService.validateModelDefaults(modelDefaults);

		Map<String, Object> config = loadWorkspaceConfig(workspaceId);
		config.put(MODEL_DEFAULTS_KEY, JsonUtils.fromObjectToMap(modelDefaults));
		int updated = workspaceRepository.updateWorkspaceConfig(DesktopLocalConstants.LOCAL_PROFILE_ID, workspaceId,
				JsonUtils.toJson(config));
		if (updated <= 0) {
			throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND.toError());
		}
		return modelDefaults;
	}

	private Map<String, Object> loadWorkspaceConfig(String workspaceId) {
		if (workspaceId == null || workspaceId.isBlank()) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspace_id"));
		}
		if (!workspaceRepository.existsActiveWorkspace(DesktopLocalConstants.LOCAL_PROFILE_ID, workspaceId)) {
			throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND.toError());
		}

		return workspaceRepository.findWorkspaceConfig(DesktopLocalConstants.LOCAL_PROFILE_ID, workspaceId)
			.filter(value -> !value.isBlank())
			.map(JsonUtils::<String, Object>fromJsonToMap)
			.map(LinkedHashMap::new)
			.orElseGet(LinkedHashMap::new);
	}

	private Map<String, Object> toStringObjectMap(Map<?, ?> rawMap) {
		Map<String, Object> result = new LinkedHashMap<>();
		rawMap.forEach((key, value) -> result.put(String.valueOf(key), value));
		return result;
	}

}

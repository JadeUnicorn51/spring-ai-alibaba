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
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalModelRef;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.EffectiveModelDefaultsDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.ModelDefaultsDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalKnowledgeBaseRepository;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalModelLookupRepository;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Resolves effective desktop-local model defaults.
 */
@Service
public class DesktopLocalModelDefaultsResolver {

	private static final String MODEL_TYPE_LLM = "llm";

	private static final String MODEL_TYPE_EMBEDDING = "text_embedding";

	private final DesktopLocalWorkspaceModelDefaultsService workspaceDefaultsService;

	private final DesktopLocalModelDefaultsService profileDefaultsService;

	private final DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository;

	private final DesktopLocalModelLookupRepository modelLookupRepository;

	public DesktopLocalModelDefaultsResolver(DesktopLocalWorkspaceModelDefaultsService workspaceDefaultsService,
			DesktopLocalModelDefaultsService profileDefaultsService,
			DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository,
			DesktopLocalModelLookupRepository modelLookupRepository) {
		this.workspaceDefaultsService = workspaceDefaultsService;
		this.profileDefaultsService = profileDefaultsService;
		this.knowledgeBaseRepository = knowledgeBaseRepository;
		this.modelLookupRepository = modelLookupRepository;
	}

	public EffectiveModelDefaultsDTO resolve(String workspaceId, String kbId) {
		if (workspaceId == null || workspaceId.isBlank()) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspace_id"));
		}

		ModelDefaultsDTO workspaceDefaults = workspaceDefaultsService.getWorkspaceModelDefaults(workspaceId);
		ModelDefaultsDTO profileDefaults = profileDefaultsService.getModelDefaults();

		EffectiveModelDefaultsDTO.ResolvedModel chat = resolveChat(workspaceId, workspaceDefaults, profileDefaults);
		EffectiveModelDefaultsDTO.ResolvedModel embedding = resolveEmbedding(workspaceId, kbId, workspaceDefaults,
				profileDefaults);
		return new EffectiveModelDefaultsDTO(chat, embedding);
	}

	private EffectiveModelDefaultsDTO.ResolvedModel resolveChat(String workspaceId, ModelDefaultsDTO workspaceDefaults,
			ModelDefaultsDTO profileDefaults) {
		if (workspaceDefaults.chat() != null && hasModelRef(workspaceDefaults.chat().provider(),
				workspaceDefaults.chat().modelId())) {
			return new EffectiveModelDefaultsDTO.ResolvedModel(workspaceDefaults.chat().provider(),
					workspaceDefaults.chat().modelId(), workspaceDefaults.chat().parameters(),
					EffectiveModelDefaultsDTO.Source.WORKSPACE, null);
		}

		if (profileDefaults.chat() != null && hasModelRef(profileDefaults.chat().provider(),
				profileDefaults.chat().modelId())) {
			return new EffectiveModelDefaultsDTO.ResolvedModel(profileDefaults.chat().provider(),
					profileDefaults.chat().modelId(), profileDefaults.chat().parameters(),
					EffectiveModelDefaultsDTO.Source.LOCAL_PROFILE, null);
		}

		return modelLookupRepository.findFirstEnabledModel(workspaceId, MODEL_TYPE_LLM)
			.map(model -> resolvedFallback(model, null))
			.orElseGet(() -> unresolved("No enabled chat model found for workspace."));
	}

	private EffectiveModelDefaultsDTO.ResolvedModel resolveEmbedding(String workspaceId, String kbId,
			ModelDefaultsDTO workspaceDefaults, ModelDefaultsDTO profileDefaults) {
		if (kbId != null && !kbId.isBlank()) {
			EffectiveModelDefaultsDTO.ResolvedModel kbEmbedding = resolveKbEmbedding(workspaceId, kbId);
			if (kbEmbedding != null) {
				return kbEmbedding;
			}
		}

		if (workspaceDefaults.embedding() != null && hasModelRef(workspaceDefaults.embedding().provider(),
				workspaceDefaults.embedding().modelId())) {
			return new EffectiveModelDefaultsDTO.ResolvedModel(workspaceDefaults.embedding().provider(),
					workspaceDefaults.embedding().modelId(), null, EffectiveModelDefaultsDTO.Source.WORKSPACE, null);
		}

		if (profileDefaults.embedding() != null && hasModelRef(profileDefaults.embedding().provider(),
				profileDefaults.embedding().modelId())) {
			return new EffectiveModelDefaultsDTO.ResolvedModel(profileDefaults.embedding().provider(),
					profileDefaults.embedding().modelId(), null, EffectiveModelDefaultsDTO.Source.LOCAL_PROFILE, null);
		}

		return modelLookupRepository.findFirstEnabledModel(workspaceId, MODEL_TYPE_EMBEDDING)
			.map(model -> resolvedFallback(model, null))
			.orElseGet(() -> unresolved("No enabled embedding model found for workspace."));
	}

	private EffectiveModelDefaultsDTO.ResolvedModel resolveKbEmbedding(String workspaceId, String kbId) {
		String indexConfigJson = knowledgeBaseRepository
			.findIndexConfig(DesktopLocalConstants.LOCAL_PROFILE_ID, workspaceId, kbId)
			.orElseThrow(() -> new BizException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND.toError()));
		Map<String, Object> indexConfig = indexConfigJson == null || indexConfigJson.isBlank() ? Map.of()
				: JsonUtils.fromJsonToMap(indexConfigJson);

		String provider = stringValue(indexConfig.get("embedding_provider"));
		String modelId = stringValue(indexConfig.get("embedding_model"));
		boolean hasExplicitEmbedding = hasModelRef(provider, modelId);
		boolean inheritEmbeddingDefault = resolveInheritEmbeddingDefault(indexConfig, hasExplicitEmbedding);

		if (!inheritEmbeddingDefault && hasExplicitEmbedding) {
			return new EffectiveModelDefaultsDTO.ResolvedModel(provider, modelId, null,
					EffectiveModelDefaultsDTO.Source.KB, null);
		}
		if (!inheritEmbeddingDefault) {
			return unresolved("KB embedding inheritance is disabled but explicit embedding is incomplete.");
		}
		return null;
	}

	private boolean resolveInheritEmbeddingDefault(Map<String, Object> indexConfig, boolean hasExplicitEmbedding) {
		Object value = indexConfig.get("inherit_embedding_default");
		if (value == null) {
			return !hasExplicitEmbedding;
		}
		if (value instanceof Boolean bool) {
			return bool;
		}
		return Boolean.parseBoolean(String.valueOf(value));
	}

	private EffectiveModelDefaultsDTO.ResolvedModel resolvedFallback(DesktopLocalModelRef model,
			Map<String, Object> parameters) {
		return new EffectiveModelDefaultsDTO.ResolvedModel(model.provider(), model.modelId(), parameters,
				EffectiveModelDefaultsDTO.Source.FALLBACK_ENABLED_MODEL, null);
	}

	private EffectiveModelDefaultsDTO.ResolvedModel unresolved(String message) {
		return new EffectiveModelDefaultsDTO.ResolvedModel(null, null, null, EffectiveModelDefaultsDTO.Source.UNRESOLVED,
				message);
	}

	private boolean hasModelRef(String provider, String modelId) {
		return !isBlank(provider) && !isBlank(modelId);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}

}

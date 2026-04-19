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
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalKnowledgeBaseDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalKnowledgeBaseRepository;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalWorkspaceRepository;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for desktop-local knowledge base metadata.
 */
@Service
public class DesktopLocalKnowledgeBaseService {

	private static final String DEFAULT_KB_TYPE = "unstructured";

	private final DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository;

	private final DesktopLocalWorkspaceRepository workspaceRepository;

	public DesktopLocalKnowledgeBaseService(DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository,
			DesktopLocalWorkspaceRepository workspaceRepository) {
		this.knowledgeBaseRepository = knowledgeBaseRepository;
		this.workspaceRepository = workspaceRepository;
	}

	public String createKnowledgeBase(DesktopLocalKnowledgeBaseDTO request) {
		KnowledgeBaseValues values = validateAndNormalize(null, request);
		String kbId = "kb_" + UUID.randomUUID().toString().replace("-", "");
		knowledgeBaseRepository.insertKnowledgeBase(DesktopLocalConstants.LOCAL_PROFILE_ID, values.toMap(kbId));
		return kbId;
	}

	public void updateKnowledgeBase(String kbId, DesktopLocalKnowledgeBaseDTO request) {
		if (kbId == null || kbId.isBlank()) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("kb_id"));
		}
		KnowledgeBaseValues values = validateAndNormalize(kbId, request);
		int updated = knowledgeBaseRepository.updateKnowledgeBase(DesktopLocalConstants.LOCAL_PROFILE_ID, kbId,
				values.toMap(kbId));
		if (updated <= 0) {
			throw new BizException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND.toError());
		}
	}

	private KnowledgeBaseValues validateAndNormalize(String existingKbId, DesktopLocalKnowledgeBaseDTO request) {
		if (request == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("knowledge_base"));
		}
		if (isBlank(request.workspaceId())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspace_id"));
		}
		if (!workspaceRepository.existsActiveWorkspace(DesktopLocalConstants.LOCAL_PROFILE_ID, request.workspaceId())) {
			throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND.toError());
		}
		if (isBlank(request.name())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}
		if (request.processConfig() == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("process_config"));
		}
		if (request.indexConfig() == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("index_config"));
		}
		if (knowledgeBaseRepository.existsActiveName(DesktopLocalConstants.LOCAL_PROFILE_ID, request.workspaceId(),
				request.name(), existingKbId)) {
			throw new BizException(ErrorCode.KNOWLEDGE_BASE_NAME_EXISTS.toError());
		}

		Map<String, Object> indexConfig = normalizeIndexConfig(request.indexConfig());
		Map<String, Object> searchConfig = request.searchConfig() == null ? Map.of() : request.searchConfig();
		String type = isBlank(request.type()) ? DEFAULT_KB_TYPE : request.type();
		return new KnowledgeBaseValues(request.workspaceId(), type, request.name(), request.description(),
				request.processConfig(), indexConfig, searchConfig);
	}

	private Map<String, Object> normalizeIndexConfig(Map<String, Object> rawIndexConfig) {
		Map<String, Object> indexConfig = new LinkedHashMap<>(rawIndexConfig);
		String embeddingProvider = stringValue(indexConfig.get("embedding_provider"));
		String embeddingModel = stringValue(indexConfig.get("embedding_model"));
		boolean hasProvider = !isBlank(embeddingProvider);
		boolean hasModel = !isBlank(embeddingModel);
		if (hasProvider != hasModel) {
			throw new BizException(ErrorCode.INVALID_PARAMS
				.toError("index_config", "embedding_provider and embedding_model must be both set or both empty"));
		}

		boolean hasExplicitEmbedding = hasProvider && hasModel;
		boolean inheritEmbeddingDefault = resolveInheritEmbeddingDefault(indexConfig, hasExplicitEmbedding);
		indexConfig.put("inherit_embedding_default", inheritEmbeddingDefault);
		if (!inheritEmbeddingDefault && !hasExplicitEmbedding) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("embedding_provider/embedding_model"));
		}
		return indexConfig;
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

	private String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record KnowledgeBaseValues(String workspaceId, String type, String name, String description,
			Map<String, Object> processConfig, Map<String, Object> indexConfig, Map<String, Object> searchConfig) {

		Map<String, Object> toMap(String kbId) {
			Map<String, Object> values = new LinkedHashMap<>();
			values.put("kbId", kbId);
			values.put("workspaceId", workspaceId);
			values.put("type", type);
			values.put("name", name);
			values.put("description", description);
			values.put("processConfig", JsonUtils.toJson(processConfig));
			values.put("indexConfig", JsonUtils.toJson(indexConfig));
			values.put("searchConfig", JsonUtils.toJson(searchConfig));
			values.put("operator", DesktopLocalConstants.LOCAL_PROFILE_ID);
			return values;
		}

	}

}

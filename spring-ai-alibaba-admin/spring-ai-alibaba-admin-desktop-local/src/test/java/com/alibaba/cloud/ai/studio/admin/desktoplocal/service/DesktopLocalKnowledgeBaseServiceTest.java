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

import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalKnowledgeBaseDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalKnowledgeBaseRepository;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalWorkspaceRepository;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesktopLocalKnowledgeBaseServiceTest {

	@Test
	void createKnowledgeBaseAllowsInheritedEmbeddingWithoutExplicitModel() {
		DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository = mock(DesktopLocalKnowledgeBaseRepository.class);
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(workspaceRepository.existsActiveWorkspace(anyString(), anyString())).thenReturn(true);
		DesktopLocalKnowledgeBaseService service = new DesktopLocalKnowledgeBaseService(knowledgeBaseRepository,
				workspaceRepository);
		DesktopLocalKnowledgeBaseDTO request = new DesktopLocalKnowledgeBaseDTO(null, "ws_1", null, "Contracts", null,
				Map.of("chunk_strategy", "whole_document"), Map.of("inherit_embedding_default", true), null);

		String kbId = service.createKnowledgeBase(request);

		assertThat(kbId).startsWith("kb_");
		verify(knowledgeBaseRepository).insertKnowledgeBase(anyString(), any());
	}

	@Test
	void createKnowledgeBaseRejectsMissingEmbeddingWhenInheritanceDisabled() {
		DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository = mock(DesktopLocalKnowledgeBaseRepository.class);
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(workspaceRepository.existsActiveWorkspace(anyString(), anyString())).thenReturn(true);
		DesktopLocalKnowledgeBaseService service = new DesktopLocalKnowledgeBaseService(knowledgeBaseRepository,
				workspaceRepository);
		DesktopLocalKnowledgeBaseDTO request = new DesktopLocalKnowledgeBaseDTO(null, "ws_1", null, "Contracts", null,
				Map.of("chunk_strategy", "whole_document"), Map.of("inherit_embedding_default", false), null);

		assertThatThrownBy(() -> service.createKnowledgeBase(request)).isInstanceOf(BizException.class);
	}

	@Test
	void createKnowledgeBaseRejectsPartialEmbeddingConfig() {
		DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository = mock(DesktopLocalKnowledgeBaseRepository.class);
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(workspaceRepository.existsActiveWorkspace(anyString(), anyString())).thenReturn(true);
		DesktopLocalKnowledgeBaseService service = new DesktopLocalKnowledgeBaseService(knowledgeBaseRepository,
				workspaceRepository);
		DesktopLocalKnowledgeBaseDTO request = new DesktopLocalKnowledgeBaseDTO(null, "ws_1", null, "Contracts", null,
				Map.of("chunk_strategy", "standard"), Map.of("embedding_provider", "Tongyi"), null);

		assertThatThrownBy(() -> service.createKnowledgeBase(request)).isInstanceOf(BizException.class);
	}

	@Test
	void updateKnowledgeBaseRejectsMissingRow() {
		DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository = mock(DesktopLocalKnowledgeBaseRepository.class);
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(workspaceRepository.existsActiveWorkspace(anyString(), anyString())).thenReturn(true);
		when(knowledgeBaseRepository.updateKnowledgeBase(anyString(), anyString(), any())).thenReturn(0);
		DesktopLocalKnowledgeBaseService service = new DesktopLocalKnowledgeBaseService(knowledgeBaseRepository,
				workspaceRepository);
		DesktopLocalKnowledgeBaseDTO request = new DesktopLocalKnowledgeBaseDTO(null, "ws_1", null, "Contracts", null,
				Map.of("chunk_strategy", "standard"),
				Map.of("embedding_provider", "Tongyi", "embedding_model", "text-embedding-v3"), null);

		assertThatThrownBy(() -> service.updateKnowledgeBase("missing", request)).isInstanceOf(BizException.class);
	}

}

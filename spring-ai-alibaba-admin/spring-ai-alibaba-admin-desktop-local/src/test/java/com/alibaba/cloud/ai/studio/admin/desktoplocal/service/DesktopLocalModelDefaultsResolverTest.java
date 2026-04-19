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

import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalModelRef;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.EffectiveModelDefaultsDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.ModelDefaultsDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalKnowledgeBaseRepository;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalModelLookupRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesktopLocalModelDefaultsResolverTest {

	@Test
	void workspaceChatOverrideWinsOverProfileDefault() {
		DesktopLocalWorkspaceModelDefaultsService workspaceDefaults = mock(DesktopLocalWorkspaceModelDefaultsService.class);
		DesktopLocalModelDefaultsService profileDefaults = mock(DesktopLocalModelDefaultsService.class);
		DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository = mock(DesktopLocalKnowledgeBaseRepository.class);
		DesktopLocalModelLookupRepository modelLookupRepository = mock(DesktopLocalModelLookupRepository.class);
		when(workspaceDefaults.getWorkspaceModelDefaults("ws_1"))
			.thenReturn(new ModelDefaultsDTO(new ModelDefaultsDTO.ChatDefaults("Tongyi", "qwen-plus", null), null));
		when(profileDefaults.getModelDefaults())
			.thenReturn(new ModelDefaultsDTO(new ModelDefaultsDTO.ChatDefaults("Tongyi", "qwen-turbo", null),
					new ModelDefaultsDTO.EmbeddingDefaults("Tongyi", "text-embedding-v3")));
		DesktopLocalModelDefaultsResolver resolver = new DesktopLocalModelDefaultsResolver(workspaceDefaults,
				profileDefaults, knowledgeBaseRepository, modelLookupRepository);

		EffectiveModelDefaultsDTO result = resolver.resolve("ws_1", null);

		assertThat(result.chat().modelId()).isEqualTo("qwen-plus");
		assertThat(result.chat().source()).isEqualTo(EffectiveModelDefaultsDTO.Source.WORKSPACE);
		assertThat(result.embedding().source()).isEqualTo(EffectiveModelDefaultsDTO.Source.LOCAL_PROFILE);
	}

	@Test
	void kbExplicitEmbeddingWinsWhenInheritanceDisabled() {
		DesktopLocalWorkspaceModelDefaultsService workspaceDefaults = mock(DesktopLocalWorkspaceModelDefaultsService.class);
		DesktopLocalModelDefaultsService profileDefaults = mock(DesktopLocalModelDefaultsService.class);
		DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository = mock(DesktopLocalKnowledgeBaseRepository.class);
		DesktopLocalModelLookupRepository modelLookupRepository = mock(DesktopLocalModelLookupRepository.class);
		when(workspaceDefaults.getWorkspaceModelDefaults("ws_1")).thenReturn(new ModelDefaultsDTO(null,
				new ModelDefaultsDTO.EmbeddingDefaults("Tongyi", "text-embedding-v2")));
		when(profileDefaults.getModelDefaults()).thenReturn(new ModelDefaultsDTO(null,
				new ModelDefaultsDTO.EmbeddingDefaults("Tongyi", "text-embedding-v3")));
		when(knowledgeBaseRepository.findIndexConfig(anyString(), anyString(), anyString()))
			.thenReturn(Optional.of("""
					{"inherit_embedding_default":false,"embedding_provider":"Tongyi","embedding_model":"text-embedding-v1"}
					"""));
		DesktopLocalModelDefaultsResolver resolver = new DesktopLocalModelDefaultsResolver(workspaceDefaults,
				profileDefaults, knowledgeBaseRepository, modelLookupRepository);

		EffectiveModelDefaultsDTO result = resolver.resolve("ws_1", "kb_1");

		assertThat(result.embedding().modelId()).isEqualTo("text-embedding-v1");
		assertThat(result.embedding().source()).isEqualTo(EffectiveModelDefaultsDTO.Source.KB);
	}

	@Test
	void fallbackModelUsedWhenNoDefaultsSet() {
		DesktopLocalWorkspaceModelDefaultsService workspaceDefaults = mock(DesktopLocalWorkspaceModelDefaultsService.class);
		DesktopLocalModelDefaultsService profileDefaults = mock(DesktopLocalModelDefaultsService.class);
		DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository = mock(DesktopLocalKnowledgeBaseRepository.class);
		DesktopLocalModelLookupRepository modelLookupRepository = mock(DesktopLocalModelLookupRepository.class);
		when(workspaceDefaults.getWorkspaceModelDefaults("ws_1")).thenReturn(new ModelDefaultsDTO(null, null));
		when(profileDefaults.getModelDefaults()).thenReturn(new ModelDefaultsDTO(null, null));
		when(modelLookupRepository.findFirstEnabledModel("ws_1", "llm"))
			.thenReturn(Optional.of(new DesktopLocalModelRef("Tongyi", "qwen-turbo")));
		when(modelLookupRepository.findFirstEnabledModel("ws_1", "text_embedding"))
			.thenReturn(Optional.of(new DesktopLocalModelRef("Tongyi", "text-embedding-v3")));
		DesktopLocalModelDefaultsResolver resolver = new DesktopLocalModelDefaultsResolver(workspaceDefaults,
				profileDefaults, knowledgeBaseRepository, modelLookupRepository);

		EffectiveModelDefaultsDTO result = resolver.resolve("ws_1", null);

		assertThat(result.chat().modelId()).isEqualTo("qwen-turbo");
		assertThat(result.chat().source()).isEqualTo(EffectiveModelDefaultsDTO.Source.FALLBACK_ENABLED_MODEL);
		assertThat(result.embedding().modelId()).isEqualTo("text-embedding-v3");
		assertThat(result.embedding().source()).isEqualTo(EffectiveModelDefaultsDTO.Source.FALLBACK_ENABLED_MODEL);
	}

	@Test
	void unresolvedReturnedWhenFallbackMissing() {
		DesktopLocalWorkspaceModelDefaultsService workspaceDefaults = mock(DesktopLocalWorkspaceModelDefaultsService.class);
		DesktopLocalModelDefaultsService profileDefaults = mock(DesktopLocalModelDefaultsService.class);
		DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository = mock(DesktopLocalKnowledgeBaseRepository.class);
		DesktopLocalModelLookupRepository modelLookupRepository = mock(DesktopLocalModelLookupRepository.class);
		when(workspaceDefaults.getWorkspaceModelDefaults("ws_1")).thenReturn(new ModelDefaultsDTO(null, null));
		when(profileDefaults.getModelDefaults()).thenReturn(new ModelDefaultsDTO(null, null));
		when(modelLookupRepository.findFirstEnabledModel(anyString(), anyString())).thenReturn(Optional.empty());
		DesktopLocalModelDefaultsResolver resolver = new DesktopLocalModelDefaultsResolver(workspaceDefaults,
				profileDefaults, knowledgeBaseRepository, modelLookupRepository);

		EffectiveModelDefaultsDTO result = resolver.resolve("ws_1", null);

		assertThat(result.chat().source()).isEqualTo(EffectiveModelDefaultsDTO.Source.UNRESOLVED);
		assertThat(result.embedding().source()).isEqualTo(EffectiveModelDefaultsDTO.Source.UNRESOLVED);
		assertThat(result.chat().message()).isNotBlank();
	}

	@Test
	void kbIncompleteExplicitEmbeddingStopsFallback() {
		DesktopLocalWorkspaceModelDefaultsService workspaceDefaults = mock(DesktopLocalWorkspaceModelDefaultsService.class);
		DesktopLocalModelDefaultsService profileDefaults = mock(DesktopLocalModelDefaultsService.class);
		DesktopLocalKnowledgeBaseRepository knowledgeBaseRepository = mock(DesktopLocalKnowledgeBaseRepository.class);
		DesktopLocalModelLookupRepository modelLookupRepository = mock(DesktopLocalModelLookupRepository.class);
		when(workspaceDefaults.getWorkspaceModelDefaults("ws_1")).thenReturn(new ModelDefaultsDTO(null,
				new ModelDefaultsDTO.EmbeddingDefaults("Tongyi", "text-embedding-v2")));
		when(profileDefaults.getModelDefaults()).thenReturn(new ModelDefaultsDTO(null, null));
		when(knowledgeBaseRepository.findIndexConfig(anyString(), anyString(), anyString()))
			.thenReturn(Optional.of("""
					{"inherit_embedding_default":false,"embedding_provider":"Tongyi"}
					"""));
		DesktopLocalModelDefaultsResolver resolver = new DesktopLocalModelDefaultsResolver(workspaceDefaults,
				profileDefaults, knowledgeBaseRepository, modelLookupRepository);

		EffectiveModelDefaultsDTO result = resolver.resolve("ws_1", "kb_1");

		assertThat(result.embedding().source()).isEqualTo(EffectiveModelDefaultsDTO.Source.UNRESOLVED);
		assertThat(result.embedding().message()).contains("incomplete");
	}

}

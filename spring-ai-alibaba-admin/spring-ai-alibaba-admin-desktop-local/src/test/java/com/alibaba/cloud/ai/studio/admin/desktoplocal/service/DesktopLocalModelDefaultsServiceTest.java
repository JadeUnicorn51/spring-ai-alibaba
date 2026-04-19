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

import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.ModelDefaultsDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalSettingRepository;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesktopLocalModelDefaultsServiceTest {

	@Test
	void getModelDefaultsReturnsEmptyDefaultsWhenUnset() {
		DesktopLocalSettingRepository repository = mock(DesktopLocalSettingRepository.class);
		when(repository.findValue(anyString(), anyString())).thenReturn(Optional.empty());
		DesktopLocalModelDefaultsService service = new DesktopLocalModelDefaultsService(repository);

		ModelDefaultsDTO result = service.getModelDefaults();

		assertThat(result.chat()).isNull();
		assertThat(result.embedding()).isNull();
	}

	@Test
	void saveModelDefaultsPersistsValidPayload() {
		DesktopLocalSettingRepository repository = mock(DesktopLocalSettingRepository.class);
		DesktopLocalModelDefaultsService service = new DesktopLocalModelDefaultsService(repository);
		ModelDefaultsDTO request = new ModelDefaultsDTO(
				new ModelDefaultsDTO.ChatDefaults("Tongyi", "qwen-plus", Map.of("temperature", 0.2)),
				new ModelDefaultsDTO.EmbeddingDefaults("Tongyi", "text-embedding-v3"));

		ModelDefaultsDTO result = service.saveModelDefaults(request, "local");

		assertThat(result).isEqualTo(request);
		verify(repository).upsertValue(anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void saveModelDefaultsRejectsUnsupportedChatParameter() {
		DesktopLocalSettingRepository repository = mock(DesktopLocalSettingRepository.class);
		DesktopLocalModelDefaultsService service = new DesktopLocalModelDefaultsService(repository);
		ModelDefaultsDTO request = new ModelDefaultsDTO(
				new ModelDefaultsDTO.ChatDefaults("Tongyi", "qwen-plus", Map.of("frequency_penalty", 0.1)), null);

		assertThatThrownBy(() -> service.saveModelDefaults(request, "local")).isInstanceOf(BizException.class);
	}

	@Test
	void saveModelDefaultsRejectsPartialModelReference() {
		DesktopLocalSettingRepository repository = mock(DesktopLocalSettingRepository.class);
		DesktopLocalModelDefaultsService service = new DesktopLocalModelDefaultsService(repository);
		ModelDefaultsDTO request = new ModelDefaultsDTO(new ModelDefaultsDTO.ChatDefaults("Tongyi", null, null), null);

		assertThatThrownBy(() -> service.saveModelDefaults(request, "local")).isInstanceOf(BizException.class);
	}

}

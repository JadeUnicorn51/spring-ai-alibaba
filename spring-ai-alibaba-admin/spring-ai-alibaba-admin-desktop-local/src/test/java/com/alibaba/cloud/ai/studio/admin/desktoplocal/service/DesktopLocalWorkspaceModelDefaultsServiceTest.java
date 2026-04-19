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
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalWorkspaceRepository;
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

class DesktopLocalWorkspaceModelDefaultsServiceTest {

	@Test
	void getWorkspaceModelDefaultsReadsConfigValue() {
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(workspaceRepository.existsActiveWorkspace(anyString(), anyString())).thenReturn(true);
		when(workspaceRepository.findWorkspaceConfig(anyString(), anyString()))
			.thenReturn(Optional.of("""
					{"theme":"dark","model_defaults":{"chat":{"provider":"Tongyi","model_id":"qwen-plus"}}}
					"""));
		DesktopLocalWorkspaceModelDefaultsService service = createService(workspaceRepository);

		ModelDefaultsDTO result = service.getWorkspaceModelDefaults("ws_1");

		assertThat(result.chat().provider()).isEqualTo("Tongyi");
		assertThat(result.chat().modelId()).isEqualTo("qwen-plus");
	}

	@Test
	void saveWorkspaceModelDefaultsPreservesOtherConfigKeys() {
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(workspaceRepository.existsActiveWorkspace(anyString(), anyString())).thenReturn(true);
		when(workspaceRepository.findWorkspaceConfig(anyString(), anyString()))
			.thenReturn(Optional.of("{\"theme\":\"dark\"}"));
		when(workspaceRepository.updateWorkspaceConfig(anyString(), anyString(), anyString())).thenReturn(1);
		DesktopLocalWorkspaceModelDefaultsService service = createService(workspaceRepository);
		ModelDefaultsDTO request = new ModelDefaultsDTO(
				new ModelDefaultsDTO.ChatDefaults("Tongyi", "qwen-plus", Map.of("top_p", 1.0)), null);

		ModelDefaultsDTO result = service.saveWorkspaceModelDefaults("ws_1", request);

		assertThat(result).isEqualTo(request);
		verify(workspaceRepository).updateWorkspaceConfig(anyString(), anyString(),
				org.mockito.ArgumentMatchers.contains("\"theme\":\"dark\""));
	}

	@Test
	void saveWorkspaceModelDefaultsRejectsMissingWorkspace() {
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(workspaceRepository.existsActiveWorkspace(anyString(), anyString())).thenReturn(false);
		DesktopLocalWorkspaceModelDefaultsService service = createService(workspaceRepository);

		assertThatThrownBy(() -> service.saveWorkspaceModelDefaults("missing", new ModelDefaultsDTO(null, null)))
			.isInstanceOf(BizException.class);
	}

	private DesktopLocalWorkspaceModelDefaultsService createService(DesktopLocalWorkspaceRepository workspaceRepository) {
		DesktopLocalSettingRepository settingRepository = mock(DesktopLocalSettingRepository.class);
		DesktopLocalModelDefaultsService modelDefaultsService = new DesktopLocalModelDefaultsService(settingRepository);
		return new DesktopLocalWorkspaceModelDefaultsService(workspaceRepository, modelDefaultsService);
	}

}

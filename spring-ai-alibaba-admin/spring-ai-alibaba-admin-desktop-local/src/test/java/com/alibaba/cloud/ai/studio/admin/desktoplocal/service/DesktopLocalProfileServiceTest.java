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

import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalProfileDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalProfileRepository;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalWorkspaceRepository;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DesktopLocalProfileServiceTest {

	@Test
	void getProfileReturnsLocalFallbackWhenUnset() {
		DesktopLocalProfileRepository profileRepository = mock(DesktopLocalProfileRepository.class);
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(profileRepository.findProfile(anyString())).thenReturn(Optional.empty());
		DesktopLocalProfileService service = new DesktopLocalProfileService(profileRepository, workspaceRepository);

		DesktopLocalProfileDTO profile = service.getProfile();

		assertThat(profile.profileId()).isEqualTo("local");
		assertThat(profile.accountId()).isEqualTo("local");
		assertThat(profile.defaultWorkspaceId()).isNull();
	}

	@Test
	void switchDefaultWorkspacePersistsExistingWorkspace() {
		DesktopLocalProfileRepository profileRepository = mock(DesktopLocalProfileRepository.class);
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(workspaceRepository.existsActiveWorkspace(anyString(), anyString())).thenReturn(true);
		when(profileRepository.findProfile(anyString()))
			.thenReturn(Optional.of(new DesktopLocalProfileDTO("local", "local", "ws_1")));
		DesktopLocalProfileService service = new DesktopLocalProfileService(profileRepository, workspaceRepository);

		DesktopLocalProfileDTO profile = service.switchDefaultWorkspace("ws_1");

		assertThat(profile.defaultWorkspaceId()).isEqualTo("ws_1");
		verify(profileRepository).upsertDefaultWorkspace(anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void switchDefaultWorkspaceRejectsMissingWorkspace() {
		DesktopLocalProfileRepository profileRepository = mock(DesktopLocalProfileRepository.class);
		DesktopLocalWorkspaceRepository workspaceRepository = mock(DesktopLocalWorkspaceRepository.class);
		when(workspaceRepository.existsActiveWorkspace(anyString(), anyString())).thenReturn(false);
		DesktopLocalProfileService service = new DesktopLocalProfileService(profileRepository, workspaceRepository);

		assertThatThrownBy(() -> service.switchDefaultWorkspace("missing")).isInstanceOf(BizException.class);
	}

}

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
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalProfileDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalProfileRepository;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.repository.DesktopLocalWorkspaceRepository;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import org.springframework.stereotype.Service;

/**
 * Service for desktop-local account profile state.
 */
@Service
public class DesktopLocalProfileService {

	private final DesktopLocalProfileRepository profileRepository;

	private final DesktopLocalWorkspaceRepository workspaceRepository;

	public DesktopLocalProfileService(DesktopLocalProfileRepository profileRepository,
			DesktopLocalWorkspaceRepository workspaceRepository) {
		this.profileRepository = profileRepository;
		this.workspaceRepository = workspaceRepository;
	}

	public DesktopLocalProfileDTO getProfile() {
		return profileRepository.findProfile(DesktopLocalConstants.LOCAL_PROFILE_ID)
			.orElseGet(() -> new DesktopLocalProfileDTO(DesktopLocalConstants.LOCAL_PROFILE_ID,
					DesktopLocalConstants.LOCAL_ACCOUNT_ID, null));
	}

	public DesktopLocalProfileDTO switchDefaultWorkspace(String workspaceId) {
		if (workspaceId == null || workspaceId.isBlank()) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspace_id"));
		}
		if (!workspaceRepository.existsActiveWorkspace(DesktopLocalConstants.LOCAL_PROFILE_ID, workspaceId)) {
			throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND.toError());
		}

		profileRepository.upsertDefaultWorkspace(DesktopLocalConstants.LOCAL_PROFILE_ID,
				DesktopLocalConstants.LOCAL_ACCOUNT_ID, workspaceId, DesktopLocalConstants.LOCAL_PROFILE_ID);
		return getProfile();
	}

}

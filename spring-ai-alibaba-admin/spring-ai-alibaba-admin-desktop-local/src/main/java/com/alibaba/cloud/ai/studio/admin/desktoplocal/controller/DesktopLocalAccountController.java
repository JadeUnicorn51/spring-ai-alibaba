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

package com.alibaba.cloud.ai.studio.admin.desktoplocal.controller;

import com.alibaba.cloud.ai.studio.admin.desktoplocal.DesktopLocalConstants;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalProfileDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.WorkspaceSwitchRequest;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.service.DesktopLocalProfileService;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Desktop-local account profile APIs.
 */
@RestController
@RequestMapping(DesktopLocalConstants.API_PREFIX + "/accounts")
public class DesktopLocalAccountController {

	private final DesktopLocalProfileService profileService;

	public DesktopLocalAccountController(DesktopLocalProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping("/profile")
	public Result<DesktopLocalProfileDTO> getProfile() {
		return Result.success(profileService.getProfile());
	}

	@PutMapping("/profile/default-workspace")
	public Result<DesktopLocalProfileDTO> switchDefaultWorkspace(@RequestBody WorkspaceSwitchRequest request) {
		String workspaceId = request == null ? null : request.workspaceId();
		return Result.success(profileService.switchDefaultWorkspace(workspaceId));
	}

}

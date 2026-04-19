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
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.EffectiveModelDefaultsDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.ModelDefaultsDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.service.DesktopLocalModelDefaultsResolver;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.service.DesktopLocalWorkspaceModelDefaultsService;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Desktop-local workspace APIs.
 */
@RestController
@RequestMapping(DesktopLocalConstants.API_PREFIX + "/workspaces")
public class DesktopLocalWorkspaceController {

	private final DesktopLocalWorkspaceModelDefaultsService workspaceModelDefaultsService;

	private final DesktopLocalModelDefaultsResolver modelDefaultsResolver;

	public DesktopLocalWorkspaceController(DesktopLocalWorkspaceModelDefaultsService workspaceModelDefaultsService,
			DesktopLocalModelDefaultsResolver modelDefaultsResolver) {
		this.workspaceModelDefaultsService = workspaceModelDefaultsService;
		this.modelDefaultsResolver = modelDefaultsResolver;
	}

	@GetMapping("/{workspaceId}/model-defaults")
	public Result<ModelDefaultsDTO> getWorkspaceModelDefaults(@PathVariable("workspaceId") String workspaceId) {
		return Result.success(workspaceModelDefaultsService.getWorkspaceModelDefaults(workspaceId));
	}

	@PutMapping("/{workspaceId}/model-defaults")
	public Result<ModelDefaultsDTO> saveWorkspaceModelDefaults(@PathVariable("workspaceId") String workspaceId,
			@RequestBody ModelDefaultsDTO request) {
		return Result.success(workspaceModelDefaultsService.saveWorkspaceModelDefaults(workspaceId, request));
	}

	@GetMapping("/{workspaceId}/model-defaults/effective")
	public Result<EffectiveModelDefaultsDTO> getEffectiveModelDefaults(@PathVariable("workspaceId") String workspaceId,
			@RequestParam(name = "kb_id", required = false) String kbId) {
		return Result.success(modelDefaultsResolver.resolve(workspaceId, kbId));
	}

}

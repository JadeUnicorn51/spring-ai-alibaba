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
import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalKnowledgeBaseDTO;
import com.alibaba.cloud.ai.studio.admin.desktoplocal.service.DesktopLocalKnowledgeBaseService;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Desktop-local knowledge base APIs.
 */
@RestController
@RequestMapping(DesktopLocalConstants.API_PREFIX + "/knowledge-bases")
public class DesktopLocalKnowledgeBaseController {

	private final DesktopLocalKnowledgeBaseService knowledgeBaseService;

	public DesktopLocalKnowledgeBaseController(DesktopLocalKnowledgeBaseService knowledgeBaseService) {
		this.knowledgeBaseService = knowledgeBaseService;
	}

	@PostMapping
	public Result<String> createKnowledgeBase(@RequestBody DesktopLocalKnowledgeBaseDTO request) {
		return Result.success(knowledgeBaseService.createKnowledgeBase(request));
	}

	@PutMapping("/{kbId}")
	public Result<Void> updateKnowledgeBase(@PathVariable("kbId") String kbId,
			@RequestBody DesktopLocalKnowledgeBaseDTO request) {
		knowledgeBaseService.updateKnowledgeBase(kbId, request);
		return Result.success(null);
	}

}

/*
 * Copyright 2026 the original author or authors.
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

package com.alibaba.cloud.ai.studio.admin.builder.controller;

import com.alibaba.cloud.ai.studio.admin.builder.annotation.ApiModelAttribute;
import com.alibaba.cloud.ai.studio.core.base.service.SkillService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.Skill;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillImportRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillImportResult;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillQuery;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for managing reusable skills.
 *
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "skill")
@RequestMapping("/console/v1/skills")
public class SkillController {

	private final SkillService skillService;

	public SkillController(SkillService skillService) {
		this.skillService = skillService;
	}

	@PostMapping()
	public Result<String> createSkill(@RequestBody Skill skill) {
		RequestContext context = RequestContextHolder.getRequestContext();
		validateSkill(skill);
		String skillId = skillService.createSkill(skill);
		return Result.success(context.getRequestId(), skillId);
	}

	@PostMapping("/import")
	public Result<SkillImportResult> importSkill(@RequestBody SkillImportRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		SkillImportResult importResult = skillService.importSkills(request);
		return Result.success(context.getRequestId(), importResult);
	}

	@PutMapping("/{skillId}")
	public Result<Void> updateSkill(@PathVariable("skillId") String skillId, @RequestBody Skill skill) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(skillId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("skill_id"));
		}
		validateSkill(skill);
		skill.setSkillId(skillId);
		skillService.updateSkill(skill);
		return Result.success(context.getRequestId(), null);
	}

	@DeleteMapping("/{skillId}")
	public Result<Void> deleteSkill(@PathVariable("skillId") String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(skillId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("skill_id"));
		}
		skillService.deleteSkill(skillId);
		return Result.success(context.getRequestId(), null);
	}

	@GetMapping("/{skillId}")
	public Result<Skill> getSkill(@PathVariable("skillId") String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(skillId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("skill_id"));
		}
		Skill skill = skillService.getSkill(skillId);
		return Result.success(context.getRequestId(), skill);
	}

	@GetMapping()
	public Result<PagingList<Skill>> listSkills(@ApiModelAttribute BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (query == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("query"));
		}
		PagingList<Skill> skills = skillService.listSkills(query);
		return Result.success(context.getRequestId(), skills);
	}

	@PostMapping("/query-by-codes")
	public Result<List<Skill>> listSkillsByCodes(@RequestBody SkillQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (query == null || CollectionUtils.isEmpty(query.getSkillIds())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("query or skill_ids"));
		}
		List<Skill> skills = skillService.listSkills(query.getSkillIds());
		return Result.success(context.getRequestId(), skills);
	}

	private void validateSkill(Skill skill) {
		if (skill == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("skill"));
		}
		if (StringUtils.isBlank(skill.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}
	}

}

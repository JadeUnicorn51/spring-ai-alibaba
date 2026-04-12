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

package com.alibaba.cloud.ai.studio.core.base.service.impl;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.SkillScanner;
import com.alibaba.cloud.ai.studio.core.base.entity.SkillEntity;
import com.alibaba.cloud.ai.studio.core.base.mapper.SkillMapper;
import com.alibaba.cloud.ai.studio.core.base.service.SkillService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.Skill;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillImportRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillImportResult;
import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skill service implementation.
 *
 * @since 1.0.0.3
 */
@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, SkillEntity> implements SkillService {

	private static final String SKILL_FILE_NAME = "SKILL.md";

	private final SkillScanner skillScanner = new SkillScanner();

	@Override
	public String createSkill(Skill skill) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		SkillEntity existing = getSkillByName(workspaceId, skill.getName());
		if (existing != null) {
			throw new BizException(ErrorCode.SKILL_NAME_EXISTS.toError());
		}

		SkillEntity entity = new SkillEntity();
		entity.setSkillId(IdGenerator.idStr());
		entity.setWorkspaceId(workspaceId);
		entity.setTenantId(context.getTenantId());
		entity.setStatus(CommonStatus.NORMAL);
		entity.setName(skill.getName());
		entity.setDescription(skill.getDescription());
		entity.setInstruction(skill.getInstruction());
		entity.setEnabled(Boolean.FALSE.equals(skill.getEnabled()) ? 0 : 1);
		entity.setToolIds(toJson(skill.getToolIds()));
		entity.setMcpServerIds(toJson(skill.getMcpServerIds()));
		entity.setAgentComponentIds(toJson(skill.getAgentComponentIds()));
		entity.setWorkflowComponentIds(toJson(skill.getWorkflowComponentIds()));
		entity.setGmtCreate(new Date());
		entity.setGmtModified(new Date());
		entity.setCreator(context.getAccountId());
		entity.setModifier(context.getAccountId());
		this.save(entity);
		return entity.getSkillId();
	}

	@Override
	public void updateSkill(Skill skill) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();
		SkillEntity entity = getSkillEntity(workspaceId, skill.getSkillId());
		if (entity == null) {
			throw new BizException(ErrorCode.SKILL_NOT_FOUND.toError());
		}

		SkillEntity existing = getSkillByName(workspaceId, skill.getName());
		if (existing != null && !existing.getId().equals(entity.getId())) {
			throw new BizException(ErrorCode.SKILL_NAME_EXISTS.toError());
		}

		entity.setName(skill.getName());
		entity.setDescription(skill.getDescription());
		entity.setInstruction(skill.getInstruction());
		entity.setEnabled(Boolean.FALSE.equals(skill.getEnabled()) ? 0 : 1);
		entity.setToolIds(toJson(skill.getToolIds()));
		entity.setMcpServerIds(toJson(skill.getMcpServerIds()));
		entity.setAgentComponentIds(toJson(skill.getAgentComponentIds()));
		entity.setWorkflowComponentIds(toJson(skill.getWorkflowComponentIds()));
		entity.setModifier(context.getAccountId());
		entity.setGmtModified(new Date());
		this.updateById(entity);
	}

	@Override
	public void deleteSkill(String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();
		SkillEntity entity = getSkillEntity(workspaceId, skillId);
		if (entity == null) {
			throw new BizException(ErrorCode.SKILL_NOT_FOUND.toError());
		}

		entity.setStatus(CommonStatus.DELETED);
		entity.setModifier(context.getAccountId());
		entity.setGmtModified(new Date());
		this.updateById(entity);
	}

	@Override
	public Skill getSkill(String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		SkillEntity entity = getSkillEntity(context.getWorkspaceId(), skillId);
		if (entity == null) {
			throw new BizException(ErrorCode.SKILL_NOT_FOUND.toError());
		}
		return toSkillDTO(entity);
	}

	@Override
	public PagingList<Skill> listSkills(BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		Page<SkillEntity> page = new Page<>(query.getCurrent(), query.getSize());
		LambdaQueryWrapper<SkillEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SkillEntity::getWorkspaceId, context.getWorkspaceId());
		if (StringUtils.isNotBlank(query.getName())) {
			queryWrapper.like(SkillEntity::getName, query.getName());
		}
		queryWrapper.ne(SkillEntity::getStatus, CommonStatus.DELETED.getStatus());
		queryWrapper.orderByDesc(SkillEntity::getId);
		IPage<SkillEntity> pageResult = this.page(page, queryWrapper);

		List<Skill> skills;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			skills = new ArrayList<>();
		}
		else {
			skills = pageResult.getRecords().stream().map(this::toSkillDTO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), skills);
	}

	@Override
	public List<Skill> listSkills(List<String> skillIds) {
		if (CollectionUtils.isEmpty(skillIds)) {
			return new ArrayList<>();
		}
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<SkillEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SkillEntity::getWorkspaceId, context.getWorkspaceId());
		queryWrapper.in(SkillEntity::getSkillId, skillIds);
		queryWrapper.ne(SkillEntity::getStatus, CommonStatus.DELETED.getStatus());
		List<SkillEntity> entities = this.list(queryWrapper);
		if (CollectionUtils.isEmpty(entities)) {
			return new ArrayList<>();
		}

		Map<String, Skill> skillMap = new LinkedHashMap<>();
		for (SkillEntity entity : entities) {
			skillMap.put(entity.getSkillId(), toSkillDTO(entity));
		}

		List<Skill> orderedSkills = new ArrayList<>();
		for (String skillId : skillIds) {
			Skill skill = skillMap.get(skillId);
			if (skill != null) {
				orderedSkills.add(skill);
			}
		}
		return orderedSkills;
	}

	@Override
	public SkillImportResult importSkills(SkillImportRequest request) {
		if (request == null || StringUtils.isBlank(request.getDirectoryPath())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("directory_path"));
		}

		Path skillRootPath = resolveSkillRootPath(request.getDirectoryPath());
		boolean overwriteExisting = !Boolean.FALSE.equals(request.getOverwriteExisting());
		RequestContext context = RequestContextHolder.getRequestContext();

		List<SkillMetadata> skillMetadataList = skillScanner.scan(skillRootPath.toString(), "import");
		SkillImportResult result = new SkillImportResult();
		result.setDirectoryPath(skillRootPath.toString());
		result.setOverwriteExisting(overwriteExisting);
		result.setTotalCount(skillMetadataList.size());

		for (SkillMetadata metadata : skillMetadataList) {
			if (metadata == null || StringUtils.isBlank(metadata.getName())) {
				increaseFailed(result, "unknown", "invalid skill metadata");
				continue;
			}

			try {
				importSingleSkill(context, metadata, overwriteExisting, result);
			}
			catch (Exception ex) {
				increaseFailed(result, metadata.getName(), ex.getMessage());
			}
		}

		return result;
	}

	private void importSingleSkill(RequestContext context, SkillMetadata metadata, boolean overwriteExisting,
			SkillImportResult result) throws IOException {
		String workspaceId = context.getWorkspaceId();
		String skillName = metadata.getName();

		SkillEntity existingEntity = getSkillByName(workspaceId, skillName);
		if (existingEntity != null && !overwriteExisting) {
			result.setSkippedCount(result.getSkippedCount() + 1);
			return;
		}

		String skillMarkdown = readRawSkillMarkdown(metadata);
		SkillEntity entity = existingEntity == null ? new SkillEntity() : existingEntity;
		boolean createMode = existingEntity == null;

		if (createMode) {
			entity.setSkillId(IdGenerator.idStr());
			entity.setWorkspaceId(workspaceId);
			entity.setTenantId(context.getTenantId());
			entity.setStatus(CommonStatus.NORMAL);
			entity.setCreator(context.getAccountId());
			entity.setGmtCreate(new Date());
		}

		entity.setName(skillName);
		entity.setDescription(metadata.getDescription());
		entity.setInstruction(skillMarkdown);
		entity.setEnabled(1);
		entity.setToolIds(null);
		entity.setMcpServerIds(null);
		entity.setAgentComponentIds(null);
		entity.setWorkflowComponentIds(null);
		entity.setModifier(context.getAccountId());
		entity.setGmtModified(new Date());

		if (createMode) {
			this.save(entity);
			result.setImportedCount(result.getImportedCount() + 1);
		}
		else {
			this.updateById(entity);
			result.setUpdatedCount(result.getUpdatedCount() + 1);
		}
	}

	private String readRawSkillMarkdown(SkillMetadata metadata) throws IOException {
		Path skillFilePath = Paths.get(metadata.getSkillPath(), SKILL_FILE_NAME);
		if (Files.exists(skillFilePath)) {
			return Files.readString(skillFilePath);
		}
		String fullContent = metadata.getFullContent();
		return StringUtils.defaultString(fullContent);
	}

	private Path resolveSkillRootPath(String directoryPath) {
		Path skillRootPath;
		try {
			skillRootPath = Paths.get(directoryPath).toAbsolutePath().normalize();
		}
		catch (Exception ex) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("directory_path", "invalid path"));
		}

		if (!Files.exists(skillRootPath)) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("directory_path", "directory does not exist"));
		}
		if (!Files.isDirectory(skillRootPath)) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("directory_path", "path is not a directory"));
		}
		return skillRootPath;
	}

	private void increaseFailed(SkillImportResult result, String skillName, String reason) {
		result.setFailedCount(result.getFailedCount() + 1);
		String normalizedReason = StringUtils.defaultIfBlank(reason, "unknown error");
		result.getFailedSkills().add(skillName + ": " + normalizedReason);
	}

	private SkillEntity getSkillByName(String workspaceId, String name) {
		LambdaQueryWrapper<SkillEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SkillEntity::getWorkspaceId, workspaceId)
			.eq(SkillEntity::getName, name)
			.ne(SkillEntity::getStatus, CommonStatus.DELETED.getStatus())
			.last("limit 1");
		Optional<SkillEntity> entity = this.getOneOpt(queryWrapper);
		return entity.orElse(null);
	}

	private SkillEntity getSkillEntity(String workspaceId, String skillId) {
		LambdaQueryWrapper<SkillEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SkillEntity::getWorkspaceId, workspaceId)
			.eq(SkillEntity::getSkillId, skillId)
			.ne(SkillEntity::getStatus, CommonStatus.DELETED.getStatus())
			.last("limit 1");
		Optional<SkillEntity> entity = this.getOneOpt(queryWrapper);
		return entity.orElse(null);
	}

	private Skill toSkillDTO(SkillEntity entity) {
		Skill skill = new Skill();
		skill.setSkillId(entity.getSkillId());
		skill.setStatus(entity.getStatus());
		skill.setName(entity.getName());
		skill.setDescription(entity.getDescription());
		skill.setInstruction(entity.getInstruction());
		skill.setEnabled(entity.getEnabled() == null || entity.getEnabled() == 1);
		skill.setToolIds(fromJson(entity.getToolIds()));
		skill.setMcpServerIds(fromJson(entity.getMcpServerIds()));
		skill.setAgentComponentIds(fromJson(entity.getAgentComponentIds()));
		skill.setWorkflowComponentIds(fromJson(entity.getWorkflowComponentIds()));
		skill.setGmtCreate(entity.getGmtCreate());
		skill.setGmtModified(entity.getGmtModified());
		skill.setCreator(entity.getCreator());
		skill.setModifier(entity.getModifier());
		skill.setWorkspaceId(entity.getWorkspaceId());
		return skill;
	}

	private String toJson(List<String> values) {
		if (CollectionUtils.isEmpty(values)) {
			return null;
		}
		return JsonUtils.toJson(values);
	}

	private List<String> fromJson(String content) {
		if (StringUtils.isBlank(content)) {
			return new ArrayList<>();
		}
		return JsonUtils.fromJsonToList(content, String.class);
	}

}

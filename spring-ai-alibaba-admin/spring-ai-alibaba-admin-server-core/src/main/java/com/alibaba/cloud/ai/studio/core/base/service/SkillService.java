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

package com.alibaba.cloud.ai.studio.core.base.service;

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillImportRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillImportResult;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.Skill;

import java.util.List;

/**
 * Service for managing reusable skill resources.
 *
 * @since 1.0.0.3
 */
public interface SkillService {

	/**
	 * Creates a skill.
	 * @param skill skill definition
	 * @return created skill id
	 */
	String createSkill(Skill skill);

	/**
	 * Updates a skill.
	 * @param skill skill definition with skill id
	 */
	void updateSkill(Skill skill);

	/**
	 * Deletes a skill.
	 * @param skillId skill id
	 */
	void deleteSkill(String skillId);

	/**
	 * Gets a skill by id.
	 * @param skillId skill id
	 * @return skill definition
	 */
	Skill getSkill(String skillId);

	/**
	 * Lists skills with pagination.
	 * @param query query params
	 * @return paged skills
	 */
	PagingList<Skill> listSkills(BaseQuery query);

	/**
	 * Lists skills by ids.
	 * @param skillIds skill id list
	 * @return skills in request order
	 */
	List<Skill> listSkills(List<String> skillIds);

	/**
	 * Imports skills from a local directory.
	 * @param request import request
	 * @return import summary
	 */
	SkillImportResult importSkills(SkillImportRequest request);

}

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

package com.alibaba.cloud.ai.studio.admin.desktoplocal.repository;

import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalModelRef;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Read-only model lookup facade for desktop-local fallback resolution.
 */
@Repository
public class DesktopLocalModelLookupRepository {

	private final JdbcTemplate jdbcTemplate;

	public DesktopLocalModelLookupRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<DesktopLocalModelRef> findFirstEnabledModel(String workspaceId, String modelType) {
		List<DesktopLocalModelRef> models = jdbcTemplate.query("""
				SELECT m.provider, m.model_id
				FROM `model` m
				JOIN provider p
				  ON p.workspace_id = m.workspace_id
				 AND p.provider = m.provider
				 AND p.enable = 1
				WHERE m.workspace_id = ?
				  AND m.type = ?
				  AND m.enable = 1
				ORDER BY m.id ASC
				LIMIT 1
				""", (rs, rowNum) -> new DesktopLocalModelRef(rs.getString("provider"), rs.getString("model_id")),
				workspaceId, modelType);
		return models.stream().findFirst();
	}

}

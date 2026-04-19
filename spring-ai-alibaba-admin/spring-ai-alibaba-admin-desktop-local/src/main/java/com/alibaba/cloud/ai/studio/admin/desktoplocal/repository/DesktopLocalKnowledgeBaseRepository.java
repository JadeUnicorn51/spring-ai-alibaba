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

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/**
 * JDBC repository for desktop-local knowledge bases.
 */
@Repository
public class DesktopLocalKnowledgeBaseRepository {

	private final JdbcTemplate jdbcTemplate;

	public DesktopLocalKnowledgeBaseRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<String> findIndexConfig(String profileId, String workspaceId, String kbId) {
		try {
			String config = jdbcTemplate.queryForObject("""
					SELECT index_config
					FROM desktop_local_knowledge_base
					WHERE profile_id = ? AND workspace_id = ? AND kb_id = ? AND status <> 0
					""", String.class, profileId, workspaceId, kbId);
			return Optional.ofNullable(config);
		}
		catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}
	}

	public boolean existsActiveName(String profileId, String workspaceId, String name, String excludeKbId) {
		String sql = """
				SELECT COUNT(1)
				FROM desktop_local_knowledge_base
				WHERE profile_id = ? AND workspace_id = ? AND name = ? AND status <> 0
				""";
		Object[] args;
		if (excludeKbId == null || excludeKbId.isBlank()) {
			args = new Object[] { profileId, workspaceId, name };
		}
		else {
			sql = sql + " AND kb_id <> ?";
			args = new Object[] { profileId, workspaceId, name, excludeKbId };
		}
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
		return count != null && count > 0;
	}

	public void insertKnowledgeBase(String profileId, Map<String, Object> values) {
		jdbcTemplate.update("""
				INSERT INTO desktop_local_knowledge_base
				  (kb_id, workspace_id, profile_id, type, status, name, description,
				   process_config, index_config, search_config, total_docs, creator, modifier)
				VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?, ?, 0, ?, ?)
				""", values.get("kbId"), values.get("workspaceId"), profileId, values.get("type"), values.get("name"),
				values.get("description"), values.get("processConfig"), values.get("indexConfig"),
				values.get("searchConfig"), values.get("operator"), values.get("operator"));
	}

	public int updateKnowledgeBase(String profileId, String kbId, Map<String, Object> values) {
		return jdbcTemplate.update("""
				UPDATE desktop_local_knowledge_base
				SET type = ?,
				    name = ?,
				    description = ?,
				    process_config = ?,
				    index_config = ?,
				    search_config = ?,
				    modifier = ?,
				    gmt_modified = CURRENT_TIMESTAMP
				WHERE profile_id = ? AND workspace_id = ? AND kb_id = ? AND status <> 0
				""", values.get("type"), values.get("name"), values.get("description"), values.get("processConfig"),
				values.get("indexConfig"), values.get("searchConfig"), values.get("operator"), profileId,
				values.get("workspaceId"), kbId);
	}

}

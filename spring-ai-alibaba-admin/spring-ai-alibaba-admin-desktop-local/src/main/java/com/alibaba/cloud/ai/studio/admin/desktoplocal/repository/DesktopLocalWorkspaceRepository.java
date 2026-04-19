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

import java.util.Optional;

/**
 * JDBC repository for desktop-local workspaces.
 */
@Repository
public class DesktopLocalWorkspaceRepository {

	private final JdbcTemplate jdbcTemplate;

	public DesktopLocalWorkspaceRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean existsActiveWorkspace(String profileId, String workspaceId) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(1)
				FROM desktop_local_workspace
				WHERE profile_id = ? AND workspace_id = ? AND status <> 0
				""", Integer.class, profileId, workspaceId);
		return count != null && count > 0;
	}

	public Optional<String> findWorkspaceConfig(String profileId, String workspaceId) {
		try {
			String config = jdbcTemplate.queryForObject("""
					SELECT config
					FROM desktop_local_workspace
					WHERE profile_id = ? AND workspace_id = ? AND status <> 0
					""", String.class, profileId, workspaceId);
			return Optional.ofNullable(config);
		}
		catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}
	}

	public int updateWorkspaceConfig(String profileId, String workspaceId, String config) {
		return jdbcTemplate.update("""
				UPDATE desktop_local_workspace
				SET config = ?, gmt_modified = CURRENT_TIMESTAMP
				WHERE profile_id = ? AND workspace_id = ? AND status <> 0
				""", config, profileId, workspaceId);
	}

}

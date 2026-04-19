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

import com.alibaba.cloud.ai.studio.admin.desktoplocal.model.DesktopLocalProfileDTO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JDBC repository for desktop-local profile state.
 */
@Repository
public class DesktopLocalProfileRepository {

	private final JdbcTemplate jdbcTemplate;

	public DesktopLocalProfileRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<DesktopLocalProfileDTO> findProfile(String profileId) {
		try {
			DesktopLocalProfileDTO profile = jdbcTemplate.queryForObject("""
					SELECT profile_id, account_id, default_workspace_id
					FROM desktop_local_account_profile
					WHERE profile_id = ?
					""", (rs, rowNum) -> new DesktopLocalProfileDTO(rs.getString("profile_id"),
					rs.getString("account_id"), rs.getString("default_workspace_id")), profileId);
			return Optional.ofNullable(profile);
		}
		catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}
	}

	public void upsertDefaultWorkspace(String profileId, String accountId, String workspaceId, String operator) {
		jdbcTemplate.update("""
				INSERT INTO desktop_local_account_profile
				  (profile_id, account_id, default_workspace_id, creator, modifier)
				VALUES (?, ?, ?, ?, ?)
				ON DUPLICATE KEY UPDATE
				  account_id = VALUES(account_id),
				  default_workspace_id = VALUES(default_workspace_id),
				  modifier = VALUES(modifier),
				  gmt_modified = CURRENT_TIMESTAMP
				""", profileId, accountId, workspaceId, operator, operator);
	}

}

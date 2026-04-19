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
 * JDBC repository for desktop-local settings.
 */
@Repository
public class DesktopLocalSettingRepository {

	private final JdbcTemplate jdbcTemplate;

	public DesktopLocalSettingRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<String> findValue(String profileId, String settingKey) {
		try {
			String value = jdbcTemplate.queryForObject(
					"SELECT setting_value FROM desktop_local_setting WHERE profile_id = ? AND setting_key = ?",
					String.class, profileId, settingKey);
			return Optional.ofNullable(value);
		}
		catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}
	}

	public void upsertValue(String profileId, String settingKey, String settingValue, String operator) {
		jdbcTemplate.update("""
				INSERT INTO desktop_local_setting
				  (profile_id, setting_key, setting_value, creator, modifier)
				VALUES (?, ?, ?, ?, ?)
				ON DUPLICATE KEY UPDATE
				  setting_value = VALUES(setting_value),
				  modifier = VALUES(modifier),
				  gmt_modified = CURRENT_TIMESTAMP
				""", profileId, settingKey, settingValue, operator, operator);
	}

}

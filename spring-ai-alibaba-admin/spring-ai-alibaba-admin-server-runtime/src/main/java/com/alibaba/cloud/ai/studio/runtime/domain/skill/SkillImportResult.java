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

package com.alibaba.cloud.ai.studio.runtime.domain.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Import result summary for directory-based skill import.
 *
 * @since 1.0.0.3
 */
@Data
public class SkillImportResult implements Serializable {

	@JsonProperty("directory_path")
	private String directoryPath;

	@JsonProperty("overwrite_existing")
	private Boolean overwriteExisting;

	@JsonProperty("total_count")
	private Integer totalCount = 0;

	@JsonProperty("imported_count")
	private Integer importedCount = 0;

	@JsonProperty("updated_count")
	private Integer updatedCount = 0;

	@JsonProperty("skipped_count")
	private Integer skippedCount = 0;

	@JsonProperty("failed_count")
	private Integer failedCount = 0;

	@JsonProperty("failed_skills")
	private List<String> failedSkills = new ArrayList<>();

}


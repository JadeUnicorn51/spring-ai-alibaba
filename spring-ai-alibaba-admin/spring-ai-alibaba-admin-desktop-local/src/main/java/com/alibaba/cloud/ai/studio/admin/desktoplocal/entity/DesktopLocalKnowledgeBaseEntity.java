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

package com.alibaba.cloud.ai.studio.admin.desktoplocal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * Desktop-local knowledge base metadata.
 */
@TableName("desktop_local_knowledge_base")
public class DesktopLocalKnowledgeBaseEntity {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("kb_id")
	private String kbId;

	@TableField("workspace_id")
	private String workspaceId;

	@TableField("profile_id")
	private String profileId;

	private String type;

	private Integer status;

	private String name;

	private String description;

	@TableField("process_config")
	private String processConfig;

	@TableField("index_config")
	private String indexConfig;

	@TableField("search_config")
	private String searchConfig;

	@TableField("total_docs")
	private Long totalDocs;

	@TableField("gmt_create")
	private Date gmtCreate;

	@TableField("gmt_modified")
	private Date gmtModified;

	private String creator;

	private String modifier;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getKbId() {
		return kbId;
	}

	public void setKbId(String kbId) {
		this.kbId = kbId;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getProcessConfig() {
		return processConfig;
	}

	public void setProcessConfig(String processConfig) {
		this.processConfig = processConfig;
	}

	public String getIndexConfig() {
		return indexConfig;
	}

	public void setIndexConfig(String indexConfig) {
		this.indexConfig = indexConfig;
	}

	public String getSearchConfig() {
		return searchConfig;
	}

	public void setSearchConfig(String searchConfig) {
		this.searchConfig = searchConfig;
	}

	public Long getTotalDocs() {
		return totalDocs;
	}

	public void setTotalDocs(Long totalDocs) {
		this.totalDocs = totalDocs;
	}

	public Date getGmtCreate() {
		return gmtCreate;
	}

	public void setGmtCreate(Date gmtCreate) {
		this.gmtCreate = gmtCreate;
	}

	public Date getGmtModified() {
		return gmtModified;
	}

	public void setGmtModified(Date gmtModified) {
		this.gmtModified = gmtModified;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getModifier() {
		return modifier;
	}

	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

}

/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.config;

import com.alibaba.cloud.ai.studio.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

/**
 * MyBatis Plus meta object handler for automatic tenant_id filling.
 * Automatically fills tenant_id field when inserting new records.
 *
 * @since 1.0.0
 */
public class TenantMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // Skip if platform level
        if (TenantContextHolder.isPlatformLevel()) {
            return;
        }

        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return;
        }

        // Fill tenantId for entities that actually have this field
        if (metaObject.hasSetter("tenantId") && getFieldValByName("tenantId", metaObject) == null) {
            strictInsertFill(metaObject, "tenantId", String.class, tenantId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // Update fill is not needed for tenant_id
    }

}

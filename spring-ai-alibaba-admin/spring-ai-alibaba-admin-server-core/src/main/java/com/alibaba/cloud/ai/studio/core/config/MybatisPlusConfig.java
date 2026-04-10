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
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for MyBatis-Plus integration. Provides pagination support,
 * tenant data isolation, and mapper scanning configuration.
 *
 * @since 1.0.0.3
 */
@Configuration
@MapperScan("com.alibaba.cloud.ai.studio.core.base.mapper")
public class MybatisPlusConfig {

    /**
     * Tables that should NOT be filtered by tenant_id (shared tables)
     */
    private static final String[] IGNORE_TABLES = {
        "tenant", "provider", "model"
    };

    /**
     * Configures MyBatis-Plus interceptor with MySQL pagination support
     * and tenant data isolation using TenantLineInnerInterceptor.
     * @return MybatisPlusInterceptor instance
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // TenantLineInnerInterceptor must be added BEFORE pagination
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new DynamicTenantLineHandler()));

        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * Configures meta object handler for automatic tenant_id filling.
     * @return TenantMetaObjectHandler instance
     */
    @Bean
    public TenantMetaObjectHandler tenantMetaObjectHandler() {
        return new TenantMetaObjectHandler();
    }

    /**
     * Dynamic tenant line handler that reads tenant ID from TenantContextHolder.
     * Tables in IGNORE_TABLES are not filtered by tenant.
     */
    private static class DynamicTenantLineHandler implements TenantLineHandler {

        @Override
        public Expression getTenantId() {
            // Skip tenant filter for requests without auth context (e.g. login).
            if (RequestContextHolder.getRequestContext() == null) {
                return null;
            }

            // Skip if platform level (SUPER_ADMIN)
            if (TenantContextHolder.isPlatformLevel()) {
                return null;
            }

            String tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) {
                throw new IllegalStateException("Tenant context is missing for non-platform request");
            }

            return new StringValue(tenantId);
        }

        @Override
        public String getTenantIdColumn() {
            return "tenant_id";
        }

        @Override
        public boolean ignoreTable(String tableName) {
            if (tableName == null) {
                return false;
            }
            for (String ignore : IGNORE_TABLES) {
                if (ignore.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }

}

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

package com.alibaba.cloud.ai.studio.admin.builder.interceptor;

import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that cleans up ThreadLocal context after request completion.
 * This prevents memory leaks in long-running server threads.
 * Must run AFTER all other interceptors to ensure cleanup happens last.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class TenantContextCleanupInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
            @NotNull Object handler, Exception ex) {
        try {
            RequestContextHolder.clearRequestContext();
        }
        catch (Exception e) {
            log.debug("Failed to clear RequestContext", e);
        }

        try {
            TenantContextHolder.clear();
        }
        catch (Exception e) {
            log.debug("Failed to clear TenantContext", e);
        }
    }

}

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

package com.alibaba.cloud.ai.studio.admin.builder.filter;

import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.context.TenantContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter that cleans up ThreadLocal context after request completion.
 * Uses Filter instead of HandlerInterceptor because Filter's doFilter
 * runs after the entire filter chain, ensuring cleanup happens last.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE)
public class TenantContextCleanupFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        }
        finally {
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

}

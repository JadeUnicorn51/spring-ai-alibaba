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

package com.alibaba.cloud.ai.studio.core.base.service.impl;

import com.alibaba.cloud.ai.studio.core.base.entity.AccountEntity;
import com.alibaba.cloud.ai.studio.core.base.manager.ModelManager;
import com.alibaba.cloud.ai.studio.core.base.manager.ProviderManager;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.base.manager.TokenManager;
import com.alibaba.cloud.ai.studio.core.base.service.TenantService;
import com.alibaba.cloud.ai.studio.core.base.service.WorkspaceService;
import com.alibaba.cloud.ai.studio.core.config.JwtConfigProperties;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.enums.AccountStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.AccountType;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Regression tests for tenant-governance permission boundaries in account service.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTenantGovernanceTest {

	@Mock
	private TokenManager tokenManager;

	@Mock
	private JwtConfigProperties jwtConfigProperties;

	@Mock
	private RedisManager redisManager;

	@Mock
	private WorkspaceService workspaceService;

	@Mock
	private TenantService tenantService;

	@Mock
	private ProviderManager providerManager;

	@Mock
	private ModelManager modelManager;

	private AccountServiceImpl accountService;

	private final Map<String, AccountEntity> accountCache = new HashMap<>();

	@BeforeEach
	void setUp() {
		accountService = spy(new AccountServiceImpl(tokenManager, jwtConfigProperties, redisManager, workspaceService,
				tenantService, providerManager, modelManager));

		lenient().when(redisManager.get(anyString())).thenAnswer(invocation -> accountCache.get(invocation.getArgument(0)));
		lenient().doNothing().when(redisManager).put(anyString(), any());
		lenient().doNothing().when(redisManager).delete(anyString());
		lenient().doReturn(true).when(accountService).updateById(any(AccountEntity.class));
		lenient().doReturn(1L).when(accountService).count(any());
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.clearRequestContext();
		accountCache.clear();
	}

	@Test
	void updateTenantAdminStatus_shouldRejectCrossTenantOperation() {
		AccountEntity operator = buildAccount("op-super", null, AccountType.SUPER_ADMIN, AccountStatus.NORMAL);
		AccountEntity target = buildAccount("target-admin", "tenant-b", AccountType.TENANT_ADMIN, AccountStatus.NORMAL);
		cacheAccount(operator);
		cacheAccount(target);
		setRequestContext(operator.getAccountId());

		BizException exception = assertThrows(BizException.class,
				() -> accountService.updateTenantAdminStatus("tenant-a", target.getAccountId(), AccountStatus.DISABLED));

		assertEquals(ErrorCode.PERMISSION_DENIED.getCode(), exception.getError().getCode());
		verify(accountService, never()).updateById(any(AccountEntity.class));
	}

	@Test
	void listTenantAdmins_shouldRejectTenantAdminOperator() {
		AccountEntity operator = buildAccount("op-tenant", "tenant-a", AccountType.TENANT_ADMIN, AccountStatus.NORMAL);
		cacheAccount(operator);
		setRequestContext(operator.getAccountId());

		BizException exception = assertThrows(BizException.class,
				() -> accountService.listTenantAdmins("tenant-a", new BaseQuery()));

		assertEquals(ErrorCode.PERMISSION_DENIED.getCode(), exception.getError().getCode());
	}

	@Test
	void deleteTenantAdmin_shouldRejectDeletingLastTenantAdmin() {
		AccountEntity operator = buildAccount("op-super", null, AccountType.SUPER_ADMIN, AccountStatus.NORMAL);
		AccountEntity target = buildAccount("target-admin", "tenant-a", AccountType.TENANT_ADMIN, AccountStatus.NORMAL);
		cacheAccount(operator);
		cacheAccount(target);
		setRequestContext(operator.getAccountId());
		doReturn(0L).when(accountService).count(any());

		BizException exception = assertThrows(BizException.class,
				() -> accountService.deleteTenantAdmin("tenant-a", target.getAccountId()));

		assertEquals(ErrorCode.INVALID_PARAMS.getCode(), exception.getError().getCode());
		verify(redisManager, never()).delete(AccountServiceImpl.getAccountCacheKey(target.getAccountId()));
	}

	private void setRequestContext(String accountId) {
		RequestContext requestContext = new RequestContext();
		requestContext.setAccountId(accountId);
		RequestContextHolder.setRequestContext(requestContext);
	}

	private void cacheAccount(AccountEntity accountEntity) {
		accountCache.put(AccountServiceImpl.getAccountCacheKey(accountEntity.getAccountId()), accountEntity);
	}

	private AccountEntity buildAccount(String accountId, String tenantId, AccountType type, AccountStatus status) {
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setAccountId(accountId);
		accountEntity.setTenantId(tenantId);
		accountEntity.setType(type);
		accountEntity.setStatus(status);
		return accountEntity;
	}

}

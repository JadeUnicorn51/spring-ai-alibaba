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

package com.alibaba.cloud.ai.studio.core.bootstrap;

import com.alibaba.cloud.ai.studio.core.base.entity.AccountEntity;
import com.alibaba.cloud.ai.studio.core.base.service.AccountService;
import com.alibaba.cloud.ai.studio.runtime.enums.AccountStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.AccountType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Ensures platform tenant-management can be used after upgrading from legacy schema/data.
 *
 * <p>
 * Legacy deployments often seed an account as {@code type=admin} with {@code tenant_id=null}.
 * New platform management requires {@code SUPER_ADMIN}. This runner performs a safe one-time
 * promotion when no super admin exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAdminBootstrapRunner implements ApplicationRunner {

	private final AccountService accountService;

	@Override
	public void run(ApplicationArguments args) {
		AccountEntity superAdmin = findFirstAccountByType(AccountType.SUPER_ADMIN);
		if (superAdmin != null) {
			return;
		}

		AccountEntity legacyPlatformAdmin = findLegacyPlatformAdmin();
		if (legacyPlatformAdmin == null) {
			log.warn("No SUPER_ADMIN account found and no legacy platform admin can be promoted.");
			return;
		}

		legacyPlatformAdmin.setType(AccountType.SUPER_ADMIN);
		legacyPlatformAdmin.setTenantId(null);
		legacyPlatformAdmin.setGmtModified(new Date());
		accountService.updateById(legacyPlatformAdmin);

		log.info("Promoted legacy platform account [{}] to SUPER_ADMIN.", legacyPlatformAdmin.getUsername());
	}

	private AccountEntity findFirstAccountByType(AccountType type) {
		LambdaQueryWrapper<AccountEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AccountEntity::getType, type)
			.ne(AccountEntity::getStatus, AccountStatus.DELETED.getStatus())
			.orderByAsc(AccountEntity::getId)
			.last("limit 1");
		return accountService.getOne(queryWrapper, false);
	}

	private AccountEntity findLegacyPlatformAdmin() {
		LambdaQueryWrapper<AccountEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AccountEntity::getType, AccountType.ADMIN)
			.isNull(AccountEntity::getTenantId)
			.ne(AccountEntity::getStatus, AccountStatus.DELETED.getStatus())
			.orderByAsc(AccountEntity::getId)
			.last("limit 1");
		return accountService.getOne(queryWrapper, false);
	}

}

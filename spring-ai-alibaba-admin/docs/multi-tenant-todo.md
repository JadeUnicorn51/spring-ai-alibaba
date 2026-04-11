# Spring AI Alibaba Admin 多租户改造 TODO

## 1. 改造目标

基于现有 `spring-ai-alibaba-admin`，改造成“**平台级租户管理 + 租户级 Agent 构建平台**”双层架构：

- 平台层（Platform）：
  - 管理租户生命周期（创建、启停、配额、状态）。
  - 仅 `SUPER_ADMIN` 可访问。
- 租户层（Tenant Workspace）：
  - 每个租户独立使用 Agent 构建能力（应用、知识库、工具、数据集、Tracing 等）。
  - 租户之间数据强隔离，不能串读串写。

核心原则：每个租户是独立的 Agent 构建空间，平台只做治理，不直接参与租户业务数据。

---

## 2. 目标架构

### 2.1 逻辑分层

- 认证与上下文层：
  - `RequestContext` + `TenantContext` 双上下文。
  - 在拦截器中注入 `accountId/tenantId/accountType`。
- 数据隔离层：
  - MyBatis-Plus `TenantLineInnerInterceptor` + `tenant_id` 字段。
  - `MetaObjectHandler` 自动填充 `tenantId`。
- 业务层：
  - 平台 API：`/admin/v1/**`（租户治理）。
  - 租户控制台 API：`/console/v1/**`（Agent 构建）。
- 可观测层：
  - Tracing 文档写入 `tenant_id`，查询必须带租户过滤。

### 2.2 前后端边界

- 后端：
  - 已具备平台 API 与租户 API 的路径区分。
- 前端：
  - 现有 frontend 是“租户构建控制台”。
  - 需要新增/拆分“平台租户管理前端”（可同仓多入口，或独立应用）。

---

## 3. 当前已完成（本轮）

### 3.1 已落地的关键修复

- 修复多租户改造编译错误，`spring-ai-alibaba-admin` 可 `compile` 通过。
- 修复账号创建越权风险：
  - 限制租户管理员不能创建 `SUPER_ADMIN`、不能跨租户创建账号。
  - `tenantId/type` 改为服务端裁决。
- 修复租户拦截主链路：
  - 非平台请求缺失租户上下文时不再静默放行。
- 修复 `tenant_id` 自动填充逻辑：
  - 从“类名 contains 判断”改为“字段存在即填充”。
- 修复 Tenant 管理 API/Service 中错误码、状态枚举和上下文取值问题。
- 修复 Dataset/DatasetVersion/DatasetItem 多租户链路中的签名不一致和 tenant 透传。
- 清理 `application.yml` 中硬编码凭据，恢复环境变量模式。

### 3.2 平台与租户的鉴权分流

- `AdminTokenAuthInterceptor`：`/admin/v1/**` 平台权限。
- `TokenAuthInterceptor`：`/console/v1/**` 租户控制台权限。

---

## 4. 待完成（高优先级）

## P0（上线前必须）

- [ ] 数据库迁移脚本生产化
  - `tenant-schema.sql` 需改为幂等迁移（避免重复 `ADD COLUMN` 失败）。
  - 为核心表补充复合索引（如 `tenant_id + business_id`）。
- [ ] 存量数据回填
  - 老数据 `tenant_id` 回填策略与脚本（按账号、workspace、归属关系回填）。
- [ ] 隔离回归测试
  - 集成测试覆盖跨租户读写隔离、越权、租户禁用后行为。
- [ ] 异步上下文传播
  - 检查线程池/异步任务是否完整传播 `TenantContext`（不仅是 `RequestContext`）。

## P1（迭代完善）

- [ ] 配额体系真正生效
  - `max_users/max_apps/max_workspaces/max_storage_gb/max_api_calls_per_day` 在关键写路径生效。
- [ ] 平台审计
  - 新增租户管理操作审计日志（谁在什么时间改了什么）。
- [ ] 租户状态治理
  - [x] 禁用租户后的访问拦截（Token/API Key/Refresh Token）
  - [x] 前端登录态禁用提示（tenant disabled）
  - [ ] 任务停止策略
  - [ ] 消息消费策略

---

## 5. 前端是否需要同步改造：结论

需要，同步改造是必须的。

当前 frontend 是租户 Agent 构建平台，只覆盖 `console` 视角。多租户平台完整落地至少要补齐：

- 平台前端（Platform Console）
  - 租户列表、创建租户、配额调整、启停、状态查看。
  - 对接 `/admin/v1/tenants/**`。
- 租户前端（Tenant Console）
  - 保持现有 `console` 能力。
  - 增加租户维度信息展示（当前租户、配额占用、租户状态提示）。

---

## 6. 前端改造建议（实施方案）

## 方案A（推荐，低风险）：同仓双入口

- 在现有 frontend 增加一个 `platform` 入口（或 package）：
  - `tenant-console`（现有）负责 `/console/v1/**`。
  - `platform-console`（新增）负责 `/admin/v1/**`。
- 共享基础组件、请求封装、鉴权中间件，但路由和菜单隔离。

## 方案B：单入口多角色菜单（风险较高）

- 单应用通过角色显示不同菜单。
- 缺点：边界容易混淆，权限回归风险高。

---

## 7. 里程碑建议

- M1（已完成）：后端可编译 + 基础租户隔离链路可运行。
- M2：DB 幂等迁移 + 存量数据回填 + P0 集成测试。
- M3（进行中）：平台前端上线（租户管理能力可用）。
  - [x] 新增平台租户管理页面：`/admin/tenants`
  - [x] 对接 `/admin/v1/tenants/**`（列表、创建、编辑、配额、启停）
  - [x] 侧边栏增加“平台治理/租户管理”入口
  - [x] 增加平台前端权限可见性控制（仅 `SUPER_ADMIN` 展示）
- M4：配额治理、审计、租户禁用策略完善。

---

## 8. 验收标准（Definition of Done）

- [ ] 任意业务资源 CRUD 默认带租户隔离。
- [ ] 租户 A 无法访问租户 B 数据（DB + ES + 缓存 + 文件）。
- [ ] 租户管理员无法创建跨租户或平台管理员账号。
- [ ] 平台管理员可完成租户全生命周期管理。
- [ ] 平台前端 + 租户前端边界清晰，接口路径不混用。

---

## 9. 2026-04-11 Update

Completed in this round:

- [x] Platform bootstrap compatibility:
  - Auto-promote legacy platform account (`type=admin` + `tenant_id is null`) to `super_admin` when no `SUPER_ADMIN` exists.
  - File: `spring-ai-alibaba-admin-server-core/.../PlatformAdminBootstrapRunner.java`
- [x] Seed/migration sync:
  - Seed account `saa` default type changed to `super_admin`.
  - Tenant migration script adds legacy platform admin promotion SQL.
  - Files: `docker/middleware/init/mysql/agentscope-schema.sql`, `docker/middleware/init/mysql/tenant-schema.sql`
- [x] Frontend platform menu first-load fix:
  - Side menu now reacts to async user info load (no manual click required).
  - Files: `frontend/packages/main/src/layouts/LoginProvider.tsx`, `frontend/packages/main/src/layouts/SideMenuLayout.tsx`

Next step:

- [x] Add platform tenant-governance capability to create/manage tenant admin accounts directly from `/admin/tenants`.

## 10. 2026-04-11 Update (Step 2)

Completed in this round:

- [x] Backend platform API: added `POST /admin/v1/tenants/{tenantId}/admins` for creating tenant admin accounts.
  - Validation includes: platform-level permission, tenant existence, tenant enabled status, required username/password.
  - Files:
    - `spring-ai-alibaba-admin-server-start/src/main/java/com/alibaba/cloud/ai/studio/admin/controller/TenantController.java`
    - `spring-ai-alibaba-admin-server-runtime/src/main/java/com/alibaba/cloud/ai/studio/runtime/domain/tenant/TenantAdminCreateRequest.java`
- [x] Frontend platform tenant page: added "Create Admin" action and modal in `/admin/tenants`.
  - File: `frontend/packages/main/src/pages/Admin/Tenant/index.tsx`
- [x] Frontend tenant service/types updated for tenant admin creation API.
  - Files:
    - `frontend/packages/main/src/services/tenant.ts`
    - `frontend/packages/main/src/types/tenant.ts`
- [x] Build verification passed:
  - `mvn -pl spring-ai-alibaba-admin-server-start -am -DskipTests compile`
  - `npm run build -w packages/main`

Next step:

- [ ] Add tenant-admin lifecycle operations on platform side (list tenant admins, disable/enable/reset password) to complete full governance loop.

## 11. 2026-04-11 Update (Step 3)

Completed in this round:

- [x] Backend API for tenant-admin list:
  - Added `GET /admin/v1/tenants/{tenantId}/admins` to query tenant admins by tenant scope.
  - Added service-layer method `listTenantAdmins(tenantId, query)` with `tenant_id`-scoped filtering and role filter (`tenant_admin` / legacy `admin`).
  - Files:
    - `spring-ai-alibaba-admin-server-start/src/main/java/com/alibaba/cloud/ai/studio/admin/controller/TenantController.java`
    - `spring-ai-alibaba-admin-server-core/src/main/java/com/alibaba/cloud/ai/studio/core/base/service/AccountService.java`
    - `spring-ai-alibaba-admin-server-core/src/main/java/com/alibaba/cloud/ai/studio/core/base/service/impl/AccountServiceImpl.java`
- [x] Frontend platform tenant page enhancement:
  - Added `Admin List` action in `/admin/tenants`.
  - Added tenant-admin list modal with pagination (querying `/admin/v1/tenants/{tenantId}/admins`).
  - Kept existing `Create Admin` flow and auto-refresh list after successful creation.
  - Files:
    - `frontend/packages/main/src/services/tenant.ts`
    - `frontend/packages/main/src/types/tenant.ts`
    - `frontend/packages/main/src/pages/Admin/Tenant/index.tsx`
- [x] Build verification passed:
  - `mvn -pl spring-ai-alibaba-admin-server-start -am -DskipTests compile`
  - `npm run build -w packages/main`

Next step:

- [ ] Add tenant-admin lifecycle operations: enable/disable tenant admin and reset password from platform tenant governance page.

## 12. 2026-04-11 Update (Step 4)

Completed in this round:

- [x] Tenant-admin lifecycle APIs (platform side):
  - Added enable API: `POST /admin/v1/tenants/{tenantId}/admins/{accountId}/enable`
  - Added disable API: `POST /admin/v1/tenants/{tenantId}/admins/{accountId}/disable`
  - Added reset-password API: `PUT /admin/v1/tenants/{tenantId}/admins/{accountId}/password`
  - Files:
    - `spring-ai-alibaba-admin-server-start/src/main/java/com/alibaba/cloud/ai/studio/admin/controller/TenantController.java`
    - `spring-ai-alibaba-admin-server-runtime/src/main/java/com/alibaba/cloud/ai/studio/runtime/domain/tenant/TenantAdminResetPasswordRequest.java`
- [x] Account service support for tenant-admin lifecycle:
  - Added service methods for tenant-scoped admin status update and password reset.
  - Added tenant/admin target validation and platform-super-admin permission checks.
  - Files:
    - `spring-ai-alibaba-admin-server-core/src/main/java/com/alibaba/cloud/ai/studio/core/base/service/AccountService.java`
    - `spring-ai-alibaba-admin-server-core/src/main/java/com/alibaba/cloud/ai/studio/core/base/service/impl/AccountServiceImpl.java`
- [x] Runtime auth behavior aligned with account status:
  - Disabled accounts are blocked during login and token-auth intercept.
  - Files:
    - `spring-ai-alibaba-admin-server-core/src/main/java/com/alibaba/cloud/ai/studio/core/base/service/impl/AccountServiceImpl.java`
    - `spring-ai-alibaba-admin-server-start/src/main/java/com/alibaba/cloud/ai/studio/admin/builder/interceptor/TokenAuthInterceptor.java`
    - `spring-ai-alibaba-admin-server-start/src/main/java/com/alibaba/cloud/ai/studio/admin/builder/interceptor/AdminTokenAuthInterceptor.java`
- [x] Frontend platform tenant governance page enhancement:
  - Added tenant-admin actions in admin list: `Enable/Disable`, `Reset Password`.
  - Added reset-password modal and API wiring.
  - Files:
    - `frontend/packages/main/src/services/tenant.ts`
    - `frontend/packages/main/src/types/tenant.ts`
    - `frontend/packages/main/src/pages/Admin/Tenant/index.tsx`

Next step:

- [ ] Add tenant-admin delete/recreate policy and operation audit records for platform governance actions.

## 13. 2026-04-11 Update (Step 5)

Completed in this round:

- [x] Tenant-admin delete policy (platform side):
  - Added soft-delete API: `DELETE /admin/v1/tenants/{tenantId}/admins/{accountId}`.
  - Enforced policy: at least one tenant-admin account must remain per tenant (`status != deleted` scope).
  - Files:
    - `spring-ai-alibaba-admin-server-start/src/main/java/com/alibaba/cloud/ai/studio/admin/controller/TenantController.java`
    - `spring-ai-alibaba-admin-server-core/src/main/java/com/alibaba/cloud/ai/studio/core/base/service/AccountService.java`
    - `spring-ai-alibaba-admin-server-core/src/main/java/com/alibaba/cloud/ai/studio/core/base/service/impl/AccountServiceImpl.java`
- [x] Platform governance operation audit logs (first version):
  - Added explicit monitor logs for tenant-admin lifecycle operations:
    `create/enable/disable/resetPassword/delete`.
  - Log service tag: `PlatformTenantAdminGovernance`.
  - File:
    - `spring-ai-alibaba-admin-server-start/src/main/java/com/alibaba/cloud/ai/studio/admin/controller/TenantController.java`
- [x] Frontend tenant-admin list enhanced with delete action:
  - Added `Delete` action button in `/admin/tenants` -> admin list modal.
  - Added API wiring for delete operation.
  - Files:
    - `frontend/packages/main/src/services/tenant.ts`
    - `frontend/packages/main/src/pages/Admin/Tenant/index.tsx`

Next step:

- [ ] Add tenant-admin recreate guidance (bootstrap from platform console) and optional persistent DB audit table for governance operations.

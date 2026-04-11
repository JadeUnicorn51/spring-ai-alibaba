# Spring AI Alibaba Admin 多租户改造说明

## 1. 目标与定位

基于现有 `spring-ai-alibaba-admin`，改造成双层架构：

- 平台层（Platform Governance）：由 `SUPER_ADMIN` 管理租户生命周期与租户管理员。
- 租户层（Tenant Workspace）：每个租户独立使用 Agent 构建能力（应用、知识库、插件、MCP、评测等）。

核心原则：

- 平台治理与租户业务分离。
- 租户间数据强隔离（默认按 `tenant_id` 自动过滤）。
- 平台侧只做治理，不直接读写租户业务数据。

## 2. 当前完成度（结论）

当前状态是“核心流程已打通，进入收口阶段”。

已可用（主流程）：

- `SUPER_ADMIN` 可进入平台治理页面 `/admin/tenants`。
- 可完成租户创建、启停、配额调整。
- 可创建/查看/启停/重置密码/删除租户管理员。
- 可查看平台治理审计日志（含筛选）。
- 前后端已完成 `/admin`（平台）与 `/console`（租户构建）边界分流。

仍需补齐（上线前建议完成）：

- 迁移脚本幂等化与存量数据回填策略固化。
- HTTP 级隔离回归测试体系（当前已补服务层回归测试骨架）。
- 配额在关键写路径的强校验（目前以治理配置为主）。
- 异步链路 `TenantContext` 传播完整性复核。

## 3. 架构与边界

### 3.1 路由边界

- 平台治理 API：`/admin/v1/**`
- 租户构建 API：`/console/v1/**`

拦截器分流：

- `AdminTokenAuthInterceptor` 负责 `/admin/v1/**`
- `TokenAuthInterceptor` 负责 `/console/v1/**`
- `TenantContextCleanupInterceptor` / `TenantContextCleanupFilter` 负责上下文清理

参考：

- `spring-ai-alibaba-admin-server-start/.../interceptor/InterceptorConfig.java`
- `spring-ai-alibaba-admin-server-start/.../interceptor/AdminTokenAuthInterceptor.java`
- `spring-ai-alibaba-admin-server-start/.../interceptor/TokenAuthInterceptor.java`

### 3.2 租户上下文

- 请求进入后写入 `RequestContext` + `TenantContext`。
- `TenantContextHolder` 提供 `tenantId/accountId/workspaceId/platformLevel` 访问。
- 平台级请求通过 `isPlatformLevel()` 跳过租户过滤。

参考：

- `spring-ai-alibaba-admin-server-core/.../context/TenantContextHolder.java`

### 3.3 数据隔离

- MyBatis-Plus `TenantLineInnerInterceptor` 默认按 `tenant_id` 注入过滤。
- `TenantMetaObjectHandler` 在插入时自动填充 `tenantId`。
- 平台级或无上下文请求会跳过租户过滤（用于登录、公用能力、平台治理）。

参考：

- `spring-ai-alibaba-admin-server-core/.../config/MybatisPlusConfig.java`
- `spring-ai-alibaba-admin-server-core/.../config/TenantMetaObjectHandler.java`

## 4. 平台治理能力（后端）

控制器：`/admin/v1/tenants`（`TenantController`）

已提供接口：

- `POST /admin/v1/tenants`
- `GET /admin/v1/tenants`
- `GET /admin/v1/tenants/{tenantId}`
- `PUT /admin/v1/tenants/{tenantId}`
- `PUT /admin/v1/tenants/{tenantId}/quota`
- `POST /admin/v1/tenants/{tenantId}/enable`
- `POST /admin/v1/tenants/{tenantId}/disable`
- `POST /admin/v1/tenants/{tenantId}/admins`
- `GET /admin/v1/tenants/{tenantId}/admins`
- `POST /admin/v1/tenants/{tenantId}/admins/{accountId}/enable`
- `POST /admin/v1/tenants/{tenantId}/admins/{accountId}/disable`
- `PUT /admin/v1/tenants/{tenantId}/admins/{accountId}/password`
- `DELETE /admin/v1/tenants/{tenantId}/admins/{accountId}`
- `GET /admin/v1/tenants/{tenantId}/admin-audits`

补充策略：

- 限制租户管理员越权创建平台管理员。
- 删除租户管理员时强制“至少保留一个可治理账号”。
- 审计日志同时记录 monitor + DB 持久化。

参考：

- `spring-ai-alibaba-admin-server-start/.../controller/TenantController.java`
- `spring-ai-alibaba-admin-server-core/.../service/impl/AccountServiceImpl.java`

## 5. 平台治理能力（前端）

前端入口：

- 路由：`/admin/tenants`
- 页面：`frontend/packages/main/src/pages/Admin/Tenant/index.tsx`

已实现：

- 租户列表、创建、编辑、启停、配额编辑。
- 租户管理员列表与生命周期操作（创建/启停/重置密码/删除）。
- 审计弹窗与筛选（operation/operatorAccountId/targetAccountId）。
- 管理员重建指引（管理员缺失时快速恢复）。
- 平台菜单仅对 `SUPER_ADMIN` 可见，首屏刷新可正确显示。

参考：

- `frontend/packages/main/.umirc.ts`
- `frontend/packages/main/src/layouts/SideMenuLayout.tsx`
- `frontend/packages/main/src/pages/Admin/Tenant/index.tsx`

## 6. 数据与初始化

关键脚本：

- 初始化：`docker/middleware/init/mysql/agentscope-schema.sql`
- 迁移：`docker/middleware/init/mysql/tenant-schema.sql`

已包含：

- `tenant` 主表。
- 业务表 `tenant_id` 扩展（应用、知识库、数据集、插件、MCP 等）。
- `tenant_governance_audit_log` 审计表。
- 平台管理员兼容升级（legacy `admin + tenant_id is null` -> `super_admin`）。

启动兼容：

- `PlatformAdminBootstrapRunner` 会在启动时兜底提升 legacy 平台账号为 `SUPER_ADMIN`。

## 7. 本地联调建议

推荐端口：

- 后端：`28080`（避免与 RocketMQ `18080` 冲突）
- 前端：`8000`（默认）
- RocketMQ Proxy：`18080`

当前本地脚本：

- `run-local.ps1` 已同步后端端口为 `28080`。
- 前端代理默认目标为 `http://localhost:28080`。

## 8. 验收建议（最小集）

平台侧：

- `SUPER_ADMIN` 登录后可见“平台治理/租户管理”。
- 租户创建后可创建租户管理员并可登录租户控制台。
- 租户禁用后，租户管理员 Token/API 访问应被拒绝。

隔离侧：

- 租户 A 账号无法读取/修改租户 B 的资源。
- 平台治理接口对非 `SUPER_ADMIN` 返回权限拒绝。
- 审计日志可按操作人/目标账号快速定位治理动作。

## 9. 已新增的回归测试（本轮）

文件：

- `spring-ai-alibaba-admin-server-core/src/test/java/com/alibaba/cloud/ai/studio/core/base/service/impl/AccountServiceTenantGovernanceTest.java`

覆盖：

- 跨租户治理操作拒绝。
- 非 `SUPER_ADMIN` 治理接口拒绝。
- 删除最后一个租户管理员拒绝。

说明：

- 该仓库当前 surefire 基线对 JUnit5 执行存在配置差异（`mvn test` 可能显示 `Tests run: 0`）。
- 测试源码已可 `test-compile`，建议下一步统一 surefire/JUnit-platform 后接入 CI 执行。

## 10. 下一步建议

1. 完成 P0 的 HTTP 级集成回归（跨租户读写、越权访问、租户禁用后访问行为）。
2. 将迁移脚本升级为幂等版本，并提供存量数据回填脚本与回滚预案。
3. 在关键写路径接入租户配额强校验（用户数、应用数、存储、调用量）。
4. 固化生产运维手册（平台账号恢复、租户紧急禁用、审计排障路径）。

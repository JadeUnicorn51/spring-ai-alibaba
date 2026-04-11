# Multi-Tenant Implementation TODO V2

This TODO tracks only the new V2 architecture scope.

## 1. Principles

1. Reuse runtime capabilities, rebuild IAM and business domains.
2. Three backend microservices.
3. Two frontend portals (`admin-portal`, `tenant-portal`).
4. Tenant isolation by `tenant_id` first, dedicated DB as upgrade path.
5. Nacos 3.1+ dynamic config and A2A registration are mandatory.

## 2. Workstreams

## 2.1 Foundation

- [ ] Confirm final package naming and module naming conventions.
- [ ] Create `services/` root and scaffold:
  - [ ] `platform-service`
  - [ ] `tenant-service`
  - [ ] `agent-runtime-service`
  - [ ] `shared-capabilities`
- [ ] Add common BOM/dependency management for new services.
- [ ] Add unified error model and response contract for new APIs.

## 2.2 Identity and Security

- [ ] Design IAM schema (tenant/org/user/role/permission/user-role/session-policy).
- [ ] Implement unified auth issuing JWT with `tenant_id/user_id/role_codes/scope`.
- [ ] Build gateway token verification and claim forwarding.
- [ ] Implement permission middleware in all three services.
- [ ] Add audit log schema for auth and permission changes.

## 2.3 Platform Service

- [ ] Tenant lifecycle APIs (create/enable/disable/update/status).
- [ ] Package/subscription/quota APIs.
- [ ] Agent product catalog and tenant authorization APIs.
- [ ] Governance audit query APIs.
- [ ] Seed bootstrap for first platform super admin.

## 2.4 Tenant Service

- [ ] Org/member CRUD APIs.
- [ ] Role and permission assignment APIs.
- [ ] Role-agent binding APIs.
- [ ] HITL task center APIs:
  - [ ] task list
  - [ ] claim/reassign
  - [ ] approve/reject
  - [ ] replay/audit

## 2.5 Agent Runtime Service

- [ ] Extract and integrate reusable runtime capabilities from existing modules.
- [ ] Implement runtime auth context abstraction (remove old account-table coupling).
- [ ] Multi-conversation APIs (list/create/switch/archive).
- [ ] Streaming chat APIs and tool-call trace APIs.
- [ ] A2A runtime routing and health policies.

## 2.6 Nacos Dynamic Config

- [ ] Implement config loader for prompts/skills.
- [ ] Implement DataId conventions and version resolution.
- [ ] Implement hot reload with config validation.
- [ ] Add rollback mechanism for invalid config rollout.
- [ ] Add config-change audit persistence.

## 2.7 A2A + Nacos Naming

- [ ] Runtime node self-registration to Nacos 3.1+.
- [ ] Node metadata schema (`agent_code/version/capabilities/tenant_scope`).
- [ ] Runtime discovery client and weighted routing.
- [ ] Health heartbeat and failover strategy.

## 2.8 Structured Data Pipeline

- [ ] Upload API and file metadata model.
- [ ] Parse/OCR pipeline orchestration.
- [ ] AI extraction template model and versioning.
- [ ] Schema validation engine.
- [ ] Structured persistence tables.
- [ ] ES/vector indexing sync.

## 2.9 Frontend

- [ ] Scaffold `frontend/admin-portal` (PC-first).
- [ ] Scaffold `frontend/tenant-portal` (responsive PC/H5).
- [ ] Shared design/i18n/theme package reuse.
- [ ] Tenant portal modules:
  - [ ] tenant management workspace
  - [ ] runtime multi-chat workspace
  - [ ] HITL task workspace
- [ ] Role-based menu and action visibility integration.

## 2.10 WeCom Integration

- [ ] WeCom login/session bridge API.
- [ ] Callback signature verification and event receiver.
- [ ] To-do/message push for HITL tasks.
- [ ] H5 deep-link routing and auth handoff.

## 2.11 Observability and Ops

- [ ] Unified trace/log conventions with `tenant_id`.
- [ ] Metrics for runtime latency, success, retries, queue depth.
- [ ] Alert rules for auth errors, cross-tenant violations, runtime failures.
- [ ] Gray release checklist and rollback runbook.

## 3. Milestones

- [ ] M1: skeleton + auth + tenant context + base schemas.
- [ ] M2: platform core governance + admin portal PC.
- [ ] M3: tenant portal responsive PC/H5 + runtime multi-chat.
- [ ] M4: structured data extraction/query pipeline.
- [ ] M5: HITL + WeCom + production readiness.

## 4. Acceptance Criteria

- [ ] No cross-tenant data read/write in API and data layers.
- [ ] Runtime no longer depends on legacy account tables for auth context.
- [ ] Prompt/skills config hot reload works with rollback.
- [ ] A2A nodes can register/discover via Nacos 3.1+.
- [ ] Tenant portal supports both PC and H5 from one codebase.
- [ ] Admin portal can manage tenant/package/product/authorization end to end.


# Multi-Tenant Agent Cloud Platform Blueprint V2

## 1. Scope

This blueprint defines the target architecture for a production-grade multi-tenant agent cloud platform based on the existing Spring AI Alibaba admin codebase.

Design decisions confirmed in discussion:

1. Monorepo, but new independent modules.
2. Three backend microservices:
   1. `platform-service`
   2. `tenant-service`
   3. `agent-runtime-service`
3. Frontend split into two portals:
   1. `admin-portal` (platform side, PC first)
   2. `tenant-portal` (tenant + runtime combined, responsive PC/H5 in one app)
4. Reuse low-level AI/runtime capabilities, but rebuild IAM and business domain tables.
5. Nacos 3.1+ for dynamic config and service discovery.
6. A2A-enabled agent runtime nodes register to Nacos.

## 2. Architecture Overview

## 2.1 Backend Services

1. `platform-service`
   1. Tenant lifecycle management.
   2. Package/subscription/product catalog management.
   3. Agent product publishing and tenant authorization.
   4. Governance audit APIs.

2. `tenant-service`
   1. Tenant org/user/role/permission management.
   2. Role-agent assignment.
   3. HITL task center and approval APIs.
   4. Tenant business management APIs.

3. `agent-runtime-service`
   1. ReactAgent + Skills execution.
   2. Chat, streaming response, tool calling, MCP.
   3. RAG retrieval and document runtime workflow.
   4. A2A runtime orchestration and agent routing.

4. Shared capability module (library, not an HTTP service)
   1. Doc parsing, chunking, vector retrieval adapters.
   2. Model client abstraction.
   3. Common telemetry/logging/error model.

## 2.2 Frontend Apps

1. `admin-portal` (platform operators)
   1. PC-first management console.
   2. Tenant, package, product, governance views.

2. `tenant-portal` (tenant users)
   1. One responsive app for PC and H5.
   2. Tenant management workspace + runtime chat workspace in the same app.
   3. Menu and action visibility controlled by role permissions.
   4. Multi-conversation UX implemented as independent top-level route modules.

## 2.3 Deployment Boundary

1. `admin-portal` and `tenant-portal` are separate deployables.
2. Backend services are independent deployables.
3. Keep code reuse high, keep runtime and security boundaries strict.

## 3. Reuse vs Rebuild Boundary

## 3.1 Reuse

1. Document parsing and preprocessing.
2. Chunking and retrieval engine.
3. Chat and streaming output.
4. Tool calling and MCP integration.
5. A2A communication runtime.
6. Existing model provider adapters.

## 3.2 Rebuild

1. IAM domain (tenant/org/user/role/permission/session policy).
2. Platform commerce/governance domain (package/subscription/quota/audit).
3. Tenant business domain schema (contract/procurement/on-site/cost/schedule structured records).
4. Unified authentication token pipeline.

## 4. Security and Identity

## 4.1 Identity Model

1. Unified auth issues JWT containing:
   1. `tenant_id`
   2. `user_id`
   3. `role_codes`
   4. `scope` (`platform` or `tenant`)

2. Services trust claims and use strict server-side authorization.

## 4.2 Authorization Layers

1. Menu permission.
2. Action permission.
3. Data permission.

All three are enforced server-side; frontend only improves UX visibility.

## 4.3 Audit

1. Governance audit for platform actions.
2. Security audit for authentication and role changes.
3. Runtime audit for agent/tool/HITL operations.

## 5. Multi-Tenant Data Isolation

## 5.1 Default Strategy

1. Shared database and shared tables with strict `tenant_id`.
2. Every business unique index includes `tenant_id`.
3. No cross-service direct table writes.

## 5.2 Upgrade Strategy

1. Support dedicated database per large tenant via tenant-to-datasource routing.
2. Keep identical schema to reduce migration risk.

## 5.3 Side Systems Isolation

1. Redis key prefix includes `tenant_id`.
2. Elasticsearch index alias/prefix includes `tenant_id`.
3. Object storage path includes `tenant_id`.
4. Logs/traces carry `tenant_id`.

## 6. Dynamic Config (Nacos)

## 6.1 Scope

1. Prompt templates.
2. Skills config.
3. Agent runtime tuning and feature switches.

## 6.2 DataId Convention

1. `agent.prompt.{agentCode}.{version}.yaml`
2. `agent.skills.{agentCode}.{version}.yaml`
3. `agent.tenant.override.{tenantId}.{agentCode}.yaml`

## 6.3 Runtime Behavior

1. Hot reload with validation.
2. Canary rollout and rollback support.
3. Change audit trail required.

## 7. A2A + Nacos Service Discovery

1. Each runtime node registers itself to Nacos Naming (3.1+).
2. Node metadata includes:
   1. `agent_code`
   2. `version`
   3. `capabilities`
   4. `tenant_scope`

3. Runtime router uses health, weight, version policy to pick target nodes.

## 8. Document-to-Structured Data Pipeline

1. User uploads files only; no advanced configuration required by end customers.
2. Pipeline:
   1. Upload
   2. Parse/OCR
   3. AI extraction
   4. Schema validation
   5. Structured persistence
   6. Search index update

3. Storage split:
   1. Structured DB for deterministic queries.
   2. Search/vector index for semantic retrieval.

4. Extraction templates are versioned by business document type.

## 9. HITL and Enterprise WeCom

1. HITL workflow states:
   1. `ai_draft`
   2. `human_review`
   3. `approved` or `rejected`
   4. `effective`

2. Tenant portal H5 is the primary surface for WeCom integration.
3. WeCom support includes login/session bridge, callback validation, and to-do notifications.

## 10. Monorepo Structure Proposal

```text
spring-ai-alibaba-admin/
  docs/
    multi-tenant-blueprint-v2.md
    multi-tenant-todo-v2.md
  services/
    platform-service/
    tenant-service/
    agent-runtime-service/
    shared-capabilities/
  frontend/
    admin-portal/
    tenant-portal/
```

## 11. Delivery Milestones

1. M1: Service skeletons + gateway auth + tenant context.
2. M2: Platform portal (PC) and governance core.
3. M3: Tenant portal (responsive PC/H5) with tenant management + multi-chat runtime workspace.
4. M4: Structured data pipeline and query APIs.
5. M5: HITL + WeCom + production hardening.

## 12. Non-Goals in V2 Baseline

1. Do not migrate old IAM tables into new IAM domain.
2. Do not force per-tenant table topology.
3. Do not merge admin and tenant portals into one deployment unit.


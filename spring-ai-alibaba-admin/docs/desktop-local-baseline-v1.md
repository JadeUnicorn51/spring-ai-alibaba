# Construction Procurement AI Assistant V1 Baseline (Current Stack)

## 1. Goals and Scope

### 1.1 Product Goals
- Serve construction procurement, cost estimation, document, and project roles.
- Keep current technical route: `React frontend + Spring Boot backend`.
- Evolve to desktop packaging (Windows EXE / macOS DMG) and LAN collaboration.

### 1.2 V1 In Scope
- Multi-workspace isolation (data, config, permission).
- Knowledge base management with multiple chunk strategies.
- Multi-role Agent and skill orchestration on current Agent framework.
- Model config center (global defaults + workspace override).
- Local-first storage and enterprise intranet deployment.
- Desktop-local backend and frontend development in a separate module/route boundary.

### 1.3 Out of Scope
- No Vue3 frontend rewrite.
- No FastAPI backend rewrite.
- No forced migration to SQLite/FAISS/Chroma in V1.
- No in-place rewrite of the existing multi-tenant production code path.

## 2. Technical Baseline (Frozen)

### 2.1 Interaction Layer
- Frontend: React (reuse current Umi + Ant Design project).
- I18n: keep existing i18n mechanism (`zh-CN/en-US/ja-JP`, extensible).
- Theme: keep existing dark/light theme capability.

### 2.2 Application Service Layer
- Backend: Spring Boot multi-module architecture (`admin/server/services`).
- API: REST + streaming responses (keep current chat/completions flow).
- Agent: continue with `ReactAgentExecutor` and tool callback pipeline.
- Desktop-local services: add a dedicated module/package and API prefix; current multi-tenant services are reference/reuse sources, not the edit target.

### 2.3 Model Engine Layer
- Chat models: local deployed models + OpenAI-compatible online models.
- Embedding models: global default + knowledge-base level override.
- Unified config fields: `base_url`, `api_key`, `model_name`, `temperature`, `max_tokens`, `top_p`.

### 2.4 Data Layer
- Business data: MySQL (continue current schema and tenant evolution).
- Vector retrieval: Elasticsearch vector capability as mainline.
- File storage: local directories (raw docs, chunks, index cache) by workspace.

## 3. Domain and Isolation Rules

### 3.1 Workspace
- Each workspace has isolated knowledge bases.
- Each workspace has isolated skills.
- Each workspace has isolated Agent role settings.
- Each workspace has isolated model override settings.
- Each workspace has isolated document and index directories.

### 3.2 Knowledge Base
- One knowledge base binds one chunk strategy.
- One knowledge base binds one embedding config (inherits global by default, optional override).
- Required strategy set includes standard chunking.
- Required strategy set includes whole-document no-chunk (contracts).
- Required strategy set includes chapter/hierarchy chunking.
- Required strategy set includes table-structured parsing.
- Required strategy set includes metadata-only.

### 3.3 Skill and Agent Role
- Skills include system skills and workspace custom skills.
- Role binds system prompt + allowed skills + tool permissions.
- License policy controls role/skill visibility and availability.

## 4. Config Precedence (Frozen)

1. System defaults
2. Global admin config
3. Workspace config (overrides global)
4. Knowledge-base config (embedding only)

Rule: smaller scope wins; if unset, fallback to upper scope.

## 5. Commercialization and License (V1)

### 5.1 Minimum License Payload
- validity period
- max workspace count
- max members per workspace
- unlocked Agent roles
- unlocked plugins/advanced skills
- custom-skill allowed flag
- LAN-share allowed flag
- advanced KB strategy allowed flag

### 5.2 Enforcement Rules
- Unlicensed features are disabled in UI with upgrade entry.
- Backend is the final authorization decision point.
- Activation info is encrypted locally and supports offline signature verification.

## 6. LAN Collaboration (V1)

- Host mode: one machine provides service, members join by `IP:PORT`.
- Data is stored centrally on host machine.
- Permission levels: read-only / edit / admin.
- No central cloud dependency required.

## 7. Delivery Milestones

### M1 Foundation
- Workspace isolation, KB isolation, model config center.
- End-to-end config override chain works.

### M2 Knowledge and Agent
- Chunk strategies implemented (including contract whole-document mode).
- Role-skill-tool permission chain closed loop.

### M3 Commercialization
- License model, offline verification, capability gating.
- Frontend/backend authorization consistency checks.

### M4 LAN Collaboration
- Host mode, member join, permission control.
- Document upload and collaboration audit trail.

### M5 Desktop Packaging
- Electron shell integration and EXE/DMG packaging.
- Local startup, logging, upgrade, and diagnostics readiness.

## 8. Definition of Done

- No cross-workspace data access.
- KB chunk strategies are configurable and effective.
- Global-workspace-KB fallback rules are correct.
- Unlicensed actions are blocked by backend and reflected by frontend.
- LAN host mode supports multi-member collaboration.
- Desktop app can start and run stably.

## 9. Implementation Principles

- Build desktop-local capability in a new module/package boundary first; avoid modifying existing multi-tenant code in place.
- Reuse current framework, domain ideas, and utilities through additive adapters or copied desktop-local implementations when needed.
- Deliver critical path first, then advanced capabilities.
- Backend authorization always has final authority.
- Critical actions must be auditable and traceable.
- Existing `/console/v1/*` multi-tenant behavior must remain unchanged while desktop APIs evolve independently.

## 10. Companion Docs

- M1 execution and technical design: `docs/desktop-local-m1-execution-design.md`.

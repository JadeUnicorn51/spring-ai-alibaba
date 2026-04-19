# Desktop Local V1 - M1 Execution Plan and Technical Design

## 1. Scope of This Document

This document is the direct next step after `desktop-local-baseline-v1.md`.
It defines:

- executable M1 tasks by workstream
- concrete backend/frontend changes
- API and data model contracts
- acceptance criteria and test checklist

M1 target: foundation only.
M1 does not deliver full commercialization, full LAN collaboration, or desktop packaging.

Hard boundary: desktop-local development must be isolated in a new module or package tree. Do not modify the existing
multi-tenant production code path unless the change is a purely additive shared abstraction explicitly reviewed for both
paths.

## 2. Current Baseline in Repository

Current implementation already has:

- Workspace CRUD API: `/console/v1/workspaces` (`WorkspaceController`)
- Knowledge base API: `/console/v1/knowledge-bases` (`KnowledgeBaseController`)
- Provider and model APIs: `/console/v1/providers`, `/console/v1/models/*` (`ProviderController`, `ModelController`)
- Request context with workspace and tenant: `TokenAuthInterceptor`
- Workspace persistence via `workspace.config` JSON field
- KB process persistence via `knowledge_base.process_config` JSON field
- KB index persistence via `knowledge_base.index_config` JSON field
- `AccountEntity.defaultWorkspaceId` exists only as a transient field (`@TableField(exist = false)`)
- Current default workspace behavior is `WorkspaceService.getDefaultWorkspace(accountId)`, which returns the first active workspace by ascending primary key

Known gaps for M1 baseline goals:

- no workspace switching API in current console flow
- no persisted default workspace preference; switching cannot be implemented by mutating the current transient account field only
- no unified global model default config API for tenant/admin console
- KB create currently enforces explicit embedding provider/model and cannot inherit defaults
- fallback model selection must remain workspace-scoped because `provider` and `model` are workspace tables
- some legacy controllers still hardcode workspace IDs; M1 must not rely on them for the desktop-local critical path

Desktop-local implementation boundary:

- Do not change current multi-tenant controllers, services, interceptors, or schema behavior in place.
- Build desktop-local APIs under a separate module/package, for example `spring-ai-alibaba-admin-desktop-local` or
  `...admin.desktoplocal`.
- Existing multi-tenant code is reference material only; copy/adapt behavior into the desktop-local module when needed.
- Any shared utility extracted from current code must be additive and must not change existing public behavior.

## 3. M1 Deliverables

M1 is complete when all items below are done:

- workspace isolation remains strict and auditable
- workspace switching is available to end user
- global default model profile is configurable
- workspace model profile override is configurable
- KB embedding can inherit defaults without explicit provider/model input
- effective model config resolution order is deterministic and test-covered

## 4. Workstream Backlog (Execution Ready)

| ID       | Workstream        | Task                                                        | Main Modules                                              | Output                                                          |
| -------- | ----------------- | ----------------------------------------------------------- | --------------------------------------------------------- | --------------------------------------------------------------- |
| M1-WS-01 | Module boundary   | Create desktop-local backend module and package namespace   | new desktop-local module                                | Existing multi-tenant code remains untouched                    |
| M1-WS-02 | Config foundation | Add desktop-local setting storage for local defaults        | desktop-local data/entity/mapper/service                | Persisted `MODEL_DEFAULTS` for local profile                    |
| M1-WS-03 | Workspace         | Persist and switch desktop-local default workspace          | desktop-local account/workspace services                | `PUT /desktop/v1/accounts/profile/default-workspace`           |
| M1-WS-04 | Model defaults    | Add desktop-local global model defaults API                 | desktop-local system service                            | `GET/PUT /desktop/v1/system/model-defaults`                    |
| M1-WS-05 | Model defaults    | Add desktop-local workspace model defaults API              | desktop-local workspace service                         | `GET/PUT /desktop/v1/workspaces/{workspaceId}/model-defaults`  |
| M1-WS-06 | Resolver          | Implement desktop-local effective config resolver           | desktop-local resolver                                  | One deterministic resolution chain                              |
| M1-WS-07 | KB inheritance    | Add desktop-local KB inheritance flow                       | desktop-local KB facade/service                         | KB can omit embedding when inheriting                           |
| M1-WS-08 | Frontend          | Add desktop-local settings UI and workspace switch entry    | desktop-local frontend route/services                   | Usable UI for desktop-local APIs                                |
| M1-WS-09 | Quality           | Add tests and desktop-local migration scripts               | desktop-local module + desktop SQL                      | stable build + regression protection                            |

## 5. Technical Design

### 5.1 New and Updated API Contracts

#### 5.1.1 Workspace switch

- `PUT /desktop/v1/accounts/profile/default-workspace`
- request body:

```json
{
  "workspace_id": "ws_xxx"
}
```

- behavior verifies workspace belongs to current desktop-local account/profile
- behavior updates persisted `account.default_workspace_id`
- behavior invalidates account and default-workspace cache entries
- behavior uses the new workspace in request context on the next authenticated request after cache invalidation
- response body returns the selected workspace ID:

```json
{
  "workspace_id": "ws_xxx"
}
```

Implementation note:

- Current `AccountEntity.defaultWorkspaceId` is not persisted. Desktop-local must model this in its own account/profile entity instead of changing the existing multi-tenant `AccountEntity` in place.
- The desktop-local workspace service must first read the persisted account default. If blank, keep a local fallback to the earliest active workspace for backward compatibility with imported seed data.
- Do not encode workspace switching only in frontend local storage; backend `RequestContext.workspaceId` is the authorization boundary.

#### 5.1.2 Global model defaults (desktop-local profile scope)

- `GET /desktop/v1/system/model-defaults`
- `PUT /desktop/v1/system/model-defaults`
- payload:

```json
{
  "chat": {
    "provider": "openai",
    "model_id": "gpt-4o-mini",
    "parameters": {
      "temperature": 0.2,
      "max_tokens": 4096,
      "top_p": 1.0
    }
  },
  "embedding": {
    "provider": "openai",
    "model_id": "text-embedding-3-large"
  }
}
```

Validation:

- `provider` and `model_id` must reference an enabled provider/model visible in the target workspace when used as an effective value.
- desktop-local profile defaults may be saved before every workspace has a matching model, but the effective resolver must report unresolved values explicitly for such workspaces.
- parameter keys are limited to the common baseline fields: `temperature`, `max_tokens`, `top_p`.

#### 5.1.3 Workspace model defaults

- `GET /desktop/v1/workspaces/{workspaceId}/model-defaults`
- `PUT /desktop/v1/workspaces/{workspaceId}/model-defaults`
- payload format same as desktop-local profile scope
- stored under `desktop_local_workspace.config.model_defaults`
- request must verify `{workspaceId}` belongs to the current desktop-local account/profile before reading or writing `desktop_local_workspace.config`

#### 5.1.4 Effective resolution API

- `GET /desktop/v1/workspaces/{workspaceId}/model-defaults/effective`
- optional query `kb_id`
- returns resolved chat and embedding defaults plus source
- source values: `kb|workspace|local_profile|fallback_enabled_model`
- unresolved values return `source=unresolved`, `provider=null`, `model_id=null`, and a business error code when used by create/index/run operations

### 5.2 Data Model Changes

Desktop-local schema changes must live in desktop-local migration/init scripts first. Do not alter existing multi-tenant
schema files in place for M1. If a later milestone decides to merge schemas, that must be a separate compatibility task.

#### 5.2.1 Desktop-local account default workspace column

```sql
ALTER TABLE desktop_local_account
  ADD COLUMN default_workspace_id VARCHAR(64) DEFAULT NULL COMMENT 'Default workspace id' AFTER account_id,
  ADD KEY idx_default_workspace_id (default_workspace_id);
```

If the desktop-local module reuses imported `account` rows during early prototyping, put this field in a separate
`desktop_local_account_profile` table keyed by `account_id` rather than altering the existing `account` table.

Backfill rule:

```sql
UPDATE desktop_local_account a
JOIN (
  SELECT account_id, MIN(id) AS min_workspace_pk
  FROM desktop_local_workspace
  WHERE status <> 0
  GROUP BY account_id
) w0 ON a.account_id = w0.account_id
JOIN desktop_local_workspace w ON w.id = w0.min_workspace_pk
SET a.default_workspace_id = w.workspace_id
WHERE a.default_workspace_id IS NULL;
```

Entity/service updates:

- Add `DesktopLocalAccountEntity.defaultWorkspaceId` or `DesktopLocalAccountProfileEntity.defaultWorkspaceId`.
- Include `defaultWorkspaceId` in desktop-local account/profile cache payload.
- When a selected workspace is deleted, reject deletion if it is the account default unless another active workspace is selected in the same transaction.

#### 5.2.2 New table: desktop_local_setting

```sql
CREATE TABLE desktop_local_setting (
  id BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL,
  profile_id VARCHAR(64) NOT NULL,
  setting_key VARCHAR(64) NOT NULL,
  setting_value LONGTEXT NOT NULL,
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  creator VARCHAR(64) DEFAULT NULL,
  modifier VARCHAR(64) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_desktop_local_setting (profile_id, setting_key),
  KEY idx_profile_id (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

`setting_key` values in M1:

- `MODEL_DEFAULTS`

Note:

- This keeps desktop-local defaults independent from static `StudioProperties` and existing tenant settings.
- This avoids overloading unrelated business tables.
- Add matching migration under desktop-local init SQL and any desktop-local versioned migration path.

#### 5.2.3 Desktop-local setting value shape

`desktop_local_setting.setting_value` stores the same payload as `ModelDefaultsDTO`:

```json
{
  "chat": {
    "provider": "Tongyi",
    "model_id": "qwen-plus",
    "parameters": {
      "temperature": 0.2,
      "max_tokens": 4096,
      "top_p": 1.0
    }
  },
  "embedding": {
    "provider": "Tongyi",
    "model_id": "text-embedding-v3"
  }
}
```

Profile lookup key:

- single-user local mode: reserved profile key such as `local`
- LAN host mode later: host profile or workspace owner profile key
- do not depend on existing multi-tenant `tenant_id` for M1 desktop-local storage

#### 5.2.4 Workspace config JSON extension

`desktop_local_workspace.config` adds:

```json
{
  "model_defaults": {
    "chat": { "provider": "...", "model_id": "...", "parameters": {} },
    "embedding": { "provider": "...", "model_id": "..." }
  }
}
```

Merge rule:

- preserve unrelated keys in `desktop_local_workspace.config`
- update only `model_defaults`
- parse JSON with existing `JsonUtils`/structured DTOs instead of string splicing

#### 5.2.5 KB index config extension

`desktop_local_knowledge_base.index_config` adds:

```json
{
  "name": "kb_index_name",
  "embedding_provider": "openai",
  "embedding_model": "text-embedding-3-large",
  "inherit_embedding_default": true
}
```

Behavior:

- if `inherit_embedding_default=true`, explicit embedding fields are optional
- if `inherit_embedding_default=false`, explicit embedding fields are required
- if the field is missing on legacy KB rows, treat it as `false` when explicit embedding exists, otherwise treat it as `true` and resolve on next index/retrieval operation
- when a resolved inherited embedding is used for actual indexing, persist `resolved_embedding_provider`, `resolved_embedding_model`, and `resolved_embedding_source` in `index_config` for auditability

### 5.3 Resolution Rule (Core M1 Logic)

Embedding model resolution for KB indexing/retrieval:

1. KB explicit embedding (`inherit_embedding_default=false` and provider/model present)
2. workspace defaults from `desktop_local_workspace.config.model_defaults.embedding`
3. local profile defaults from `desktop_local_setting.MODEL_DEFAULTS.embedding`
4. fallback first enabled embedding model from current workspace's enabled providers/models

Chat default resolution:

1. workspace chat defaults
2. local profile chat defaults
3. fallback first enabled `llm` model from current workspace

Any unresolved state after the fallback step returns explicit business error.

Resolver constraints:

- Resolution input must include desktop-local `profileId`, `accountId`, `workspaceId`, optional `kbId`, and purpose (`chat` or `embedding`).
- Never resolve fallback without a workspace ID.
- Validate provider is enabled and model is enabled at the point of use.
- Keep the resolver side-effect free except for optional audit metadata written by the caller after successful KB create/index.
- Return a typed result with `provider`, `modelId`, `parameters`, `source`, and optional `message`; do not return raw maps to callers.

## 6. Code-Level Change Map

### 6.1 Backend

- create a new desktop-local backend module or package namespace
- add `DesktopLocalSettingEntity/Mapper/Service` in the desktop-local module
- add DTO `ModelDefaultsDTO`
- add DTO `EffectiveModelDefaultsDTO` with per-field source metadata
- add DTO `WorkspaceSwitchRequest`
- add desktop-local account/workspace persistence without changing existing `AccountEntity` or `WorkspaceServiceImpl`
- add desktop-local endpoint for local model defaults get/update
- add desktop-local endpoint for workspace model defaults get/update
- add desktop-local endpoint for default workspace switch
- add resolver service `DesktopLocalModelDefaultsResolver`
- add desktop-local model lookup facade that accepts `(workspaceId, provider, modelId, modelType, requireEnabled)`
- add desktop-local KB facade/service for inherited embedding validation and fallback
- update cache invalidation for account profile, default workspace, and workspace config
- keep existing `/console/v1/*` behavior unchanged

### 6.2 Frontend

- add desktop-local services for workspace list and switch
- add desktop-local services for local/workspace model defaults
- add desktop-local settings route or panel without rewiring current multi-tenant settings flow
- add workspace override panel under desktop-local settings
- add workspace switch entry in desktop-local shell/header or settings
- after switch: clear workspace-scoped query caches/state, reload account profile, and reload current route data

### 6.3 Error Handling

Use existing `BizException`/`ErrorCode` style. Add missing codes only when no current code is precise enough.

Required M1 error cases:

- workspace not found or not owned by current desktop-local account/profile
- selected default workspace was deleted or disabled
- no effective chat model can be resolved
- no effective embedding model can be resolved
- configured provider/model exists but is disabled
- KB explicit embedding missing when inheritance is disabled

## 7. Acceptance Checklist (DoD for M1)

- user can switch default workspace through API and UI
- request context uses switched workspace in subsequent requests
- existing `/console/v1/*` multi-tenant APIs still behave as before
- desktop-local APIs are isolated under their own route/module boundary
- desktop-local global model defaults can be saved and loaded
- workspace override can be saved and loaded
- KB create/update works with inherited embedding defaults
- resolver order works exactly as defined and has automated tests
- no cross-profile or cross-workspace read/write regression in desktop-local APIs
- account cache and workspace cache do not preserve stale default workspace after switch
- legacy accounts get a deterministic default workspace after migration/backfill
- effective model API explains source and unresolved state without silent fallback

## 8. Test Plan

### 8.1 Unit tests

- default workspace: persisted default wins over earliest workspace
- default workspace: missing persisted default falls back to earliest active workspace
- default workspace: switch rejects workspace owned by another desktop-local account/profile
- resolver order: workspace override hit
- resolver order: desktop-local profile fallback hit
- resolver order: enabled-model fallback hit
- resolver validation: disabled provider/model is rejected at use time
- validation: KB with inherit true and empty embedding accepted
- validation: KB with inherit false and empty embedding rejected

### 8.2 Integration tests

- switch workspace then call `/desktop/v1/knowledge-bases`, verify workspace scope changed
- switch workspace then re-fetch `/desktop/v1/accounts/profile`, verify `default_workspace_id` changed
- set desktop-local defaults, create KB with inherit, verify index config resolved
- set workspace override, verify it takes precedence over desktop-local profile defaults
- delete or disable configured model, verify resolver returns explicit unresolved/error state

### 8.3 Frontend smoke

- settings save/load round-trip
- workspace switch refresh and data scope change
- inherited embedding KB create flow does not force provider/model selection
- disabled or unresolved model defaults show actionable validation state

## 9. Suggested Sprint Split

### Sprint A

- M1-WS-01 to M1-WS-04 plus default workspace persistence
- new module boundary + desktop-local migration + local defaults API + account switch API + tests

### Sprint B

- M1-WS-05 to M1-WS-07
- workspace defaults API + KB inheritance + resolver tests

### Sprint C

- M1-WS-08 to M1-WS-09
- frontend integration + end-to-end regression + hardening

## 10. Risks and Mitigation

- Risk: legacy `/api/*` model config paths conflict with `/console/v1/*`.
- Mitigation: keep M1 desktop-local changes under `/desktop/v1/*` and do not modify existing multi-tenant controllers.
- Risk: workspace switch might break long-lived client cache.
- Mitigation: clear cache keys tied to old workspace after switch and force frontend context refresh.
- Risk: KB fallback ambiguity when no enabled embedding model exists.
- Mitigation: explicit error code and UI guidance instead of silent fallback.
- Risk: `AccountEntity.defaultWorkspaceId` is currently transient and can mislead implementation.
- Mitigation: do not change it in place; add the real field to the desktop-local account/profile schema before implementing switch API.
- Risk: desktop-local defaults can reference a model not present in every workspace.
- Mitigation: save desktop-local defaults independently, but validate effective resolution per workspace at use time.
- Risk: workspace config updates overwrite unrelated JSON keys.
- Mitigation: parse, merge only `model_defaults`, and preserve unknown fields.

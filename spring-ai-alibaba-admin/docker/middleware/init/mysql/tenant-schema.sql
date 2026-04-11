/******************************************/
/*   TableName = tenant                     */
/******************************************/
DROP TABLE IF EXISTS tenant;
CREATE TABLE tenant
(
    id              BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    tenant_id       VARCHAR(64)  NOT NULL COMMENT 'Tenant unique identifier',
    name            VARCHAR(255) NOT NULL COMMENT 'Tenant name',
    description     VARCHAR(512)          DEFAULT NULL COMMENT 'Tenant description',
    status          TINYINT(4)  NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-normal',
    max_users       INT(11)     NOT NULL DEFAULT 10 COMMENT 'Max users quota',
    max_apps       INT(11)     NOT NULL DEFAULT 50 COMMENT 'Max applications quota',
    max_workspaces  INT(11)     NOT NULL DEFAULT 5 COMMENT 'Max workspaces quota',
    max_storage_gb  BIGINT(20) NOT NULL DEFAULT 10 COMMENT 'Max storage quota in GB',
    max_api_calls_per_day BIGINT(20)          DEFAULT NULL COMMENT 'Max API calls per day quota',
    expire_date     DATETIME             DEFAULT NULL COMMENT 'Expiration date',
    gmt_create      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    gmt_modified    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    creator         VARCHAR(64)           DEFAULT NULL COMMENT 'Creator uid',
    modifier        VARCHAR(64)           DEFAULT NULL COMMENT 'Modifier uid',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_id (tenant_id),
    KEY idx_status (status),
    KEY idx_name (name)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='Tenant Table';

-- Add tenant_id to existing tables
ALTER TABLE account ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER account_id;
ALTER TABLE account ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE workspace ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER workspace_id;
ALTER TABLE workspace ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE api_key ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER account_id;
ALTER TABLE api_key ADD INDEX idx_tenant_id (tenant_id);

-- Add tenant_id to workspace-scoped tables for data isolation
ALTER TABLE application ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER application_id;
ALTER TABLE application ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE application_version ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE application_version ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE knowledge_base ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE knowledge_base ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE document ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE document ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE dataset ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE dataset ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE dataset_version ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE dataset_version ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE dataset_item ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE dataset_item ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE evaluator ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE evaluator ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE evaluator_version ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE evaluator_version ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE evaluator_template ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE evaluator_template ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE experiment ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE experiment ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE experiment_result ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE experiment_result ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE prompt ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE prompt ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE prompt_version ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE prompt_version ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE prompt_build_template ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE prompt_build_template ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE model_config ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE model_config ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE application_component ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE application_component ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE reference ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE reference ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE mcp_server ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE mcp_server ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE plugin ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE plugin ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE tool ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE tool ADD INDEX idx_tenant_id (tenant_id);

ALTER TABLE agent_schema ADD COLUMN tenant_id VARCHAR(64) COMMENT 'Tenant ID' AFTER id;
ALTER TABLE agent_schema ADD INDEX idx_tenant_id (tenant_id);

-- Provider and Model are preset/shared tables, no tenant_id needed

-- Promote legacy platform admin account to SUPER_ADMIN for tenant-platform access.
UPDATE account
SET type = 'super_admin'
WHERE type = 'admin'
  AND tenant_id IS NULL;

CREATE TABLE IF NOT EXISTS desktop_local_account_profile (
  id BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL,
  profile_id VARCHAR(64) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  default_workspace_id VARCHAR(64) DEFAULT NULL,
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  creator VARCHAR(64) DEFAULT NULL,
  modifier VARCHAR(64) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_desktop_local_profile (profile_id),
  KEY idx_account_id (account_id),
  KEY idx_default_workspace_id (default_workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS desktop_local_setting (
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

CREATE TABLE IF NOT EXISTS desktop_local_workspace (
  id BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL,
  workspace_id VARCHAR(64) NOT NULL,
  profile_id VARCHAR(64) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  status TINYINT(4) NOT NULL DEFAULT 1,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(4096) DEFAULT NULL,
  config LONGTEXT DEFAULT NULL,
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  creator VARCHAR(64) DEFAULT NULL,
  modifier VARCHAR(64) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_desktop_local_workspace_id (workspace_id),
  KEY idx_profile_status_name (profile_id, status, name),
  KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS desktop_local_knowledge_base (
  id BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL,
  kb_id VARCHAR(64) NOT NULL,
  workspace_id VARCHAR(64) NOT NULL,
  profile_id VARCHAR(64) NOT NULL,
  type VARCHAR(64) NOT NULL,
  status TINYINT(4) NOT NULL DEFAULT 1,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(4096) DEFAULT NULL,
  process_config LONGTEXT DEFAULT NULL,
  index_config LONGTEXT DEFAULT NULL,
  search_config LONGTEXT DEFAULT NULL,
  total_docs BIGINT(20) UNSIGNED NOT NULL DEFAULT 0,
  gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  creator VARCHAR(64) DEFAULT NULL,
  modifier VARCHAR(64) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_desktop_local_kb_id (kb_id),
  KEY idx_workspace_status_name (workspace_id, status, name),
  KEY idx_profile_id (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

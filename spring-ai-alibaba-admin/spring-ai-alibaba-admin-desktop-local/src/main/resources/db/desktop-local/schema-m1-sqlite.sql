CREATE TABLE IF NOT EXISTS desktop_local_account_profile (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  profile_id TEXT NOT NULL,
  account_id TEXT NOT NULL,
  default_workspace_id TEXT DEFAULT NULL,
  gmt_create TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creator TEXT DEFAULT NULL,
  modifier TEXT DEFAULT NULL,
  UNIQUE (profile_id)
);

CREATE INDEX IF NOT EXISTS idx_desktop_local_account_profile_account_id
  ON desktop_local_account_profile (account_id);
CREATE INDEX IF NOT EXISTS idx_desktop_local_account_profile_default_workspace_id
  ON desktop_local_account_profile (default_workspace_id);

CREATE TABLE IF NOT EXISTS desktop_local_setting (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  profile_id TEXT NOT NULL,
  setting_key TEXT NOT NULL,
  setting_value TEXT NOT NULL,
  gmt_create TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creator TEXT DEFAULT NULL,
  modifier TEXT DEFAULT NULL,
  UNIQUE (profile_id, setting_key)
);

CREATE INDEX IF NOT EXISTS idx_desktop_local_setting_profile_id
  ON desktop_local_setting (profile_id);

CREATE TABLE IF NOT EXISTS desktop_local_workspace (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  workspace_id TEXT NOT NULL,
  profile_id TEXT NOT NULL,
  account_id TEXT NOT NULL,
  status INTEGER NOT NULL DEFAULT 1,
  name TEXT NOT NULL,
  description TEXT DEFAULT NULL,
  config TEXT DEFAULT NULL,
  gmt_create TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creator TEXT DEFAULT NULL,
  modifier TEXT DEFAULT NULL,
  UNIQUE (workspace_id)
);

CREATE INDEX IF NOT EXISTS idx_desktop_local_workspace_profile_status_name
  ON desktop_local_workspace (profile_id, status, name);
CREATE INDEX IF NOT EXISTS idx_desktop_local_workspace_account_id
  ON desktop_local_workspace (account_id);

CREATE TABLE IF NOT EXISTS desktop_local_knowledge_base (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  kb_id TEXT NOT NULL,
  workspace_id TEXT NOT NULL,
  profile_id TEXT NOT NULL,
  type TEXT NOT NULL,
  status INTEGER NOT NULL DEFAULT 1,
  name TEXT NOT NULL,
  description TEXT DEFAULT NULL,
  process_config TEXT DEFAULT NULL,
  index_config TEXT DEFAULT NULL,
  search_config TEXT DEFAULT NULL,
  total_docs INTEGER NOT NULL DEFAULT 0,
  gmt_create TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creator TEXT DEFAULT NULL,
  modifier TEXT DEFAULT NULL,
  UNIQUE (kb_id)
);

CREATE INDEX IF NOT EXISTS idx_desktop_local_knowledge_base_workspace_status_name
  ON desktop_local_knowledge_base (workspace_id, status, name);
CREATE INDEX IF NOT EXISTS idx_desktop_local_knowledge_base_profile_id
  ON desktop_local_knowledge_base (profile_id);

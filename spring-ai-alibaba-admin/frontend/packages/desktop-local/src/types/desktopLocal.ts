export type DesktopLocalModelDefaultsSource =
  | 'kb'
  | 'workspace'
  | 'local_profile'
  | 'fallback_enabled_model'
  | 'unresolved';

export interface IDesktopLocalChatDefaults {
  provider?: string | null;
  model_id?: string | null;
  parameters?: Record<string, unknown> | null;
}

export interface IDesktopLocalEmbeddingDefaults {
  provider?: string | null;
  model_id?: string | null;
}

export interface IDesktopLocalModelDefaults {
  chat?: IDesktopLocalChatDefaults | null;
  embedding?: IDesktopLocalEmbeddingDefaults | null;
}

export interface IDesktopLocalResolvedModel {
  provider?: string | null;
  model_id?: string | null;
  parameters?: Record<string, unknown> | null;
  source: DesktopLocalModelDefaultsSource;
  message?: string | null;
}

export interface IDesktopLocalEffectiveModelDefaults {
  chat: IDesktopLocalResolvedModel;
  embedding: IDesktopLocalResolvedModel;
}

export interface IDesktopLocalProfile {
  profile_id: string;
  account_id: string;
  default_workspace_id?: string | null;
}

export interface IDesktopLocalWorkspaceSwitchRequest {
  workspace_id: string;
}

export interface IDesktopLocalKnowledgeBase {
  kb_id?: string;
  workspace_id: string;
  type: string;
  name: string;
  description?: string | null;
  process_config?: Record<string, unknown> | null;
  index_config?: Record<string, unknown> | null;
  search_config?: Record<string, unknown> | null;
}

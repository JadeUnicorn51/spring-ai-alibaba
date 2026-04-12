import { IPagingList } from './knowledge';

export interface ISkillItem {
  skill_id: string;
  name: string;
  description?: string;
  instruction?: string;
  enabled?: boolean;
  tool_ids?: string[];
  mcp_server_ids?: string[];
  agent_component_ids?: string[];
  workflow_component_ids?: string[];
  gmt_modified?: string;
  gmt_create?: string;
}

export interface ISkillListParams {
  current: number;
  size: number;
  name?: string;
}

export interface ISkillImportParams {
  directory_path: string;
  overwrite_existing?: boolean;
}

export interface ISkillImportResult {
  directory_path: string;
  overwrite_existing: boolean;
  total_count: number;
  imported_count: number;
  updated_count: number;
  skipped_count: number;
  failed_count: number;
  failed_skills: string[];
}

export type ISkillPagingList = IPagingList<ISkillItem>;

export interface ITenant {
  tenant_id: string;
  name: string;
  description?: string;
  status: number;
  max_users: number;
  max_apps: number;
  max_workspaces: number;
  max_storage_gb: number;
  max_api_calls_per_day: number;
  expire_date?: string;
  gmt_create?: string;
  gmt_modified?: string;
}

export interface ICreateTenantParams {
  name: string;
  description?: string;
  max_users?: number;
  max_apps?: number;
  max_workspaces?: number;
  max_storage_gb?: number;
  max_api_calls_per_day?: number;
  expire_date?: string;
}

export interface IUpdateTenantParams {
  name: string;
  description?: string;
}

export interface IUpdateTenantQuotaParams {
  max_users: number;
  max_apps: number;
  max_workspaces: number;
  max_storage_gb: number;
  max_api_calls_per_day: number;
}

export interface IGetTenantListParams {
  current?: number;
  size?: number;
  name?: string;
}

export interface ICreateTenantAdminParams {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  mobile?: string;
}

export interface IGetTenantAdminListParams {
  current?: number;
  size?: number;
  name?: string;
}

export interface ITenantAdmin {
  account_id: string;
  tenant_id: string;
  username: string;
  nickname?: string;
  email?: string;
  mobile?: string;
  type: string;
  status?: string;
  gmt_create?: string;
}

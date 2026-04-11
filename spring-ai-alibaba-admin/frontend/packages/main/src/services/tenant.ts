import { request } from '@/request';
import { IPagingList } from '@/types/account';
import { IApiResponse } from '@/types/common';
import type {
  ICreateTenantAdminParams,
  ICreateTenantParams,
  IGetTenantAdminAuditListParams,
  IGetTenantAdminListParams,
  IGetTenantListParams,
  IResetTenantAdminPasswordParams,
  ITenantAdminAudit,
  ITenantAdmin,
  ITenant,
  IUpdateTenantParams,
  IUpdateTenantQuotaParams,
} from '@/types/tenant';

export async function getTenantList(
  params?: IGetTenantListParams,
): Promise<IApiResponse<IPagingList<ITenant>>> {
  const response = await request({
    url: '/admin/v1/tenants',
    method: 'GET',
    params,
  });
  return response.data as IApiResponse<IPagingList<ITenant>>;
}

export async function createTenant(
  params: ICreateTenantParams,
): Promise<IApiResponse<string>> {
  const response = await request({
    url: '/admin/v1/tenants',
    method: 'POST',
    data: params,
  });
  return response.data as IApiResponse<string>;
}

export async function updateTenant(
  tenantId: string,
  params: IUpdateTenantParams,
): Promise<IApiResponse<void>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}`,
    method: 'PUT',
    data: params,
  });
  return response.data as IApiResponse<void>;
}

export async function updateTenantQuota(
  tenantId: string,
  params: IUpdateTenantQuotaParams,
): Promise<IApiResponse<void>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/quota`,
    method: 'PUT',
    data: params,
  });
  return response.data as IApiResponse<void>;
}

export async function enableTenant(tenantId: string): Promise<IApiResponse<void>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/enable`,
    method: 'POST',
  });
  return response.data as IApiResponse<void>;
}

export async function disableTenant(
  tenantId: string,
): Promise<IApiResponse<void>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/disable`,
    method: 'POST',
  });
  return response.data as IApiResponse<void>;
}

export async function createTenantAdmin(
  tenantId: string,
  params: ICreateTenantAdminParams,
): Promise<IApiResponse<string>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/admins`,
    method: 'POST',
    data: params,
  });
  return response.data as IApiResponse<string>;
}

export async function getTenantAdminList(
  tenantId: string,
  params?: IGetTenantAdminListParams,
): Promise<IApiResponse<IPagingList<ITenantAdmin>>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/admins`,
    method: 'GET',
    params,
  });
  return response.data as IApiResponse<IPagingList<ITenantAdmin>>;
}

export async function enableTenantAdmin(
  tenantId: string,
  accountId: string,
): Promise<IApiResponse<void>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/admins/${accountId}/enable`,
    method: 'POST',
  });
  return response.data as IApiResponse<void>;
}

export async function disableTenantAdmin(
  tenantId: string,
  accountId: string,
): Promise<IApiResponse<void>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/admins/${accountId}/disable`,
    method: 'POST',
  });
  return response.data as IApiResponse<void>;
}

export async function resetTenantAdminPassword(
  tenantId: string,
  accountId: string,
  params: IResetTenantAdminPasswordParams,
): Promise<IApiResponse<void>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/admins/${accountId}/password`,
    method: 'PUT',
    data: params,
  });
  return response.data as IApiResponse<void>;
}

export async function deleteTenantAdmin(
  tenantId: string,
  accountId: string,
): Promise<IApiResponse<void>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/admins/${accountId}`,
    method: 'DELETE',
  });
  return response.data as IApiResponse<void>;
}

export async function getTenantAdminAuditList(
  tenantId: string,
  params?: IGetTenantAdminAuditListParams,
): Promise<IApiResponse<IPagingList<ITenantAdminAudit>>> {
  const response = await request({
    url: `/admin/v1/tenants/${tenantId}/admin-audits`,
    method: 'GET',
    params,
  });
  return response.data as IApiResponse<IPagingList<ITenantAdminAudit>>;
}

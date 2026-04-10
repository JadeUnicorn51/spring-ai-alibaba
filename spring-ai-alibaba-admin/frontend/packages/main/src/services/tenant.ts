import { request } from '@/request';
import { IPagingList } from '@/types/account';
import { IApiResponse } from '@/types/common';
import type {
  ICreateTenantParams,
  IGetTenantListParams,
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

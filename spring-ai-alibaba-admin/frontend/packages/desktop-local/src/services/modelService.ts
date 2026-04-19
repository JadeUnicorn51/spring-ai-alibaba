import { fetch } from '@/request';
import type { IApiResponse } from '@/types/common';
import type { IModelSelectorItem } from '@/types/modelService';

export async function getModelSelector(
  modelType: string,
): Promise<IApiResponse<IModelSelectorItem[]>> {
  const response = await fetch<IApiResponse<IModelSelectorItem[]>>({
    url: `/console/v1/models/${modelType}/selector`,
    method: 'GET',
  });
  return response.data;
}

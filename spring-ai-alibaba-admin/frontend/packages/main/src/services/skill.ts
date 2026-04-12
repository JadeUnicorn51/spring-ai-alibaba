import { request } from '@/request';
import { IApiResponse } from '@/types/common';
import {
  ISkillImportParams,
  ISkillImportResult,
  ISkillItem,
  ISkillListParams,
  ISkillPagingList,
} from '@/types/skill';

export const getSkillList = (params: ISkillListParams) => {
  return request({
    url: '/console/v1/skills',
    method: 'GET',
    params,
  }).then((res) => res.data.data as ISkillPagingList);
};

export const createSkill = (params: Omit<ISkillItem, 'skill_id'>) => {
  return request({
    url: '/console/v1/skills',
    method: 'POST',
    data: params,
  }).then((res) => res.data.data as string);
};

export const importSkill = (params: ISkillImportParams) => {
  return request({
    url: '/console/v1/skills/import',
    method: 'POST',
    data: params,
  }).then((res) => res.data.data as ISkillImportResult);
};

export const updateSkill = (params: ISkillItem) => {
  const { skill_id, ...rest } = params;
  return request({
    url: `/console/v1/skills/${skill_id}`,
    method: 'PUT',
    data: rest,
  }).then((res) => res.data as IApiResponse<null>);
};

export const deleteSkill = (skill_id: string) => {
  return request({
    url: `/console/v1/skills/${skill_id}`,
    method: 'DELETE',
  }).then((res) => res.data as IApiResponse<null>);
};

export const getSkillsByCodes = (skill_ids: string[]) => {
  return request({
    url: '/console/v1/skills/query-by-codes',
    method: 'POST',
    data: { skill_ids },
  }).then((res) => res.data.data as ISkillItem[]);
};

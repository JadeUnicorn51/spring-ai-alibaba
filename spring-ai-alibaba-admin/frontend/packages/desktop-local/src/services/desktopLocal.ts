import { fetch } from '@/request';
import type { IApiResponse } from '@/types/common';
import type {
  IDesktopLocalEffectiveModelDefaults,
  IDesktopLocalKnowledgeBase,
  IDesktopLocalModelDefaults,
  IDesktopLocalProfile,
  IDesktopLocalWorkspaceSwitchRequest,
} from '@/types/desktopLocal';

const API_PREFIX = '/desktop/v1';

export async function getDesktopLocalProfile(): Promise<IDesktopLocalProfile> {
  const response = await fetch<IApiResponse<IDesktopLocalProfile>>({
    url: `${API_PREFIX}/accounts/profile`,
    method: 'GET',
  });
  return response.data.data;
}

export async function switchDesktopLocalDefaultWorkspace(
  params: IDesktopLocalWorkspaceSwitchRequest,
): Promise<IDesktopLocalWorkspaceSwitchRequest> {
  const response = await fetch<IApiResponse<IDesktopLocalWorkspaceSwitchRequest>>({
    url: `${API_PREFIX}/accounts/profile/default-workspace`,
    method: 'PUT',
    data: params,
  });
  return response.data.data;
}

export async function getDesktopLocalModelDefaults(): Promise<IDesktopLocalModelDefaults> {
  const response = await fetch<IApiResponse<IDesktopLocalModelDefaults>>({
    url: `${API_PREFIX}/system/model-defaults`,
    method: 'GET',
  });
  return response.data.data;
}

export async function saveDesktopLocalModelDefaults(
  params: IDesktopLocalModelDefaults,
): Promise<IDesktopLocalModelDefaults> {
  const response = await fetch<IApiResponse<IDesktopLocalModelDefaults>>({
    url: `${API_PREFIX}/system/model-defaults`,
    method: 'PUT',
    data: params,
  });
  return response.data.data;
}

export async function getDesktopLocalWorkspaceModelDefaults(
  workspaceId: string,
): Promise<IDesktopLocalModelDefaults> {
  const response = await fetch<IApiResponse<IDesktopLocalModelDefaults>>({
    url: `${API_PREFIX}/workspaces/${workspaceId}/model-defaults`,
    method: 'GET',
  });
  return response.data.data;
}

export async function saveDesktopLocalWorkspaceModelDefaults(
  workspaceId: string,
  params: IDesktopLocalModelDefaults,
): Promise<IDesktopLocalModelDefaults> {
  const response = await fetch<IApiResponse<IDesktopLocalModelDefaults>>({
    url: `${API_PREFIX}/workspaces/${workspaceId}/model-defaults`,
    method: 'PUT',
    data: params,
  });
  return response.data.data;
}

export async function getDesktopLocalEffectiveModelDefaults(
  workspaceId: string,
  kbId?: string,
): Promise<IDesktopLocalEffectiveModelDefaults> {
  const response = await fetch<IApiResponse<IDesktopLocalEffectiveModelDefaults>>({
    url: `${API_PREFIX}/workspaces/${workspaceId}/model-defaults/effective`,
    method: 'GET',
    params: {
      kb_id: kbId,
    },
  });
  return response.data.data;
}

export async function createDesktopLocalKnowledgeBase(
  params: IDesktopLocalKnowledgeBase,
): Promise<string> {
  const response = await fetch<IApiResponse<string>>({
    url: `${API_PREFIX}/knowledge-bases`,
    method: 'POST',
    data: params,
  });
  return response.data.data;
}

export async function updateDesktopLocalKnowledgeBase(
  kbId: string,
  params: IDesktopLocalKnowledgeBase,
): Promise<string> {
  const response = await fetch<IApiResponse<string>>({
    url: `${API_PREFIX}/knowledge-bases/${kbId}`,
    method: 'PUT',
    data: params,
  });
  return response.data.data;
}

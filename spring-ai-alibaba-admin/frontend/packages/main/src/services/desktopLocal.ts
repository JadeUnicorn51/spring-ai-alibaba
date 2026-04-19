import { request } from '@/request';
import type {
  IDesktopLocalEffectiveModelDefaults,
  IDesktopLocalKnowledgeBase,
  IDesktopLocalModelDefaults,
  IDesktopLocalProfile,
  IDesktopLocalWorkspaceSwitchRequest,
} from '@/types/desktopLocal';

const DESKTOP_LOCAL_API_PREFIX = '/desktop/v1';

export async function getDesktopLocalProfile(): Promise<IDesktopLocalProfile> {
  const response = await request({
    url: `${DESKTOP_LOCAL_API_PREFIX}/accounts/profile`,
    method: 'GET',
  });
  return response.data.data;
}

export async function switchDesktopLocalDefaultWorkspace(
  params: IDesktopLocalWorkspaceSwitchRequest,
): Promise<IDesktopLocalWorkspaceSwitchRequest> {
  const response = await request({
    url: `${DESKTOP_LOCAL_API_PREFIX}/accounts/profile/default-workspace`,
    method: 'PUT',
    data: params,
  });
  return response.data.data;
}

export async function getDesktopLocalModelDefaults(): Promise<IDesktopLocalModelDefaults> {
  const response = await request({
    url: `${DESKTOP_LOCAL_API_PREFIX}/system/model-defaults`,
    method: 'GET',
  });
  return response.data.data;
}

export async function saveDesktopLocalModelDefaults(
  params: IDesktopLocalModelDefaults,
): Promise<IDesktopLocalModelDefaults> {
  const response = await request({
    url: `${DESKTOP_LOCAL_API_PREFIX}/system/model-defaults`,
    method: 'PUT',
    data: params,
  });
  return response.data.data;
}

export async function getDesktopLocalWorkspaceModelDefaults(
  workspaceId: string,
): Promise<IDesktopLocalModelDefaults> {
  const response = await request({
    url: `${DESKTOP_LOCAL_API_PREFIX}/workspaces/${workspaceId}/model-defaults`,
    method: 'GET',
  });
  return response.data.data;
}

export async function saveDesktopLocalWorkspaceModelDefaults(
  workspaceId: string,
  params: IDesktopLocalModelDefaults,
): Promise<IDesktopLocalModelDefaults> {
  const response = await request({
    url: `${DESKTOP_LOCAL_API_PREFIX}/workspaces/${workspaceId}/model-defaults`,
    method: 'PUT',
    data: params,
  });
  return response.data.data;
}

export async function getDesktopLocalEffectiveModelDefaults(
  workspaceId: string,
  kbId?: string,
): Promise<IDesktopLocalEffectiveModelDefaults> {
  const response = await request({
    url: `${DESKTOP_LOCAL_API_PREFIX}/workspaces/${workspaceId}/model-defaults/effective`,
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
  const response = await request({
    url: `${DESKTOP_LOCAL_API_PREFIX}/knowledge-bases`,
    method: 'POST',
    data: params,
  });
  return response.data.data;
}

export async function updateDesktopLocalKnowledgeBase(
  kbId: string,
  params: IDesktopLocalKnowledgeBase,
): Promise<string> {
  const response = await request({
    url: `${DESKTOP_LOCAL_API_PREFIX}/knowledge-bases/${kbId}`,
    method: 'PUT',
    data: params,
  });
  return response.data.data;
}

import InnerLayout from '@/components/InnerLayout';
import {
  getDesktopLocalEffectiveModelDefaults,
  getDesktopLocalModelDefaults,
  getDesktopLocalProfile,
  getDesktopLocalWorkspaceModelDefaults,
  saveDesktopLocalModelDefaults,
  saveDesktopLocalWorkspaceModelDefaults,
  switchDesktopLocalDefaultWorkspace,
} from '@/services/desktopLocal';
import { getModelSelector } from '@/services/modelService';
import type {
  IDesktopLocalEffectiveModelDefaults,
  IDesktopLocalModelDefaults,
  IDesktopLocalProfile,
  IDesktopLocalResolvedModel,
} from '@/types/desktopLocal';
import type { IModelSelectorItem } from '@/types/modelService';
import { Button, message } from '@spark-ai/design';
import {
  Alert,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Tag,
} from 'antd';
import { useEffect, useMemo, useState } from 'react';
import styles from './index.module.less';

type ModelOption = {
  label: string;
  value: string;
  provider: string;
  modelId: string;
};

type ModelDefaultsForm = {
  chat?: {
    value?: string;
    temperature?: number;
    max_tokens?: number;
    top_p?: number;
  };
  embedding?: {
    value?: string;
  };
};

const toModelValue = (provider?: string | null, modelId?: string | null) => {
  if (!provider || !modelId) {
    return undefined;
  }
  return `${provider}::${modelId}`;
};

const fromModelValue = (value?: string) => {
  if (!value) {
    return {};
  }
  const [provider, ...rest] = value.split('::');
  return {
    provider,
    model_id: rest.join('::'),
  };
};

const toFormValues = (
  defaults?: IDesktopLocalModelDefaults,
): ModelDefaultsForm => {
  return {
    chat: {
      value: toModelValue(defaults?.chat?.provider, defaults?.chat?.model_id),
      temperature: defaults?.chat?.parameters?.temperature as number | undefined,
      max_tokens: defaults?.chat?.parameters?.max_tokens as number | undefined,
      top_p: defaults?.chat?.parameters?.top_p as number | undefined,
    },
    embedding: {
      value: toModelValue(
        defaults?.embedding?.provider,
        defaults?.embedding?.model_id,
      ),
    },
  };
};

const toDefaultsPayload = (
  values: ModelDefaultsForm,
): IDesktopLocalModelDefaults => {
  const chat = fromModelValue(values.chat?.value);
  const embedding = fromModelValue(values.embedding?.value);
  const parameters = {
    temperature: values.chat?.temperature,
    max_tokens: values.chat?.max_tokens,
    top_p: values.chat?.top_p,
  };

  return {
    chat:
      chat.provider && chat.model_id
        ? {
            provider: chat.provider,
            model_id: chat.model_id,
            parameters: Object.fromEntries(
              Object.entries(parameters).filter(
                ([, value]) => value !== undefined && value !== null,
              ),
            ),
          }
        : null,
    embedding:
      embedding.provider && embedding.model_id
        ? {
            provider: embedding.provider,
            model_id: embedding.model_id,
          }
        : null,
  };
};

const buildModelOptions = (selector: IModelSelectorItem[]): ModelOption[] => {
  return selector.flatMap((group) =>
    (group.models || []).map((model) => ({
      label: `${group.provider.name || group.provider.provider} / ${
        model.name || model.model_id
      }`,
      value: `${group.provider.provider}::${model.model_id}`,
      provider: group.provider.provider,
      modelId: model.model_id,
    })),
  );
};

const modelSelectFilter = (input: string, option?: ModelOption) => {
  return `${option?.label || ''} ${option?.provider || ''} ${
    option?.modelId || ''
  }`
    .toLowerCase()
    .includes(input.toLowerCase());
};

const renderResolvedModel = (
  title: string,
  model?: IDesktopLocalResolvedModel,
) => {
  const isUnresolved = model?.source === 'unresolved';
  return (
    <div className={styles.effectiveItem}>
      <div className={styles.effectiveTitle}>
        <span>{title}</span>
        <Tag color={isUnresolved ? 'red' : 'green'}>
          {model?.source || 'unresolved'}
        </Tag>
      </div>
      <div className={styles.modelLine}>
        {model?.provider && model?.model_id
          ? `${model.provider} / ${model.model_id}`
          : model?.message || '未解析到可用模型'}
      </div>
    </div>
  );
};

const DesktopLocalSetting = () => {
  const [profile, setProfile] = useState<IDesktopLocalProfile>();
  const [loading, setLoading] = useState(false);
  const [workspaceId, setWorkspaceId] = useState('');
  const [effective, setEffective] =
    useState<IDesktopLocalEffectiveModelDefaults>();
  const [chatOptions, setChatOptions] = useState<ModelOption[]>([]);
  const [embeddingOptions, setEmbeddingOptions] = useState<ModelOption[]>([]);
  const [profileForm] = Form.useForm<ModelDefaultsForm>();
  const [workspaceForm] = Form.useForm<ModelDefaultsForm>();

  const currentWorkspaceId = useMemo(
    () => workspaceId || profile?.default_workspace_id || '',
    [profile?.default_workspace_id, workspaceId],
  );

  useEffect(() => {
    init();
  }, []);

  const init = async () => {
    setLoading(true);
    try {
      const [profileData, profileDefaults, chatSelector, embeddingSelector] =
        await Promise.all([
          getDesktopLocalProfile(),
          getDesktopLocalModelDefaults(),
          getModelSelector('llm'),
          getModelSelector('embedding'),
        ]);
      setProfile(profileData);
      setWorkspaceId(profileData.default_workspace_id || '');
      profileForm.setFieldsValue(toFormValues(profileDefaults));
      setChatOptions(buildModelOptions(chatSelector.data || []));
      setEmbeddingOptions(buildModelOptions(embeddingSelector.data || []));
      if (profileData.default_workspace_id) {
        await loadWorkspaceDefaults(profileData.default_workspace_id);
        await loadEffectiveDefaults(profileData.default_workspace_id);
      }
    } finally {
      setLoading(false);
    }
  };

  const loadWorkspaceDefaults = async (
    targetWorkspaceId = currentWorkspaceId,
  ) => {
    if (!targetWorkspaceId) {
      workspaceForm.resetFields();
      setEffective(undefined);
      return;
    }
    const defaults = await getDesktopLocalWorkspaceModelDefaults(
      targetWorkspaceId,
    );
    workspaceForm.setFieldsValue(toFormValues(defaults));
  };

  const loadEffectiveDefaults = async (
    targetWorkspaceId = currentWorkspaceId,
  ) => {
    if (!targetWorkspaceId) {
      setEffective(undefined);
      return;
    }
    const result = await getDesktopLocalEffectiveModelDefaults(targetWorkspaceId);
    setEffective(result);
  };

  const handleSaveProfileDefaults = async () => {
    const values = await profileForm.validateFields();
    const result = await saveDesktopLocalModelDefaults(toDefaultsPayload(values));
    profileForm.setFieldsValue(toFormValues(result));
    message.success('全局模型默认值已保存');
    await loadEffectiveDefaults();
  };

  const handleLoadWorkspace = async () => {
    if (!currentWorkspaceId) {
      message.warning('请输入工作区 ID');
      return;
    }
    await loadWorkspaceDefaults(currentWorkspaceId);
    await loadEffectiveDefaults(currentWorkspaceId);
  };

  const handleSaveWorkspaceDefaults = async () => {
    if (!currentWorkspaceId) {
      message.warning('请输入工作区 ID');
      return;
    }
    const values = await workspaceForm.validateFields();
    const result = await saveDesktopLocalWorkspaceModelDefaults(
      currentWorkspaceId,
      toDefaultsPayload(values),
    );
    workspaceForm.setFieldsValue(toFormValues(result));
    message.success('工作区模型默认值已保存');
    await loadEffectiveDefaults(currentWorkspaceId);
  };

  const handleSwitchDefaultWorkspace = async () => {
    if (!currentWorkspaceId) {
      message.warning('请输入工作区 ID');
      return;
    }
    const result = await switchDesktopLocalDefaultWorkspace({
      workspace_id: currentWorkspaceId,
    });
    setProfile((previous) =>
      previous
        ? { ...previous, default_workspace_id: result.workspace_id }
        : previous,
    );
    message.success('默认工作区已切换');
    await loadWorkspaceDefaults(result.workspace_id);
    await loadEffectiveDefaults(result.workspace_id);
  };

  const renderModelDefaultsForm = (
    form: ReturnType<typeof Form.useForm<ModelDefaultsForm>>[0],
  ) => (
    <Form form={form} layout="vertical">
      <div className={styles.formGrid}>
        <Form.Item name={['chat', 'value']} label="默认对话模型">
          <Select
            allowClear
            showSearch
            filterOption={modelSelectFilter}
            options={chatOptions}
            placeholder="选择 LLM 模型"
          />
        </Form.Item>
        <Form.Item name={['embedding', 'value']} label="默认 Embedding 模型">
          <Select
            allowClear
            showSearch
            filterOption={modelSelectFilter}
            options={embeddingOptions}
            placeholder="选择 Embedding 模型"
          />
        </Form.Item>
      </div>
      <div className={styles.paramsGrid}>
        <Form.Item name={['chat', 'temperature']} label="Temperature">
          <InputNumber
            min={0}
            max={2}
            step={0.1}
            precision={2}
            style={{ width: '100%' }}
          />
        </Form.Item>
        <Form.Item name={['chat', 'max_tokens']} label="Max tokens">
          <InputNumber
            min={1}
            max={1000000}
            precision={0}
            style={{ width: '100%' }}
          />
        </Form.Item>
        <Form.Item name={['chat', 'top_p']} label="Top P">
          <InputNumber
            min={0}
            max={1}
            step={0.1}
            precision={2}
            style={{ width: '100%' }}
          />
        </Form.Item>
      </div>
    </Form>
  );

  return (
    <InnerLayout
      loading={loading}
      breadcrumbLinks={[{ title: '首页', path: '/' }, { title: '桌面端本地设置' }]}
    >
      <div className={styles.container}>
        <Alert
          type="info"
          showIcon
          message="桌面端本地配置"
          description="该页面只调用 /desktop/v1/* 接口，用于配置本地 profile 与工作区模型默认值。"
        />

        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.title}>本地 Profile</h2>
              <p className={styles.description}>当前桌面端账号和默认工作区状态。</p>
            </div>
          </div>
          <Descriptions column={3} size="small">
            <Descriptions.Item label="Profile">
              {profile?.profile_id || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="Account">
              {profile?.account_id || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="默认工作区">
              {profile?.default_workspace_id || '-'}
            </Descriptions.Item>
          </Descriptions>
        </div>

        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.title}>全局模型默认值</h2>
              <p className={styles.description}>
                作为桌面端本地 profile 的模型默认配置。
              </p>
            </div>
            <Button type="primary" onClick={handleSaveProfileDefaults}>
              保存全局默认值
            </Button>
          </div>
          {renderModelDefaultsForm(profileForm)}
        </div>

        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.title}>工作区覆盖</h2>
              <p className={styles.description}>工作区配置会优先于全局默认值。</p>
            </div>
            <Space>
              <Button onClick={handleLoadWorkspace}>加载工作区</Button>
              <Button onClick={handleSwitchDefaultWorkspace}>设为默认工作区</Button>
              <Button type="primary" onClick={handleSaveWorkspaceDefaults}>
                保存工作区覆盖
              </Button>
            </Space>
          </div>
          <div className={styles.workspaceBar}>
            <Input
              value={currentWorkspaceId}
              placeholder="输入 workspaceId"
              onChange={(event) => setWorkspaceId(event.target.value)}
            />
          </div>
          {renderModelDefaultsForm(workspaceForm)}
          <div className={styles.effective}>
            {renderResolvedModel('Effective Chat', effective?.chat)}
            {renderResolvedModel('Effective Embedding', effective?.embedding)}
          </div>
        </div>
      </div>
    </InnerLayout>
  );
};

export default DesktopLocalSetting;

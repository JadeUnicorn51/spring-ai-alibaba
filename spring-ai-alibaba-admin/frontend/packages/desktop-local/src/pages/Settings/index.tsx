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
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Spin,
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
): ModelDefaultsForm => ({
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
});

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

const buildModelOptions = (selector: IModelSelectorItem[]): ModelOption[] =>
  selector.flatMap((group) =>
    (group.models || []).map((model) => ({
      label: `${group.provider.name || group.provider.provider} / ${
        model.name || model.model_id
      }`,
      value: `${group.provider.provider}::${model.model_id}`,
      provider: group.provider.provider,
      modelId: model.model_id,
    })),
  );

const modelSelectFilter = (input: string, option?: ModelOption) =>
  `${option?.label || ''} ${option?.provider || ''} ${option?.modelId || ''}`
    .toLowerCase()
    .includes(input.toLowerCase());

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
          : model?.message || 'No available model resolved'}
      </div>
    </div>
  );
};

const SettingsContent = () => {
  const { message } = App.useApp();
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
    message.success('Global defaults saved');
    await loadEffectiveDefaults();
  };

  const handleLoadWorkspace = async () => {
    if (!currentWorkspaceId) {
      message.warning('Enter a workspace ID');
      return;
    }
    await loadWorkspaceDefaults(currentWorkspaceId);
    await loadEffectiveDefaults(currentWorkspaceId);
  };

  const handleSaveWorkspaceDefaults = async () => {
    if (!currentWorkspaceId) {
      message.warning('Enter a workspace ID');
      return;
    }
    const values = await workspaceForm.validateFields();
    const result = await saveDesktopLocalWorkspaceModelDefaults(
      currentWorkspaceId,
      toDefaultsPayload(values),
    );
    workspaceForm.setFieldsValue(toFormValues(result));
    message.success('Workspace defaults saved');
    await loadEffectiveDefaults(currentWorkspaceId);
  };

  const handleSwitchDefaultWorkspace = async () => {
    if (!currentWorkspaceId) {
      message.warning('Enter a workspace ID');
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
    message.success('Default workspace switched');
    await loadWorkspaceDefaults(result.workspace_id);
    await loadEffectiveDefaults(result.workspace_id);
  };

  const renderModelDefaultsForm = (
    form: ReturnType<typeof Form.useForm<ModelDefaultsForm>>[0],
  ) => (
    <Form form={form} layout="vertical">
      <div className={styles.formGrid}>
        <Form.Item name={['chat', 'value']} label="Default chat model">
          <Select
            allowClear
            showSearch
            filterOption={modelSelectFilter}
            options={chatOptions}
            placeholder="Select LLM model"
          />
        </Form.Item>
        <Form.Item
          name={['embedding', 'value']}
          label="Default embedding model"
        >
          <Select
            allowClear
            showSearch
            filterOption={modelSelectFilter}
            options={embeddingOptions}
            placeholder="Select embedding model"
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
    <div className={styles.shell}>
      <header className={styles.header}>
        <div className={styles.brand}>Spring AI Alibaba Desktop</div>
      </header>
      <Spin spinning={loading}>
        <main className={styles.content}>
          <Alert
            type="info"
            showIcon
            message="Desktop local settings"
            description="This module calls /desktop/v1/* APIs and is isolated from the main console frontend package."
          />

          <Card className={styles.section}>
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.title}>Local profile</h2>
                <p className={styles.description}>
                  Current desktop account and default workspace state.
                </p>
              </div>
            </div>
            <Descriptions column={3} size="small">
              <Descriptions.Item label="Profile">
                {profile?.profile_id || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Account">
                {profile?.account_id || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Default workspace">
                {profile?.default_workspace_id || '-'}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card className={styles.section}>
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.title}>Global model defaults</h2>
                <p className={styles.description}>
                  Profile-level defaults used when a workspace has no override.
                </p>
              </div>
              <Button type="primary" onClick={handleSaveProfileDefaults}>
                Save global defaults
              </Button>
            </div>
            {renderModelDefaultsForm(profileForm)}
          </Card>

          <Card className={styles.section}>
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.title}>Workspace override</h2>
                <p className={styles.description}>
                  Workspace defaults take precedence over profile defaults.
                </p>
              </div>
              <Space>
                <Button onClick={handleLoadWorkspace}>Load workspace</Button>
                <Button onClick={handleSwitchDefaultWorkspace}>
                  Set default
                </Button>
                <Button type="primary" onClick={handleSaveWorkspaceDefaults}>
                  Save override
                </Button>
              </Space>
            </div>
            <div className={styles.workspaceBar}>
              <Input
                value={currentWorkspaceId}
                placeholder="workspaceId"
                onChange={(event) => setWorkspaceId(event.target.value)}
              />
            </div>
            {renderModelDefaultsForm(workspaceForm)}
            <div className={styles.effective}>
              {renderResolvedModel('Effective chat', effective?.chat)}
              {renderResolvedModel('Effective embedding', effective?.embedding)}
            </div>
          </Card>
        </main>
      </Spin>
    </div>
  );
};

export default function SettingsPage() {
  return (
    <App>
      <SettingsContent />
    </App>
  );
}

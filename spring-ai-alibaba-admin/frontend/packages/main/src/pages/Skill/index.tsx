import InnerLayout from '@/components/InnerLayout';
import {
  getAppComponentList,
  IAppType,
} from '@/services/appComponent';
import { listMcpServers } from '@/services/mcp';
import { getPluginToolList, listPlugin } from '@/services/plugin';
import {
  createSkill,
  deleteSkill,
  getSkillList,
  importSkill,
  updateSkill,
} from '@/services/skill';
import { ISkillItem } from '@/types/skill';
import { Button, Form, Input, message, Modal, Popconfirm, Select, Space, Switch, Table } from 'antd';
import { useEffect, useState } from 'react';

const DEFAULT_PAGE_SIZE = 20;

type SkillPayload = Omit<ISkillItem, 'skill_id'>;

const emptySkillPayload = (): SkillPayload => ({
  name: '',
  description: '',
  instruction: '',
  enabled: true,
  tool_ids: [],
  mcp_server_ids: [],
  agent_component_ids: [],
  workflow_component_ids: [],
});

export default function SkillPage() {
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [resourceLoading, setResourceLoading] = useState(false);
  const [resourceLoaded, setResourceLoaded] = useState(false);
  const [list, setList] = useState<ISkillItem[]>([]);
  const [toolOptions, setToolOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [mcpOptions, setMcpOptions] = useState<Array<{ label: string; value: string }>>([]);
  const [agentComponentOptions, setAgentComponentOptions] = useState<
    Array<{ label: string; value: string }>
  >([]);
  const [workflowComponentOptions, setWorkflowComponentOptions] = useState<
    Array<{ label: string; value: string }>
  >([]);
  const [current, setCurrent] = useState(1);
  const [size, setSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [keyword, setKeyword] = useState('');

  const [editorOpen, setEditorOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ISkillItem | null>(null);
  const [importOpen, setImportOpen] = useState(false);
  const [importDirectory, setImportDirectory] = useState('');
  const [importOverwrite, setImportOverwrite] = useState(true);

  const [form] = Form.useForm<SkillPayload>();

  const fetchPluginTools = async () => {
    const pluginPageSize = 100;
    let pluginCurrent = 1;
    let plugins: Array<{ plugin_id?: string }> = [];
    while (true) {
      const pluginRes = await listPlugin({
        current: pluginCurrent,
        size: pluginPageSize,
      });
      const pluginPage = pluginRes.data;
      const records = pluginPage?.records || [];
      plugins = plugins.concat(records);
      if ((pluginPage?.total || 0) <= pluginCurrent * pluginPageSize) {
        break;
      }
      pluginCurrent += 1;
    }

    const toolPages = await Promise.all(
      plugins
        .map((plugin) => plugin.plugin_id)
        .filter((id): id is string => !!id)
        .map((pluginId) =>
          getPluginToolList(pluginId)
            .then((res) => res.data.records || [])
            .catch(() => []),
        ),
    );

    const toolMap = new Map<string, string>();
    toolPages.flat().forEach((tool) => {
      if (!tool?.tool_id) {
        return;
      }
      const toolName = tool.name || tool.tool_id;
      toolMap.set(tool.tool_id, `${toolName} (${tool.tool_id})`);
    });
    return Array.from(toolMap.entries()).map(([value, label]) => ({ value, label }));
  };

  const fetchMcpServers = async () => {
    const pageSize = 100;
    let currentPage = 1;
    const optionMap = new Map<string, string>();
    while (true) {
      const res = await listMcpServers({
        current: currentPage,
        size: pageSize,
        need_tools: false,
      });
      const page = res.data;
      const records = page?.records || [];
      records.forEach((item) => {
        if (!item?.server_code) {
          return;
        }
        const name = item.name || item.server_code;
        optionMap.set(item.server_code, `${name} (${item.server_code})`);
      });
      if ((page?.total || 0) <= currentPage * pageSize) {
        break;
      }
      currentPage += 1;
    }
    return Array.from(optionMap.entries()).map(([value, label]) => ({ value, label }));
  };

  const fetchAppComponents = async (type: IAppType) => {
    const pageSize = 100;
    let currentPage = 1;
    const optionMap = new Map<string, string>();
    while (true) {
      const page = await getAppComponentList({
        current: currentPage,
        size: pageSize,
        type,
      });
      const records = page?.records || [];
      records.forEach((item) => {
        if (!item?.code) {
          return;
        }
        const name = item.name || item.code;
        optionMap.set(item.code, `${name} (${item.code})`);
      });
      if ((page?.total || 0) <= currentPage * pageSize) {
        break;
      }
      currentPage += 1;
    }
    return Array.from(optionMap.entries()).map(([value, label]) => ({ value, label }));
  };

  const ensureResourceOptions = async () => {
    if (resourceLoading || resourceLoaded) {
      return;
    }
    setResourceLoading(true);
    try {
      const [tools, mcps, agentComponents, workflowComponents] = await Promise.all([
        fetchPluginTools(),
        fetchMcpServers(),
        fetchAppComponents(IAppType.AGENT),
        fetchAppComponents(IAppType.WORKFLOW),
      ]);
      setToolOptions(tools);
      setMcpOptions(mcps);
      setAgentComponentOptions(agentComponents);
      setWorkflowComponentOptions(workflowComponents);
      setResourceLoaded(true);
    } catch {
      message.warning('部分绑定资源加载失败，可稍后重试');
    } finally {
      setResourceLoading(false);
    }
  };

  const fetchList = async (nextCurrent = current, nextSize = size, nextKeyword = keyword) => {
    setLoading(true);
    try {
      const res = await getSkillList({
        current: nextCurrent,
        size: nextSize,
        name: nextKeyword || undefined,
      });
      setList(res.records || []);
      setTotal(res.total || 0);
      setCurrent(res.current || nextCurrent);
      setSize(res.size || nextSize);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchList(1, size, '');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const openCreate = () => {
    ensureResourceOptions();
    setEditingRecord(null);
    form.setFieldsValue(emptySkillPayload());
    setEditorOpen(true);
  };

  const openEdit = (record: ISkillItem) => {
    ensureResourceOptions();
    setEditingRecord(record);
    form.setFieldsValue({
      name: record.name,
      description: record.description,
      instruction: record.instruction,
      enabled: record.enabled ?? true,
      tool_ids: record.tool_ids || [],
      mcp_server_ids: record.mcp_server_ids || [],
      agent_component_ids: record.agent_component_ids || [],
      workflow_component_ids: record.workflow_component_ids || [],
    });
    setEditorOpen(true);
  };

  const onSubmitEditor = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingRecord?.skill_id) {
        await updateSkill({
          ...values,
          skill_id: editingRecord.skill_id,
        });
        message.success('技能已更新');
      } else {
        await createSkill(values);
        message.success('技能已创建');
      }
      setEditorOpen(false);
      await fetchList(editingRecord ? current : 1, size, keyword);
    } finally {
      setSubmitting(false);
    }
  };

  const onDelete = async (skillId: string) => {
    await deleteSkill(skillId);
    message.success('技能已删除');
    const nextCurrent = list.length === 1 && current > 1 ? current - 1 : current;
    await fetchList(nextCurrent, size, keyword);
  };

  const onImport = async () => {
    const directoryPath = importDirectory.trim();
    if (!directoryPath) {
      message.error('请输入技能目录路径');
      return;
    }

    setSubmitting(true);
    try {
      const result = await importSkill({
        directory_path: directoryPath,
        overwrite_existing: importOverwrite,
      });
      message.success(
        `导入完成：新增 ${result.imported_count}，更新 ${result.updated_count}，跳过 ${result.skipped_count}，失败 ${result.failed_count}`,
      );
      setImportOpen(false);
      setImportDirectory('');
      setImportOverwrite(true);
      await fetchList(1, size, keyword);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: '首页',
          path: '/',
        },
        {
          title: '技能',
        },
      ]}
      left={total}
      right={
        <Space>
          <Button onClick={() => setImportOpen(true)}>导入技能</Button>
          <Button type="primary" onClick={openCreate}>
            新建技能
          </Button>
        </Space>
      }
    >
      <div style={{ padding: 20 }}>
        <Space style={{ marginBottom: 16 }}>
          <Input
            placeholder="输入技能名称"
            value={keyword}
            allowClear
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={() => fetchList(1, size, keyword)}
            style={{ width: 260 }}
          />
          <Button type="primary" onClick={() => fetchList(1, size, keyword)}>
            搜索
          </Button>
        </Space>

        <Table<ISkillItem>
          rowKey="skill_id"
          loading={loading}
          dataSource={list}
          pagination={{
            current,
            pageSize: size,
            total,
            showSizeChanger: true,
            onChange: (nextCurrent, nextSize) => {
              fetchList(nextCurrent, nextSize, keyword);
            },
          }}
          columns={[
            {
              title: '名称',
              dataIndex: 'name',
              width: 220,
            },
            {
              title: '启用',
              dataIndex: 'enabled',
              width: 90,
              render: (enabled: boolean) => <Switch size="small" checked={enabled !== false} disabled />,
            },
            {
              title: '描述',
              dataIndex: 'description',
              ellipsis: true,
            },
            {
              title: '绑定',
              width: 260,
              render: (_, record) => (
                <span>
                  Tool {record.tool_ids?.length || 0} / MCP {record.mcp_server_ids?.length || 0} / Agent{' '}
                  {record.agent_component_ids?.length || 0} / Workflow {record.workflow_component_ids?.length || 0}
                </span>
              ),
            },
            {
              title: '操作',
              width: 140,
              render: (_, record) => (
                <Space size={4}>
                  <Button type="link" onClick={() => openEdit(record)}>
                    编辑
                  </Button>
                  <Popconfirm title="确认删除该技能？" onConfirm={() => onDelete(record.skill_id)}>
                    <Button type="link" danger>
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      </div>

      <Modal
        title={editingRecord ? '编辑技能' : '新建技能'}
        open={editorOpen}
        onCancel={() => setEditorOpen(false)}
        onOk={onSubmitEditor}
        confirmLoading={submitting}
        width={760}
      >
        <Form form={form} layout="vertical" initialValues={emptySkillPayload()}>
          <Form.Item label="技能名称" name="name" rules={[{ required: true, message: '请输入技能名称' }]}>
            <Input maxLength={128} />
          </Form.Item>
          <Form.Item label="技能描述" name="description">
            <Input.TextArea rows={2} maxLength={1024} />
          </Form.Item>
          <Form.Item label="指令补丁" name="instruction">
            <Input.TextArea rows={4} maxLength={4000} />
          </Form.Item>
          <Form.Item label="启用" name="enabled" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item label="绑定 Tool" name="tool_ids">
            <Select
              mode="multiple"
              showSearch
              optionFilterProp="label"
              placeholder="选择工具"
              options={toolOptions}
              loading={resourceLoading}
            />
          </Form.Item>
          <Form.Item label="绑定 MCP 服务" name="mcp_server_ids">
            <Select
              mode="multiple"
              showSearch
              optionFilterProp="label"
              placeholder="选择 MCP 服务"
              options={mcpOptions}
              loading={resourceLoading}
            />
          </Form.Item>
          <Form.Item label="绑定 Agent 组件" name="agent_component_ids">
            <Select
              mode="multiple"
              showSearch
              optionFilterProp="label"
              placeholder="选择 Agent 组件"
              options={agentComponentOptions}
              loading={resourceLoading}
            />
          </Form.Item>
          <Form.Item label="绑定 Workflow 组件" name="workflow_component_ids">
            <Select
              mode="multiple"
              showSearch
              optionFilterProp="label"
              placeholder="选择 Workflow 组件"
              options={workflowComponentOptions}
              loading={resourceLoading}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="导入技能（目录）"
        open={importOpen}
        onCancel={() => setImportOpen(false)}
        onOk={onImport}
        confirmLoading={submitting}
        width={680}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Input
            value={importDirectory}
            onChange={(e) => setImportDirectory(e.target.value)}
            placeholder="输入服务端可访问的技能目录路径，例如 D:\\skills"
          />
          <Space>
            <span>覆盖同名技能</span>
            <Switch checked={importOverwrite} onChange={setImportOverwrite} />
          </Space>
          <div style={{ color: 'var(--ag-ant-color-text-secondary)' }}>
            目录结构要求：每个技能一个子目录，且子目录下存在 SKILL.md（frontmatter 包含 name/description）。
          </div>
        </Space>
      </Modal>
    </InnerLayout>
  );
}

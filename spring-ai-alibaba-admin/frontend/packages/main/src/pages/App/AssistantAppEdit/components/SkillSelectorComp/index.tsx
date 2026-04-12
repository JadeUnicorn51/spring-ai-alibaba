import { ToolService } from '@/services/tool';
import { AssistantAppContext } from '@/pages/App/AssistantAppEdit/AssistantAppContext';
import { PluginTool } from '@/types/plugin';
import { IAgentSkill, AgentExecutionMode } from '@/types/appManage';
import { ITool } from '@/types/tool';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Flex, Input, InputNumber, Select, Space, Switch, Typography } from 'antd';
import { useContext, useEffect, useMemo, useState } from 'react';

const DEFAULT_MAX_ITERATIONS = 6;

const EMPTY_SKILL = (): IAgentSkill => ({
  id: `skill_${Date.now()}`,
  name: '',
  description: '',
  instruction: '',
  enabled: true,
  tool_ids: [],
  tools: [],
});

const mapToolToPluginTool = (tool: ITool): PluginTool => ({
  tool_id: tool.toolId,
  name: tool.name,
  description: tool.description,
});

const normalizeToolIds = (skill: IAgentSkill): string[] => {
  if (skill.tool_ids?.length) {
    return skill.tool_ids.filter((id): id is string => !!id);
  }
  if (!skill.tools?.length) {
    return [];
  }
  return skill.tools
    .map((tool) => tool.tool_id)
    .filter((id): id is string => !!id);
};

export default function SkillSelectorComp() {
  const { appState, onAppConfigChange } = useContext(AssistantAppContext);
  const [toolOptions, setToolOptions] = useState<ITool[]>([]);

  const config = appState.appBasicConfig?.config;
  const executionMode: AgentExecutionMode =
    config?.execution_mode || 'basic_tool_loop';
  const skills = config?.skills || [];

  useEffect(() => {
    ToolService.getTools()
      .then((res) => {
        setToolOptions(Array.isArray(res) ? res : []);
      })
      .catch(() => {
        setToolOptions([]);
      });
  }, []);

  const toolOptionItems = useMemo(
    () =>
      toolOptions
        .filter((tool) => !!tool.toolId)
        .map((tool) => ({
          label: tool.name,
          value: tool.toolId as string,
        })),
    [toolOptions],
  );

  const onChangeExecutionMode = (mode: AgentExecutionMode) => {
    onAppConfigChange({
      execution_mode: mode,
      max_iterations:
        mode === 'react_agent'
          ? config?.max_iterations || DEFAULT_MAX_ITERATIONS
          : undefined,
    });
  };

  const onChangeMaxIterations = (value: number | null) => {
    onAppConfigChange({
      max_iterations: value || DEFAULT_MAX_ITERATIONS,
    });
  };

  const updateSkills = (nextSkills: IAgentSkill[]) => {
    onAppConfigChange({ skills: nextSkills });
  };

  const onAddSkill = () => {
    updateSkills([...skills, EMPTY_SKILL()]);
  };

  const onDeleteSkill = (index: number) => {
    const nextSkills = skills.filter((_, i) => i !== index);
    updateSkills(nextSkills);
  };

  const onChangeSkill = (index: number, patch: Partial<IAgentSkill>) => {
    const nextSkills = skills.map((skill, i) => (i === index ? { ...skill, ...patch } : skill));
    updateSkills(nextSkills);
  };

  const onChangeSkillTools = (index: number, toolIds: string[]) => {
    const selectedTools = toolOptions
      .filter((tool) => tool.toolId && toolIds.includes(tool.toolId))
      .map(mapToolToPluginTool);
    onChangeSkill(index, {
      tool_ids: toolIds,
      tools: selectedTools,
    });
  };

  return (
    <Flex vertical gap={10} className="mb-[20px]">
      <Flex justify="space-between" align="center">
        <Typography.Text strong>Agent Engine</Typography.Text>
        <Select
          style={{ width: 220 }}
          value={executionMode}
          onChange={onChangeExecutionMode}
          options={[
            { label: 'Basic Tool Loop', value: 'basic_tool_loop' },
            { label: 'ReactAgent + Skills', value: 'react_agent' },
          ]}
        />
      </Flex>

      {executionMode === 'react_agent' && (
        <Flex align="center" justify="space-between">
          <Typography.Text type="secondary">Max Iterations</Typography.Text>
          <InputNumber
            min={1}
            max={20}
            value={config?.max_iterations || DEFAULT_MAX_ITERATIONS}
            onChange={onChangeMaxIterations}
          />
        </Flex>
      )}

      <Flex justify="space-between" align="center">
        <Typography.Text strong>Skills</Typography.Text>
        <Button size="small" icon={<PlusOutlined />} onClick={onAddSkill}>
          Add Skill
        </Button>
      </Flex>

      {skills.map((skill, index) => (
        <Space
          key={skill.id || `skill_${index}`}
          direction="vertical"
          size={8}
          style={{
            width: '100%',
            border: '1px solid var(--ag-ant-color-border-secondary)',
            borderRadius: 8,
            padding: 10,
          }}
        >
          <Flex justify="space-between" align="center" gap={8}>
            <Input
              placeholder="Skill name"
              value={skill.name}
              onChange={(e) => onChangeSkill(index, { name: e.target.value })}
            />
            <Switch
              checked={skill.enabled !== false}
              onChange={(checked) => onChangeSkill(index, { enabled: checked })}
            />
            <Button
              danger
              type="text"
              icon={<DeleteOutlined />}
              onClick={() => onDeleteSkill(index)}
            />
          </Flex>

          <Input
            placeholder="Skill description (optional)"
            value={skill.description}
            onChange={(e) =>
              onChangeSkill(index, { description: e.target.value })
            }
          />

          <Select
            mode="multiple"
            allowClear
            showSearch
            placeholder="Select atomic tools"
            options={toolOptionItems}
            value={normalizeToolIds(skill)}
            onChange={(value) => onChangeSkillTools(index, value)}
          />

          <Input.TextArea
            rows={3}
            placeholder="Skill instruction patch (optional)"
            value={skill.instruction}
            onChange={(e) =>
              onChangeSkill(index, { instruction: e.target.value })
            }
          />
        </Space>
      ))}
    </Flex>
  );
}

import { AssistantAppContext } from '@/pages/App/AssistantAppEdit/AssistantAppContext';
import { getSkillList, getSkillsByCodes } from '@/services/skill';
import { AgentExecutionMode, IAgentSkill } from '@/types/appManage';
import { ISkillItem } from '@/types/skill';
import { Button, Flex, InputNumber, Select, Space, Typography } from 'antd';
import { useContext, useEffect, useMemo, useState } from 'react';

const DEFAULT_MAX_ITERATIONS = 6;
const SKILL_FETCH_SIZE = 200;

const toAgentSkill = (skill: ISkillItem): IAgentSkill => ({
  id: skill.skill_id,
  name: skill.name,
  description: skill.description,
  instruction: skill.instruction,
  enabled: skill.enabled,
  tool_ids: skill.tool_ids || [],
  mcp_server_ids: skill.mcp_server_ids || [],
  agent_component_ids: skill.agent_component_ids || [],
  workflow_component_ids: skill.workflow_component_ids || [],
});

const extractSkillIds = (config: {
  skill_ids?: string[];
  skills?: IAgentSkill[];
}): string[] => {
  if (config.skill_ids?.length) {
    return config.skill_ids.filter((id): id is string => !!id);
  }
  if (!config.skills?.length) {
    return [];
  }
  return config.skills
    .map((skill) => skill.id)
    .filter((id): id is string => !!id);
};

export default function SkillSelectorComp() {
  const { appState, onAppConfigChange } = useContext(AssistantAppContext);
  const [skills, setSkills] = useState<ISkillItem[]>([]);

  const config = appState.appBasicConfig?.config;
  const executionMode: AgentExecutionMode =
    config?.execution_mode || 'basic_tool_loop';
  const selectedSkillIds = useMemo(
    () =>
      extractSkillIds({
        skill_ids: config?.skill_ids,
        skills: config?.skills,
      }),
    [config?.skill_ids, config?.skills],
  );

  useEffect(() => {
    getSkillList({ current: 1, size: SKILL_FETCH_SIZE })
      .then((res) => {
        setSkills(Array.isArray(res.records) ? res.records : []);
      })
      .catch(() => {
        setSkills([]);
      });
  }, []);

  useEffect(() => {
    const missingIds = selectedSkillIds.filter(
      (id) => !skills.some((item) => item.skill_id === id),
    );
    if (!missingIds.length) {
      return;
    }
    getSkillsByCodes(missingIds)
      .then((res) => {
        const appended = Array.isArray(res) ? res : [];
        if (!appended.length) {
          return;
        }
        setSkills((prev) => {
          const merged = [...prev];
          for (const item of appended) {
            if (!merged.find((skill) => skill.skill_id === item.skill_id)) {
              merged.push(item);
            }
          }
          return merged;
        });
      })
      .catch(() => {
        // ignore fetch miss
      });
  }, [selectedSkillIds, skills]);

  const skillOptions = useMemo(
    () =>
      skills
        .filter((skill) => !!skill.skill_id)
        .map((skill) => ({
          label: skill.name,
          value: skill.skill_id,
        })),
    [skills],
  );

  const selectedSkills = useMemo(
    () =>
      selectedSkillIds
        .map((id) => skills.find((skill) => skill.skill_id === id))
        .filter((item): item is ISkillItem => !!item),
    [selectedSkillIds, skills],
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

  const onChangeSkillIds = (ids: string[]) => {
    const nextSkills = ids
      .map((id) => skills.find((item) => item.skill_id === id))
      .filter((item): item is ISkillItem => !!item)
      .map(toAgentSkill);

    onAppConfigChange({
      skill_ids: ids,
      skills: nextSkills,
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
        <Typography.Text strong>Bound Skills</Typography.Text>
        <Button size="small" onClick={() => window.open('/skill', '_blank')}>
          管理技能
        </Button>
      </Flex>

      <Select
        mode="multiple"
        allowClear
        showSearch
        placeholder="选择要绑定的技能"
        options={skillOptions}
        value={selectedSkillIds}
        onChange={onChangeSkillIds}
      />

      {!!selectedSkills.length && (
        <Space direction="vertical" size={6} style={{ width: '100%' }}>
          {selectedSkills.map((skill) => (
            <div
              key={skill.skill_id}
              style={{
                border: '1px solid var(--ag-ant-color-border-secondary)',
                borderRadius: 8,
                padding: '8px 10px',
              }}
            >
              <Typography.Text strong>{skill.name}</Typography.Text>
              <Typography.Paragraph
                type="secondary"
                style={{ margin: '4px 0 0 0' }}
                ellipsis={{ rows: 2 }}
              >
                {skill.description || '暂无描述'}
              </Typography.Paragraph>
            </div>
          ))}
        </Space>
      )}
    </Flex>
  );
}

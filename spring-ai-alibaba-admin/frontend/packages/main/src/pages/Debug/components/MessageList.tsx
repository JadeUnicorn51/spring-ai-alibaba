import React from 'react';
import { Collapse, Tag, Typography } from 'antd';
import {
  UserOutlined,
  RobotOutlined,
  ToolOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { Message } from '../contexts/ChatContext';
import { useConfigContext } from '../contexts/ConfigContext';
import styles from '../index.module.less';

const { Panel } = Collapse;
const { Text } = Typography;

interface MessageListProps {
  messages: Message[];
}

const MessageList: React.FC<MessageListProps> = ({ messages }) => {
  const { config } = useConfigContext();

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const renderAttachments = (attachments?: File[]) => {
    if (!attachments || attachments.length === 0) return null;

    return (
      <div className={styles.fileAttachment}>
        {attachments.map((file, index) => (
          <Tag key={index} icon={<ToolOutlined />}>
            {file.name}
          </Tag>
        ))}
      </div>
    );
  };

  const renderToolCalls = (toolCalls?: any[]) => {
    if (!config.showToolCalls || !toolCalls || toolCalls.length === 0)
      return null;

    const normalizePayload = (value: any) => {
      if (value === null || value === undefined) return '';
      if (typeof value === 'string') return value;
      try {
        return JSON.stringify(value, null, 2);
      } catch (error) {
        return String(value);
      }
    };

    return (
      <div className={styles.messageToolCalls}>
        <Collapse size="small" ghost>
          <Panel header="馃敡 宸ュ叿璋冪敤璇︽儏" key="1">
            {toolCalls.map((call, index) => {
              const fn = call?.function || call;
              const name = fn?.name || call?.name || '-';
              const args = fn?.arguments ?? call?.arguments;
              const output = fn?.output ?? call?.output ?? call?.result;
              const type = call?.type;
              const inputValue = normalizePayload(args);
              const outputValue = normalizePayload(output);

              return (
                <div key={index} style={{ marginBottom: 8 }}>
                  <Text strong>
                    {type ? `类型: ${type} / ` : ''}函数: {name}
                  </Text>
                  <pre style={{ margin: '4px 0', fontSize: 11 }}>
                    参数: {inputValue || '无'}
                  </pre>
                  <pre style={{ margin: '4px 0', fontSize: 11 }}>
                    结果: {outputValue || '无'}
                  </pre>
                </div>
              );
            })}
          </Panel>
        </Collapse>
      </div>
    );
  };

  const renderError = (error?: string) => {
    if (!error) return null;

    return (
      <div className={styles.messageError}>
        <ExclamationCircleOutlined style={{ marginRight: 4 }} />
        {error}
      </div>
    );
  };

  return (
    <div>
      {messages.map((message) => (
        <div
          key={message.id}
          className={`${styles.message} ${message.type === 'user' ? styles.user : ''}`}
        >
          <div className={styles.messageAvatar}>
            {message.type === 'user' ? <UserOutlined /> : <RobotOutlined />}
          </div>

          <div className={styles.messageContent}>
            <div className={styles.messageBubble}>
              <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {message.content}
              </div>
              {renderAttachments(message.attachments)}
            </div>

            <div className={styles.messageTime}>
              {formatTime(message.timestamp)}
            </div>

            {renderToolCalls(message.toolCalls)}
            {renderError(message.error)}
          </div>
        </div>
      ))}
    </div>
  );
};

export default MessageList;

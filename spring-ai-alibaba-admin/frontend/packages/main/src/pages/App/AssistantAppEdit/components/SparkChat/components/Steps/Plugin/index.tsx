import $i18n from '@/i18n';
import { Markdown } from '@spark-ai/chat';
import {
  CodeBlock,
  CollapsePanel,
  IconFont,
  message,
  parseJsonSafely,
} from '@spark-ai/design';
import styles from './index.module.less';

export default (props: {
  params: {
    name?: string; // tool name
    arguments?: string; // input parameters
    output?: string; // output parameters
  };
}) => {
  const { params } = props;

  const normalizePayload = (value?: unknown): string => {
    if (value === null || value === undefined) return '';
    if (typeof value === 'string') return value;
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  };

  const inputValue = normalizePayload(params.arguments as unknown);
  const outputValue = normalizePayload(params.output as unknown);

  const inputIsJson = inputValue
    ? parseJsonSafely(inputValue, false, true) !== null
    : false;
  const outputIsJson = outputValue
    ? parseJsonSafely(outputValue, false, true) !== null
    : false;

  const handleCopy = async (value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      message.success(
        $i18n.get({
          id: 'main.components.SparkChat.components.Steps.Plugin.index.copySuccess',
          dm: '澶嶅埗鎴愬姛',
        }),
      );
    } catch (error) {
      message.error(
        $i18n.get({
          id: 'main.components.SparkChat.components.Steps.Plugin.index.copyFailed',
          dm: '澶嶅埗澶辫触',
        }),
      );
    }
  };

  return (
    <div className={styles.container}>
      {params.name ? (
        <div className={styles.title}>
          {$i18n.get(
            {
              id: 'main.components.SparkChat.components.Steps.Plugin.index.toolName',
              dm: '工具：{var1}',
            },
            { var1: params.name },
          )}
        </div>
      ) : null}
      {params.arguments !== undefined && (
        <CollapsePanel
          title={$i18n.get({
            id: 'main.components.SparkChat.components.Steps.Plugin.index.inputParameters',
            dm: '杈撳叆鍙傛暟',
          })}
          collapsedHeight={64}
          expandedHeight={200}
          expandOnPanelClick={true}
          extra={
            <IconFont
              type="spark-copy-line"
              style={{ fontSize: '16px' }}
              onClick={() => handleCopy(inputValue)}
            />
          }
        >
          {inputValue ? (
            inputIsJson ? (
              <CodeBlock language={'json'} value={inputValue} />
            ) : (
              <div className="p-[12px]">
                <Markdown content={inputValue} baseFontSize={12} />
              </div>
            )
          ) : (
            <div className="p-[12px]">
              {$i18n.get({
                id: 'main.components.SparkChat.components.Steps.Plugin.index.emptyInput',
                dm: '无',
              })}
            </div>
          )}
        </CollapsePanel>
      )}
      {params.output !== undefined && (
        <CollapsePanel
          title={$i18n.get({
            id: 'main.components.SparkChat.components.Steps.Plugin.index.outputParameters',
            dm: '杈撳嚭鍙傛暟',
          })}
          collapsedHeight={64}
          expandedHeight={200}
          expandOnPanelClick={true}
          extra={
            <IconFont
              type="spark-copy-line"
              style={{ fontSize: '16px' }}
              onClick={() => handleCopy(outputValue)}
            />
          }
        >
          {outputValue ? (
            outputIsJson ? (
              <CodeBlock language={'json'} value={outputValue} />
            ) : (
              <div className="p-[12px]">
                <Markdown content={outputValue} baseFontSize={12} />
              </div>
            )
          ) : (
            <div className="p-[12px]">
              {$i18n.get({
                id: 'main.components.SparkChat.components.Steps.Plugin.index.emptyOutput',
                dm: '无',
              })}
            </div>
          )}
        </CollapsePanel>
      )}
    </div>
  );
};

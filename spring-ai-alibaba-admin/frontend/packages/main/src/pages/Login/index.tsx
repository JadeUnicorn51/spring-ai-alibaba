import Header from '@/layouts/Header';
import LangSelect from '@/layouts/LangSelect';
import PureLayout from '@/layouts/Pure';
import ThemeSelect from '@/layouts/ThemeSelect';
import { authLogin } from '@/services/login';
import { useRequest } from 'ahooks';
import { Alert } from 'antd';
import React from 'react';
import { history, useLocation } from 'umi';
import Login from './components/Login';
import styles from './index.module.less';

const LoginPage: React.FC = () => {
  const location = useLocation();
  const { loading, runAsync } = useRequest((data) => authLogin(data), {
    manual: true,
  });

  const onLogin = (data: any) => {
    runAsync(data).then(() => {
      history.replace('/app');
    });
  };

  return (
    <PureLayout>
      <Header
        right={
          <>
            <ThemeSelect />
            <LangSelect />
          </>
        }
      />
      <div className={styles['container']}>
        {new URLSearchParams(location.search).get('tenant_status') ===
          'disabled' && (
          <Alert
            type="warning"
            showIcon
            message="当前租户已被禁用，请联系平台管理员"
            style={{ marginBottom: 16 }}
          />
        )}
        <Login onSubmit={onLogin} loading={loading} />
      </div>
    </PureLayout>
  );
};

export default LoginPage;

import { getAccountInfo } from '@/services/account';
import { session } from '@/request/session';
import { useRequest } from 'ahooks';
import { Spin } from 'antd';
import { history } from 'umi';

export default function (props: {
  children: React.ReactNode | React.ReactNode[];
}) {
  const { loading } = useRequest(getAccountInfo, {
    onSuccess(res) {
      window.g_config.user = res.data;
    },
    onError(error: any) {
      if (new URL(window.location.href).searchParams.get('ignore-login'))
        return;
      session.clear();
      if (error?.response?.data?.code === 'TenantDisabled') {
        history.replace('/login?tenant_status=disabled');
        return;
      }
      history.replace('/login');
    },
  });

  if (loading)
    return (
      <div className="loading-center">
        <Spin />
      </div>
    );

  return props.children;
}

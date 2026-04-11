import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import {
  createTenantAdmin,
  createTenant,
  disableTenant,
  enableTenant,
  getTenantList,
  updateTenant,
  updateTenantQuota,
} from '@/services/tenant';
import type {
  ICreateTenantAdminParams,
  ICreateTenantParams,
  ITenant,
  IUpdateTenantQuotaParams,
} from '@/types/tenant';
import { isSuperAdminAccountType } from '@/utils/accountType';
import {
  Alert,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import styles from './index.module.less';

interface CreateTenantFormValues extends ICreateTenantParams {}

interface EditTenantFormValues {
  name: string;
  description?: string;
}

interface QuotaFormValues extends IUpdateTenantQuotaParams {}

interface TenantAdminFormValues extends ICreateTenantAdminParams {}

export default function TenantAdminPage() {
  const [loading, setLoading] = useState(false);
  const [tenants, setTenants] = useState<ITenant[]>([]);
  const [searchName, setSearchName] = useState('');
  const [queryName, setQueryName] = useState('');
  const [current, setCurrent] = useState(1);
  const [size, setSize] = useState(10);
  const [total, setTotal] = useState(0);

  const [createVisible, setCreateVisible] = useState(false);
  const [editVisible, setEditVisible] = useState(false);
  const [quotaVisible, setQuotaVisible] = useState(false);
  const [adminVisible, setAdminVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [activeTenant, setActiveTenant] = useState<ITenant | null>(null);

  const [createForm] = Form.useForm<CreateTenantFormValues>();
  const [editForm] = Form.useForm<EditTenantFormValues>();
  const [quotaForm] = Form.useForm<QuotaFormValues>();
  const [adminForm] = Form.useForm<TenantAdminFormValues>();

  const isSuperAdmin = isSuperAdminAccountType(window.g_config.user?.type);

  const fetchTenants = async (
    page = current,
    pageSize = size,
    name = queryName,
  ) => {
    setLoading(true);
    try {
      const response = await getTenantList({
        current: page,
        size: pageSize,
        name: name || undefined,
      });
      setTenants(response.data.records || []);
      setCurrent(response.data.current);
      setSize(response.data.size);
      setTotal(response.data.total);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isSuperAdmin) {
      setTenants([]);
      setTotal(0);
      setCurrent(1);
      return;
    }
    fetchTenants(1, size, queryName);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryName, isSuperAdmin]);

  const onTableChange = (pagination: TablePaginationConfig) => {
    const nextCurrent = pagination.current || 1;
    const nextSize = pagination.pageSize || size;
    fetchTenants(nextCurrent, nextSize);
  };

  const onCreate = async () => {
    try {
      const values = await createForm.validateFields();
      setSubmitting(true);
      await createTenant(values);
      message.success(
        $i18n.get({
          id: 'main.pages.Admin.Tenant.createSuccess',
          dm: '租户创建成功',
        }),
      );
      setCreateVisible(false);
      createForm.resetFields();
      fetchTenants(1, size, queryName);
    } finally {
      setSubmitting(false);
    }
  };

  const onUpdate = async () => {
    if (!activeTenant) return;
    try {
      const values = await editForm.validateFields();
      setSubmitting(true);
      await updateTenant(activeTenant.tenant_id, values);
      message.success(
        $i18n.get({
          id: 'main.pages.Admin.Tenant.updateSuccess',
          dm: '租户信息更新成功',
        }),
      );
      setEditVisible(false);
      setActiveTenant(null);
      fetchTenants(current, size, queryName);
    } finally {
      setSubmitting(false);
    }
  };

  const onUpdateQuota = async () => {
    if (!activeTenant) return;
    try {
      const values = await quotaForm.validateFields();
      setSubmitting(true);
      await updateTenantQuota(activeTenant.tenant_id, values);
      message.success(
        $i18n.get({
          id: 'main.pages.Admin.Tenant.quotaSuccess',
          dm: '租户配额更新成功',
        }),
      );
      setQuotaVisible(false);
      setActiveTenant(null);
      fetchTenants(current, size, queryName);
    } finally {
      setSubmitting(false);
    }
  };

  const onCreateTenantAdmin = async () => {
    if (!activeTenant) return;
    try {
      const values = await adminForm.validateFields();
      setSubmitting(true);
      await createTenantAdmin(activeTenant.tenant_id, values);
      message.success(
        $i18n.get({
          id: 'main.pages.Admin.Tenant.createAdminSuccess',
          dm: '租户管理员创建成功',
        }),
      );
      setAdminVisible(false);
      setActiveTenant(null);
      adminForm.resetFields();
    } finally {
      setSubmitting(false);
    }
  };

  const onToggleStatus = (tenant: ITenant) => {
    const targetStatus = tenant.status === 1 ? 0 : 1;
    Modal.confirm({
      title:
        targetStatus === 1
          ? $i18n.get({
              id: 'main.pages.Admin.Tenant.enableConfirm',
              dm: '确认启用该租户？',
            })
          : $i18n.get({
              id: 'main.pages.Admin.Tenant.disableConfirm',
              dm: '确认禁用该租户？',
            }),
      onOk: async () => {
        if (targetStatus === 1) {
          await enableTenant(tenant.tenant_id);
        } else {
          await disableTenant(tenant.tenant_id);
        }
        message.success(
          targetStatus === 1
            ? $i18n.get({
                id: 'main.pages.Admin.Tenant.enableSuccess',
                dm: '租户已启用',
              })
            : $i18n.get({
                id: 'main.pages.Admin.Tenant.disableSuccess',
                dm: '租户已禁用',
              }),
        );
        fetchTenants(current, size, queryName);
      },
    });
  };

  const openEditModal = (tenant: ITenant) => {
    setActiveTenant(tenant);
    editForm.setFieldsValue({
      name: tenant.name,
      description: tenant.description,
    });
    setEditVisible(true);
  };

  const openQuotaModal = (tenant: ITenant) => {
    setActiveTenant(tenant);
    quotaForm.setFieldsValue({
      max_users: tenant.max_users,
      max_apps: tenant.max_apps,
      max_workspaces: tenant.max_workspaces,
      max_storage_gb: tenant.max_storage_gb,
      max_api_calls_per_day: tenant.max_api_calls_per_day,
    });
    setQuotaVisible(true);
  };

  const openTenantAdminModal = (tenant: ITenant) => {
    setActiveTenant(tenant);
    adminForm.setFieldsValue({
      username: '',
      password: '',
      nickname: `${tenant.name}-admin`,
      email: undefined,
      mobile: undefined,
    });
    setAdminVisible(true);
  };

  const columns: ColumnsType<ITenant> = [
    {
      title: 'Tenant ID',
      dataIndex: 'tenant_id',
      key: 'tenant_id',
      width: 220,
      ellipsis: true,
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.name',
        dm: '租户名称',
      }),
      dataIndex: 'name',
      key: 'name',
      width: 180,
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.status',
        dm: '状态',
      }),
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: number) =>
        value === 1 ? (
          <Tag color="success">ACTIVE</Tag>
        ) : (
          <Tag color="error">DISABLED</Tag>
        ),
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.quota',
        dm: '配额',
      }),
      key: 'quota',
      render: (_, record) => (
        <div className={styles.quotaText}>
          <div>{`Users ${record.max_users} | Apps ${record.max_apps}`}</div>
          <div>{`WS ${record.max_workspaces} | Storage ${record.max_storage_gb}GB`}</div>
          <div>{`API/day ${record.max_api_calls_per_day}`}</div>
        </div>
      ),
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.expireDate',
        dm: '到期时间',
      }),
      dataIndex: 'expire_date',
      key: 'expire_date',
      width: 160,
      render: (value?: string) =>
        value ? dayjs(value).format('YYYY-MM-DD') : '-',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.actions',
        dm: '操作',
      }),
      key: 'actions',
      width: 280,
      render: (_, record) => (
        <Space size={8} wrap>
          <Button size="small" disabled={!isSuperAdmin} onClick={() => openEditModal(record)}>
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.edit',
              dm: '编辑',
            })}
          </Button>
          <Button size="small" disabled={!isSuperAdmin} onClick={() => openQuotaModal(record)}>
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.editQuota',
              dm: '调整配额',
            })}
          </Button>
          <Button
            size="small"
            disabled={!isSuperAdmin}
            onClick={() => openTenantAdminModal(record)}
          >
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.createAdmin',
              dm: '创建管理员',
            })}
          </Button>
          <Button
            size="small"
            danger={record.status === 1}
            disabled={!isSuperAdmin}
            onClick={() => onToggleStatus(record)}
          >
            {record.status === 1
              ? $i18n.get({
                  id: 'main.pages.Admin.Tenant.disable',
                  dm: '禁用',
                })
              : $i18n.get({
                  id: 'main.pages.Admin.Tenant.enable',
                  dm: '启用',
                })}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.App.index.home',
            dm: '首页',
          }),
          path: '/',
        },
        {
          title: $i18n.get({
            id: 'main.pages.Admin.Tenant.title',
            dm: '租户管理',
          }),
        },
      ]}
      right={
        <Space>
          <Input.Search
            allowClear
            value={searchName}
            placeholder={$i18n.get({
              id: 'main.pages.Admin.Tenant.searchPlaceholder',
              dm: '按租户名称搜索',
            })}
            onChange={(event) => setSearchName(event.target.value)}
            onSearch={(value) => setQueryName(value.trim())}
            style={{ width: 260 }}
          />
          <Button type="primary" disabled={!isSuperAdmin} onClick={() => setCreateVisible(true)}>
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.create',
              dm: '新增租户',
            })}
          </Button>
        </Space>
      }
    >
      <div className={styles.container}>
        {!isSuperAdmin && (
          <Alert
            type="warning"
            showIcon
            message={$i18n.get({
              id: 'main.pages.Admin.Tenant.permissionHint',
              dm: '仅平台管理员（SUPER_ADMIN）可访问租户管理能力',
            })}
            className={styles.permissionAlert}
          />
        )}
        <Table
          rowKey="tenant_id"
          columns={columns}
          dataSource={tenants}
          loading={loading}
          pagination={{
            current,
            pageSize: size,
            total,
            showSizeChanger: true,
          }}
          onChange={onTableChange}
        />
      </div>

      <Modal
        title={$i18n.get({
          id: 'main.pages.Admin.Tenant.create',
          dm: '新增租户',
        })}
        open={createVisible}
        onCancel={() => setCreateVisible(false)}
        onOk={onCreate}
        confirmLoading={submitting}
        okButtonProps={{ disabled: !isSuperAdmin }}
      >
        <Form
          layout="vertical"
          form={createForm}
          requiredMark={false}
          initialValues={{
            max_users: 10,
            max_apps: 50,
            max_workspaces: 5,
            max_storage_gb: 100,
            max_api_calls_per_day: 10000,
          }}
        >
          <Form.Item
            name="name"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.name',
              dm: '租户名称',
            })}
            rules={[{ required: true, message: '请输入租户名称' }]}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item
            name="description"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.description',
              dm: '描述',
            })}
          >
            <Input.TextArea maxLength={256} rows={3} />
          </Form.Item>
          <div className={styles.formGrid}>
            <Form.Item name="max_users" label="max_users">
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="max_apps" label="max_apps">
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="max_workspaces" label="max_workspaces">
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="max_storage_gb" label="max_storage_gb">
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="max_api_calls_per_day" label="max_api_calls_per_day">
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal
        title={$i18n.get({
          id: 'main.pages.Admin.Tenant.edit',
          dm: '编辑租户',
        })}
        open={editVisible}
        onCancel={() => {
          setEditVisible(false);
          setActiveTenant(null);
        }}
        onOk={onUpdate}
        confirmLoading={submitting}
        okButtonProps={{ disabled: !isSuperAdmin }}
      >
        <Form layout="vertical" form={editForm} requiredMark={false}>
          <Form.Item
            name="name"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.name',
              dm: '租户名称',
            })}
            rules={[{ required: true, message: '请输入租户名称' }]}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item
            name="description"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.description',
              dm: '描述',
            })}
          >
            <Input.TextArea maxLength={256} rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={$i18n.get({
          id: 'main.pages.Admin.Tenant.createAdmin',
          dm: '创建管理员',
        })}
        open={adminVisible}
        onCancel={() => {
          setAdminVisible(false);
          setActiveTenant(null);
        }}
        onOk={onCreateTenantAdmin}
        confirmLoading={submitting}
        okButtonProps={{ disabled: !isSuperAdmin }}
      >
        <Form layout="vertical" form={adminForm} requiredMark={false}>
          <Form.Item
            name="username"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminUsername',
              dm: '用户名',
            })}
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item
            name="password"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminPassword',
              dm: '密码',
            })}
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password maxLength={64} />
          </Form.Item>
          <Form.Item
            name="nickname"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminNickname',
              dm: '昵称',
            })}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item
            name="email"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminEmail',
              dm: '邮箱',
            })}
          >
            <Input maxLength={128} />
          </Form.Item>
          <Form.Item
            name="mobile"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminMobile',
              dm: '手机号',
            })}
          >
            <Input maxLength={32} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={$i18n.get({
          id: 'main.pages.Admin.Tenant.editQuota',
          dm: '调整租户配额',
        })}
        open={quotaVisible}
        onCancel={() => {
          setQuotaVisible(false);
          setActiveTenant(null);
        }}
        onOk={onUpdateQuota}
        confirmLoading={submitting}
        okButtonProps={{ disabled: !isSuperAdmin }}
      >
        <Form layout="vertical" form={quotaForm} requiredMark={false}>
          <div className={styles.formGrid}>
            <Form.Item
              name="max_users"
              label="max_users"
              rules={[{ required: true, message: '必填' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="max_apps"
              label="max_apps"
              rules={[{ required: true, message: '必填' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="max_workspaces"
              label="max_workspaces"
              rules={[{ required: true, message: '必填' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="max_storage_gb"
              label="max_storage_gb"
              rules={[{ required: true, message: '必填' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="max_api_calls_per_day"
              label="max_api_calls_per_day"
              rules={[{ required: true, message: '必填' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </InnerLayout>
  );
}

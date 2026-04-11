import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import {
  createTenantAdmin,
  createTenant,
  deleteTenantAdmin,
  disableTenantAdmin,
  disableTenant,
  enableTenantAdmin,
  enableTenant,
  getTenantAdminAuditList,
  getTenantAdminList,
  getTenantList,
  resetTenantAdminPassword,
  updateTenant,
  updateTenantQuota,
} from '@/services/tenant';
import type {
  ICreateTenantAdminParams,
  ICreateTenantParams,
  IResetTenantAdminPasswordParams,
  ITenant,
  ITenantAdminAudit,
  ITenantAdmin,
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

interface ResetTenantAdminPasswordFormValues extends IResetTenantAdminPasswordParams {}

const ADMIN_LIST_PAGE_SIZE = 10;
const ADMIN_AUDIT_PAGE_SIZE = 10;

interface AuditFilterValues {
  operation?: string;
  operatorAccountId?: string;
  targetAccountId?: string;
}

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
  const [adminListVisible, setAdminListVisible] = useState(false);
  const [adminAuditVisible, setAdminAuditVisible] = useState(false);
  const [resetPasswordVisible, setResetPasswordVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [activeTenant, setActiveTenant] = useState<ITenant | null>(null);
  const [activeTenantAdmin, setActiveTenantAdmin] = useState<ITenantAdmin | null>(null);

  const [adminListLoading, setAdminListLoading] = useState(false);
  const [tenantAdmins, setTenantAdmins] = useState<ITenantAdmin[]>([]);
  const [adminCurrent, setAdminCurrent] = useState(1);
  const [adminSize, setAdminSize] = useState(ADMIN_LIST_PAGE_SIZE);
  const [adminTotal, setAdminTotal] = useState(0);
  const [adminAuditLoading, setAdminAuditLoading] = useState(false);
  const [tenantAdminAudits, setTenantAdminAudits] = useState<ITenantAdminAudit[]>([]);
  const [adminAuditCurrent, setAdminAuditCurrent] = useState(1);
  const [adminAuditSize, setAdminAuditSize] = useState(ADMIN_AUDIT_PAGE_SIZE);
  const [adminAuditTotal, setAdminAuditTotal] = useState(0);
  const [auditOperation, setAuditOperation] = useState('');
  const [auditOperatorAccountId, setAuditOperatorAccountId] = useState('');
  const [auditTargetAccountId, setAuditTargetAccountId] = useState('');

  const [createForm] = Form.useForm<CreateTenantFormValues>();
  const [editForm] = Form.useForm<EditTenantFormValues>();
  const [quotaForm] = Form.useForm<QuotaFormValues>();
  const [adminForm] = Form.useForm<TenantAdminFormValues>();
  const [resetPasswordForm] = Form.useForm<ResetTenantAdminPasswordFormValues>();

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

  const fetchTenantAdmins = async (
    tenantId: string,
    page = adminCurrent,
    pageSize = adminSize,
  ) => {
    setAdminListLoading(true);
    try {
      const response = await getTenantAdminList(tenantId, {
        current: page,
        size: pageSize,
      });
      setTenantAdmins(response.data.records || []);
      setAdminCurrent(response.data.current);
      setAdminSize(response.data.size);
      setAdminTotal(response.data.total);
    } finally {
      setAdminListLoading(false);
    }
  };

  const fetchTenantAdminAudits = async (
    tenantId: string,
    page = adminAuditCurrent,
    pageSize = adminAuditSize,
    filters?: AuditFilterValues,
  ) => {
    const operation = (filters?.operation ?? auditOperation).trim();
    const operatorAccountId = (
      filters?.operatorAccountId ?? auditOperatorAccountId
    ).trim();
    const targetAccountId = (
      filters?.targetAccountId ?? auditTargetAccountId
    ).trim();
    setAdminAuditLoading(true);
    try {
      const response = await getTenantAdminAuditList(tenantId, {
        current: page,
        size: pageSize,
        operation: operation || undefined,
        operatorAccountId: operatorAccountId || undefined,
        targetAccountId: targetAccountId || undefined,
      });
      setTenantAdminAudits(response.data.records || []);
      setAdminAuditCurrent(response.data.current);
      setAdminAuditSize(response.data.size);
      setAdminAuditTotal(response.data.total);
    } finally {
      setAdminAuditLoading(false);
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

  const onAdminTableChange = (pagination: TablePaginationConfig) => {
    if (!activeTenant) return;
    const nextCurrent = pagination.current || 1;
    const nextSize = pagination.pageSize || adminSize;
    fetchTenantAdmins(activeTenant.tenant_id, nextCurrent, nextSize);
  };

  const onAdminAuditTableChange = (pagination: TablePaginationConfig) => {
    if (!activeTenant) return;
    const nextCurrent = pagination.current || 1;
    const nextSize = pagination.pageSize || adminAuditSize;
    fetchTenantAdminAudits(activeTenant.tenant_id, nextCurrent, nextSize);
  };

  const onAdminAuditFilterSearch = () => {
    if (!activeTenant) return;
    fetchTenantAdminAudits(activeTenant.tenant_id, 1, adminAuditSize, {
      operation: auditOperation,
      operatorAccountId: auditOperatorAccountId,
      targetAccountId: auditTargetAccountId,
    });
  };

  const onAdminAuditFilterReset = () => {
    if (!activeTenant) return;
    setAuditOperation('');
    setAuditOperatorAccountId('');
    setAuditTargetAccountId('');
    fetchTenantAdminAudits(activeTenant.tenant_id, 1, adminAuditSize, {
      operation: '',
      operatorAccountId: '',
      targetAccountId: '',
    });
  };

  const onCreate = async () => {
    try {
      const values = await createForm.validateFields();
      setSubmitting(true);
      await createTenant(values);
      message.success(
        $i18n.get({
          id: 'main.pages.Admin.Tenant.createSuccess',
          dm: 'Tenant created successfully',
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
          dm: 'Tenant updated successfully',
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
          dm: 'Tenant quota updated successfully',
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
          dm: 'Tenant admin created successfully',
        }),
      );
      setAdminVisible(false);
      adminForm.resetFields();
      if (adminListVisible) {
        fetchTenantAdmins(activeTenant.tenant_id, adminCurrent, adminSize);
      } else {
        setActiveTenant(null);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const onToggleTenantAdminStatus = (admin: ITenantAdmin) => {
    if (!activeTenant) return;
    const normalizedStatus = (admin.status || '').toLowerCase();
    const nextEnable = normalizedStatus === 'disabled';
    Modal.confirm({
      title: nextEnable
        ? $i18n.get({
            id: 'main.pages.Admin.Tenant.enableAdminConfirm',
            dm: 'Enable this tenant admin?',
          })
        : $i18n.get({
            id: 'main.pages.Admin.Tenant.disableAdminConfirm',
            dm: 'Disable this tenant admin?',
          }),
      onOk: async () => {
        if (nextEnable) {
          await enableTenantAdmin(activeTenant.tenant_id, admin.account_id);
        } else {
          await disableTenantAdmin(activeTenant.tenant_id, admin.account_id);
        }
        message.success(
          nextEnable
            ? $i18n.get({
                id: 'main.pages.Admin.Tenant.enableAdminSuccess',
                dm: 'Tenant admin enabled',
              })
            : $i18n.get({
                id: 'main.pages.Admin.Tenant.disableAdminSuccess',
                dm: 'Tenant admin disabled',
              }),
        );
        fetchTenantAdmins(activeTenant.tenant_id, adminCurrent, adminSize);
      },
    });
  };

  const onDeleteTenantAdmin = (admin: ITenantAdmin) => {
    if (!activeTenant) return;
    Modal.confirm({
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.deleteAdminConfirm',
        dm: 'Delete this tenant admin?',
      }),
      onOk: async () => {
        await deleteTenantAdmin(activeTenant.tenant_id, admin.account_id);
        message.success(
          $i18n.get({
            id: 'main.pages.Admin.Tenant.deleteAdminSuccess',
            dm: 'Tenant admin deleted',
          }),
        );
        fetchTenantAdmins(activeTenant.tenant_id, adminCurrent, adminSize);
      },
    });
  };

  const openResetTenantAdminPasswordModal = (admin: ITenantAdmin) => {
    setActiveTenantAdmin(admin);
    resetPasswordForm.setFieldsValue({ new_password: '' });
    setResetPasswordVisible(true);
  };

  const onResetTenantAdminPassword = async () => {
    if (!activeTenant || !activeTenantAdmin) return;
    try {
      const values = await resetPasswordForm.validateFields();
      setSubmitting(true);
      await resetTenantAdminPassword(
        activeTenant.tenant_id,
        activeTenantAdmin.account_id,
        values,
      );
      message.success(
        $i18n.get({
          id: 'main.pages.Admin.Tenant.resetAdminPasswordSuccess',
          dm: 'Tenant admin password reset successfully',
        }),
      );
      setResetPasswordVisible(false);
      setActiveTenantAdmin(null);
      resetPasswordForm.resetFields();
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
              dm: 'Enable this tenant?',
            })
          : $i18n.get({
              id: 'main.pages.Admin.Tenant.disableConfirm',
              dm: 'Disable this tenant?',
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
                dm: 'Tenant enabled',
              })
            : $i18n.get({
                id: 'main.pages.Admin.Tenant.disableSuccess',
                dm: 'Tenant disabled',
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

  const openTenantAdminListModal = (tenant: ITenant) => {
    setActiveTenant(tenant);
    setAdminListVisible(true);
    fetchTenantAdmins(tenant.tenant_id, 1, ADMIN_LIST_PAGE_SIZE);
  };

  const openTenantAdminAuditModal = (tenant: ITenant) => {
    setActiveTenant(tenant);
    setAdminAuditVisible(true);
    setAuditOperation('');
    setAuditOperatorAccountId('');
    setAuditTargetAccountId('');
    fetchTenantAdminAudits(tenant.tenant_id, 1, ADMIN_AUDIT_PAGE_SIZE, {
      operation: '',
      operatorAccountId: '',
      targetAccountId: '',
    });
  };

  const adminColumns: ColumnsType<ITenantAdmin> = [
    {
      title: 'Account ID',
      dataIndex: 'account_id',
      key: 'account_id',
      width: 160,
      ellipsis: true,
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.adminUsername',
        dm: 'Username',
      }),
      dataIndex: 'username',
      key: 'username',
      width: 130,
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.adminNickname',
        dm: 'Nickname',
      }),
      dataIndex: 'nickname',
      key: 'nickname',
      width: 130,
      render: (value?: string) => value || '-',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.adminType',
        dm: 'Role',
      }),
      dataIndex: 'type',
      key: 'type',
      width: 110,
      render: (value?: string) => value || '-',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.status',
        dm: 'Status',
      }),
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value?: string) => {
        const normalized = (value || '').toLowerCase();
        if (normalized === 'normal') {
          return <Tag color="success">NORMAL</Tag>;
        }
        if (normalized === 'disabled') {
          return <Tag color="error">DISABLED</Tag>;
        }
        return <Tag>{value || '-'}</Tag>;
      },
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.createTime',
        dm: 'Created At',
      }),
      dataIndex: 'gmt_create',
      key: 'gmt_create',
      width: 150,
      render: (value?: string) =>
        value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.actions',
        dm: 'Actions',
      }),
      key: 'actions',
      render: (_, record) => {
        const normalizedStatus = (record.status || '').toLowerCase();
        const disabled = normalizedStatus === 'disabled';
        return (
          <Space size={4} wrap>
            <Button
              size="small"
              type="link"
              disabled={!isSuperAdmin}
              onClick={() => onToggleTenantAdminStatus(record)}
            >
              {disabled
                ? $i18n.get({
                    id: 'main.pages.Admin.Tenant.enable',
                    dm: 'Enable',
                  })
                : $i18n.get({
                    id: 'main.pages.Admin.Tenant.disable',
                    dm: 'Disable',
                  })}
            </Button>
            <Button
              size="small"
              type="link"
              disabled={!isSuperAdmin}
              onClick={() => openResetTenantAdminPasswordModal(record)}
            >
              {$i18n.get({
                id: 'main.pages.Admin.Tenant.resetAdminPassword',
                dm: 'Reset Password',
              })}
            </Button>
            <Button
              size="small"
              type="link"
              danger
              disabled={!isSuperAdmin}
              onClick={() => onDeleteTenantAdmin(record)}
            >
              {$i18n.get({
                id: 'main.pages.Admin.Tenant.deleteAdmin',
                dm: 'Delete',
              })}
            </Button>
          </Space>
        );
      },
    },
  ];

  const adminAuditColumns: ColumnsType<ITenantAdminAudit> = [
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.createTime',
        dm: 'Created At',
      }),
      dataIndex: 'gmt_create',
      key: 'gmt_create',
      width: 180,
      render: (value?: string) =>
        value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.auditOperation',
        dm: 'Operation',
      }),
      dataIndex: 'operation',
      key: 'operation',
      width: 180,
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.auditOperator',
        dm: 'Operator',
      }),
      dataIndex: 'operator_account_id',
      key: 'operator_account_id',
      width: 160,
      render: (value?: string) => value || '-',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.auditTarget',
        dm: 'Target',
      }),
      dataIndex: 'target_account_id',
      key: 'target_account_id',
      width: 160,
      render: (value?: string) => value || '-',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.auditRequestId',
        dm: 'Request ID',
      }),
      dataIndex: 'request_id',
      key: 'request_id',
      width: 200,
      ellipsis: true,
      render: (value?: string) => value || '-',
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.auditDetails',
        dm: 'Details',
      }),
      dataIndex: 'details',
      key: 'details',
      render: (value?: string) => value || '-',
    },
  ];

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
        dm: 'Tenant Name',
      }),
      dataIndex: 'name',
      key: 'name',
      width: 180,
    },
    {
      title: $i18n.get({
        id: 'main.pages.Admin.Tenant.status',
        dm: 'Status',
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
        dm: 'Quota',
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
        dm: 'Expire Date',
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
        dm: 'Actions',
      }),
      key: 'actions',
      width: 520,
      render: (_, record) => (
        <Space size={8} wrap>
          <Button
            size="small"
            disabled={!isSuperAdmin}
            onClick={() => openTenantAdminAuditModal(record)}
          >
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.adminAuditList',
              dm: 'Admin Audits',
            })}
          </Button>
          <Button
            size="small"
            disabled={!isSuperAdmin}
            onClick={() => openTenantAdminListModal(record)}
          >
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.adminList',
              dm: 'Admin List',
            })}
          </Button>
          <Button
            size="small"
            disabled={!isSuperAdmin}
            onClick={() => openTenantAdminModal(record)}
          >
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.createAdmin',
              dm: 'Create Admin',
            })}
          </Button>
          <Button
            size="small"
            disabled={!isSuperAdmin}
            onClick={() => openEditModal(record)}
          >
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.edit',
              dm: 'Edit',
            })}
          </Button>
          <Button
            size="small"
            disabled={!isSuperAdmin}
            onClick={() => openQuotaModal(record)}
          >
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.editQuota',
              dm: 'Edit Quota',
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
                  dm: 'Disable',
                })
              : $i18n.get({
                  id: 'main.pages.Admin.Tenant.enable',
                  dm: 'Enable',
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
            dm: 'Home',
          }),
          path: '/',
        },
        {
          title: $i18n.get({
            id: 'main.pages.Admin.Tenant.title',
            dm: 'Tenant Management',
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
              dm: 'Search by tenant name',
            })}
            onChange={(event) => setSearchName(event.target.value)}
            onSearch={(value) => setQueryName(value.trim())}
            style={{ width: 260 }}
          />
          <Button
            type="primary"
            disabled={!isSuperAdmin}
            onClick={() => setCreateVisible(true)}
          >
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.create',
              dm: 'Create Tenant',
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
              dm: 'Only SUPER_ADMIN can access tenant management.',
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
          dm: 'Create Tenant',
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
              dm: 'Tenant Name',
            })}
            rules={[{ required: true, message: 'Please input tenant name' }]}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item
            name="description"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.description',
              dm: 'Description',
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
          dm: 'Edit Tenant',
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
              dm: 'Tenant Name',
            })}
            rules={[{ required: true, message: 'Please input tenant name' }]}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item
            name="description"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.description',
              dm: 'Description',
            })}
          >
            <Input.TextArea maxLength={256} rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={
          activeTenant
            ? $i18n.get({
                id: 'main.pages.Admin.Tenant.adminAuditTitleWithTenant',
                dm: `Tenant Admin Audits - ${activeTenant.name}`,
              })
            : $i18n.get({
                id: 'main.pages.Admin.Tenant.adminAuditList',
                dm: 'Tenant Admin Audits',
              })
        }
        open={adminAuditVisible}
        footer={null}
        width={980}
        onCancel={() => {
          setAdminAuditVisible(false);
          setActiveTenant(null);
          setTenantAdminAudits([]);
          setAuditOperation('');
          setAuditOperatorAccountId('');
          setAuditTargetAccountId('');
          setAdminAuditCurrent(1);
          setAdminAuditSize(ADMIN_AUDIT_PAGE_SIZE);
          setAdminAuditTotal(0);
        }}
      >
        <Space size={8} wrap className={styles.auditFilterRow}>
          <Input
            allowClear
            value={auditOperation}
            placeholder={$i18n.get({
              id: 'main.pages.Admin.Tenant.auditFilterOperation',
              dm: 'Filter by operation',
            })}
            onChange={(event) => setAuditOperation(event.target.value)}
            onPressEnter={onAdminAuditFilterSearch}
            style={{ width: 180 }}
          />
          <Input
            allowClear
            value={auditOperatorAccountId}
            placeholder={$i18n.get({
              id: 'main.pages.Admin.Tenant.auditFilterOperator',
              dm: 'Filter by operator account',
            })}
            onChange={(event) => setAuditOperatorAccountId(event.target.value)}
            onPressEnter={onAdminAuditFilterSearch}
            style={{ width: 220 }}
          />
          <Input
            allowClear
            value={auditTargetAccountId}
            placeholder={$i18n.get({
              id: 'main.pages.Admin.Tenant.auditFilterTarget',
              dm: 'Filter by target account',
            })}
            onChange={(event) => setAuditTargetAccountId(event.target.value)}
            onPressEnter={onAdminAuditFilterSearch}
            style={{ width: 220 }}
          />
          <Button onClick={onAdminAuditFilterSearch}>
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.search',
              dm: 'Search',
            })}
          </Button>
          <Button onClick={onAdminAuditFilterReset}>
            {$i18n.get({
              id: 'main.pages.Admin.Tenant.reset',
              dm: 'Reset',
            })}
          </Button>
        </Space>
        <Table
          rowKey="id"
          columns={adminAuditColumns}
          dataSource={tenantAdminAudits}
          loading={adminAuditLoading}
          pagination={{
            current: adminAuditCurrent,
            pageSize: adminAuditSize,
            total: adminAuditTotal,
            showSizeChanger: true,
          }}
          onChange={onAdminAuditTableChange}
        />
      </Modal>

      <Modal
        title={
          activeTenant
            ? $i18n.get({
                id: 'main.pages.Admin.Tenant.adminListTitleWithTenant',
                dm: `Tenant Admin List - ${activeTenant.name}`,
              })
            : $i18n.get({
                id: 'main.pages.Admin.Tenant.adminList',
                dm: 'Tenant Admin List',
              })
        }
        open={adminListVisible}
        footer={null}
        width={920}
        onCancel={() => {
          setAdminListVisible(false);
          setResetPasswordVisible(false);
          setActiveTenant(null);
          setActiveTenantAdmin(null);
          setTenantAdmins([]);
          setAdminCurrent(1);
          setAdminSize(ADMIN_LIST_PAGE_SIZE);
          setAdminTotal(0);
          resetPasswordForm.resetFields();
        }}
      >
        <Alert
          className={styles.recreateGuideAlert}
          showIcon
          type="info"
          message={$i18n.get({
            id: 'main.pages.Admin.Tenant.recreateGuideTitle',
            dm: 'Tenant Admin Recreate Guidance',
          })}
          description={$i18n.get({
            id: 'main.pages.Admin.Tenant.recreateGuideDesc',
            dm: 'If tenant admins are deleted/disabled and cannot log in, use Create Admin from this page to bootstrap a new tenant administrator.',
          })}
          action={
            <Button
              size="small"
              type="link"
              disabled={!isSuperAdmin || !activeTenant}
              onClick={() => activeTenant && openTenantAdminModal(activeTenant)}
            >
              {$i18n.get({
                id: 'main.pages.Admin.Tenant.createAdmin',
                dm: 'Create Admin',
              })}
            </Button>
          }
        />
        <Table
          rowKey="account_id"
          columns={adminColumns}
          dataSource={tenantAdmins}
          loading={adminListLoading}
          pagination={{
            current: adminCurrent,
            pageSize: adminSize,
            total: adminTotal,
            showSizeChanger: true,
          }}
          onChange={onAdminTableChange}
        />
      </Modal>

      <Modal
        title={$i18n.get({
          id: 'main.pages.Admin.Tenant.createAdmin',
          dm: 'Create Admin',
        })}
        open={adminVisible}
        onCancel={() => {
          setAdminVisible(false);
          if (!adminListVisible) {
            setActiveTenant(null);
          }
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
              dm: 'Username',
            })}
            rules={[{ required: true, message: 'Please input username' }]}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item
            name="password"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminPassword',
              dm: 'Password',
            })}
            rules={[{ required: true, message: 'Please input password' }]}
          >
            <Input.Password maxLength={64} />
          </Form.Item>
          <Form.Item
            name="nickname"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminNickname',
              dm: 'Nickname',
            })}
          >
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item
            name="email"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminEmail',
              dm: 'Email',
            })}
          >
            <Input maxLength={128} />
          </Form.Item>
          <Form.Item
            name="mobile"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminMobile',
              dm: 'Mobile',
            })}
          >
            <Input maxLength={32} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={$i18n.get({
          id: 'main.pages.Admin.Tenant.resetAdminPassword',
          dm: 'Reset Password',
        })}
        open={resetPasswordVisible}
        onCancel={() => {
          setResetPasswordVisible(false);
          setActiveTenantAdmin(null);
          resetPasswordForm.resetFields();
        }}
        onOk={onResetTenantAdminPassword}
        confirmLoading={submitting}
        okButtonProps={{ disabled: !isSuperAdmin }}
      >
        <Form layout="vertical" form={resetPasswordForm} requiredMark={false}>
          <Form.Item
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminUsername',
              dm: 'Username',
            })}
          >
            <Input value={activeTenantAdmin?.username} disabled />
          </Form.Item>
          <Form.Item
            name="new_password"
            label={$i18n.get({
              id: 'main.pages.Admin.Tenant.adminPassword',
              dm: 'Password',
            })}
            rules={[{ required: true, message: 'Please input password' }]}
          >
            <Input.Password maxLength={64} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={$i18n.get({
          id: 'main.pages.Admin.Tenant.editQuota',
          dm: 'Edit Tenant Quota',
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
              rules={[{ required: true, message: 'Required' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="max_apps"
              label="max_apps"
              rules={[{ required: true, message: 'Required' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="max_workspaces"
              label="max_workspaces"
              rules={[{ required: true, message: 'Required' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="max_storage_gb"
              label="max_storage_gb"
              rules={[{ required: true, message: 'Required' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="max_api_calls_per_day"
              label="max_api_calls_per_day"
              rules={[{ required: true, message: 'Required' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </InnerLayout>
  );
}

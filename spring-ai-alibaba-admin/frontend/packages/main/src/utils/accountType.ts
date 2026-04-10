const ADMIN_ACCOUNT_TYPES = ['admin', 'tenant_admin', 'super_admin'];

export function isAdminAccountType(type?: string): boolean {
  return !!type && ADMIN_ACCOUNT_TYPES.includes(type);
}

export function isSuperAdminAccountType(type?: string): boolean {
  return type === 'super_admin';
}

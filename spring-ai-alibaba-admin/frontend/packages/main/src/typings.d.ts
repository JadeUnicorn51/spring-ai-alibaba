declare global {
  interface Window {
    g_config: {
      user: Partial<{
        username: string;
        type: 'admin' | 'user';
      }>,
      config: Partial<{
        login_method: 'third_party' | 'preset_account';
        upload_method: 'oss' | 'file';
      }>
    }
  }
}

import 'umi/typings';

declare module '*.module.less' {
  const classes: { readonly [key: string]: string };
  export default classes;
}

declare module '*.less' {
  const classes: { readonly [key: string]: string };
  export default classes;
}

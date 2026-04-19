import path from 'path';
import { defineConfig } from 'umi';

export default defineConfig({
  title: 'SAA Desktop',
  define: {
    'process.env.WEB_SERVER': process.env.WEB_SERVER,
    BUILD_ID: new Date().toString(),
  },
  alias: {
    '@': path.resolve(__dirname, './src'),
  },
  routes: [
    {
      path: '/',
      redirect: '/settings',
    },
    {
      path: '/settings',
      component: 'Settings',
    },
  ],
  clickToComponent: {},
  esbuildMinifyIIFE: true,
  mfsu: false,
  proxy: {
    '/desktop': {
      target: process.env.WEB_SERVER || 'http://localhost:28080',
      changeOrigin: true,
    },
    '/console': {
      target: process.env.WEB_SERVER || 'http://localhost:28080',
      changeOrigin: true,
    },
  },
  lessLoader: {
    javascriptEnabled: true,
  },
});

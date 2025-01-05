/**
 * 代理配置
 * 在生产环境 代理是无法生效的，所以这里没有生产环境的配置
 * @see https://pro.ant.design/docs/deploy
 * @doc https://umijs.org/docs/guides/proxy
 */
export default {
  dev: {
    '/easy-extension-api/': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      pathRewrite: { '^/easy-extension-api': '/easy-extension-admin/easy-extension-api' },
    },
  },
};

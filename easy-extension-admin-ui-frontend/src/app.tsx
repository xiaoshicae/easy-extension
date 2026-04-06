import { Document, Footer, SelectLang } from '@/components';
import { getConfigInfo } from '@/services/easy-extension-api/api';
import { PageLoading, Settings as LayoutSettings } from '@ant-design/pro-components';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import defaultSettings from '../config/defaultSettings';
import { errorConfig } from './requestErrorConfig';

/**
 * @see  https://umijs.org/zh-CN/plugins/plugin-initial-state
 * */
export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
  loading?: boolean;
  configInfo?: API.ConfigInfo;
}> {
  const fetchConfigInfo = async () => {
    try {
      const response = await getConfigInfo();
      return response.data;
    } catch (error) {
      console.log('getConfigInfo failed, error=', error);
    }
    return undefined;
  };
  const configInfo = await fetchConfigInfo();
  return { settings: defaultSettings as Partial<LayoutSettings>, configInfo };
}

// ProLayout 支持的api https://procomponents.ant.design/components/layout
export const layout: RunTimeLayoutConfig = ({ initialState }) => {
  const docUrl = initialState?.configInfo?.docUrl;
  const version = initialState?.configInfo?.version;
  return {
    actionsRender: () => [
      docUrl && <Document key="doc" docUrl={docUrl || ''} />,
      <SelectLang key="SelectLang" />,
    ],
    headerTitleRender: (_logo: any, title: any) => (
      <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <img src="./logo.svg" alt="logo" style={{ width: 32, height: 32 }} />{title}
        {version && (
          <span style={{
            fontSize: 12,
            color: '#999',
            fontWeight: 'normal',
            padding: '1px 6px',
            border: '1px solid #d9d9d9',
            borderRadius: 4,
            lineHeight: '18px',
          }}>v{version}</span>
        )}
      </span>
    ),
    avatarProps: {},
    waterMarkProps: {
      // content: 'Easy Extension',
    },
    footerRender: () => <Footer />,
    onPageChange: () => {},
    bgLayoutImgList: [
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/D2LWSqNny4sAAAAAAAAAAAAAFl94AQBr',
        left: 85,
        bottom: 100,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/C2TWRpJpiC0AAAAAAAAAAAAAFl94AQBr',
        bottom: -68,
        right: -45,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/F6vSTbj8KpYAAAAAAAAAAAAAFl94AQBr',
        bottom: 0,
        left: 0,
        width: '331px',
      },
    ],
    menuHeaderRender: undefined,
    // 增加一个 loading 的状态
    childrenRender: (children) => {
      if (initialState?.loading) return <PageLoading />;
      return <>{children}</>;
    },
    ...initialState?.settings,
  };
};

/**
 * @name request 配置，可以配置错误处理
 * 它基于 axios 和 ahooks 的 useRequest 提供了一套统一的网络请求和错误处理方案。
 * @doc https://umijs.org/docs/max/request#配置
 */
export const request: RequestConfig = {
  baseURL: '',
  ...errorConfig,
};

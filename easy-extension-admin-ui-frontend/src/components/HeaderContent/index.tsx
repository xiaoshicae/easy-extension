import { GithubOutlined, FileTextOutlined } from '@ant-design/icons';
import { SelectLang as UmiSelectLang } from '@umijs/max';
import { Tooltip } from 'antd';
import { useIntl } from 'react-intl';

export const SelectLang = () => {
  return <UmiSelectLang />;
};

export const Document = ({ docUrl }: { docUrl: string }) => {
  const intl = useIntl();

  return (
    <Tooltip
      color={'#ececec'}
      title={<text style={{ color: '#6e6e6e' }}>{intl.formatMessage({ id: 'navBar.doc' })}</text>}
    >
      <div
        style={{
          display: 'flex',
          height: '100%',
          padding: '12px',
        }}
        onClick={() => {
          window.open(docUrl);
        }}
      >
        <FileTextOutlined />
      </div>
    </Tooltip>
  );
};

export const Github = ({ projectUrl }: { projectUrl: string }) => {
  return (
    <div
      style={{
        display: 'flex',
        height: '100%',
        padding: '12px',
      }}
      onClick={() => {
        window.open(projectUrl);
      }}
    >
      <GithubOutlined />
    </div>
  );
};

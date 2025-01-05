import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import React from 'react';

const Footer: React.FC = () => {
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      copyright="Powered by Easy Extension"
      links={[
        {
          key: 'github',
          title: <GithubOutlined />,
          href: 'https://github.com/xiaoshicae/easy-extension',
          blankTarget: true,
        },
        {
          key: 'Easy Extension',
          title: 'Easy Extension',
          href: 'https://github.com/xiaoshicae/easy-extension',
          blankTarget: true,
        },
      ]}
    />
  );
};

export default Footer;

// import docIcon from '@/assets/doc-icon.svg';
import { CodeHighlight } from '@/components';
import LetterAvatar from '@/components/LetterAvatar';
import PageContent from '@/components/PageContent';
import { getAllExtensionPoints } from '@/services/easy-extension-api/api';
import { FormattedMessage } from '@@/exports';
import { Button, Card, Modal, Typography } from 'antd';
import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import useStyles from './style';

const { Paragraph } = Typography;

interface ModalInfo {
  title: string;
  content: React.ReactNode;
}

const searchItemFilter = (item: API.ExtensionPointInfo, keyword: string): boolean => {
  if (!item || !keyword) {
    return true;
  }
  const items = keyword.split(':');
  if (items.length > 1) {
    const k = items[0];
    const v = items[1];
    if (k === 'fullName') {
      return item.classInfo?.fullName === v;
    }
  }
  const name = item.classInfo?.name || '';
  const comment = item.classInfo?.comment || '';
  return (
    name.toLowerCase().includes(keyword.toLowerCase()) ||
    comment.toLowerCase().includes(keyword.toLowerCase())
  );
};

const ExtensionPointListPage = () => {
  const { styles } = useStyles({});
  const intl = useIntl();
  const modalWidth = 800;

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalInfo, setModalInfo] = useState<ModalInfo>({ title: '', content: null });

  const showModal = (title: string, content: React.ReactNode) => {
    setIsModalOpen(true);
    setModalInfo({ title, content });
  };

  const handleCancel = () => {
    setIsModalOpen(false);
    setModalInfo({ title: '', content: null });
  };

  const pageHeadContent = (
    <div className={styles.pageHeaderContent}>
      <p>
        <FormattedMessage id="page.extension-point-list.desc" />
      </p>
      {/*<div className={styles.contentLink}>*/}
      {/*  <a>*/}
      {/*    <img alt='' src={docIcon} /> <FormattedMessage id='page.extension-point-list.doc' />*/}
      {/*  </a>*/}
      {/*</div>*/}
    </div>
  );

  const renderListItemContent = (item: API.ExtensionPointInfo) => {
    const avatarLetter = item.classInfo?.name?.charAt(0)?.toUpperCase();

    const sourceCodeModalTitle = intl.formatMessage({
      id: 'page.extension-point-list.card-modal.api-define',
    });
    const sourceCodeModalContent = item.classInfo?.sourceCode && (
      <CodeHighlight code={item.classInfo?.sourceCode || ''} language="java" />
    );

    const defaultImplCodeModalTitle = intl.formatMessage({
      id: 'page.extension-point-list.card-modal.default-impl',
    });

    const defaultImplCodeModalContent = item.defaultImplCode && (
      <CodeHighlight code={item.defaultImplCode || ''} language="java" />
    );

    return (
      <Card
        hoverable
        className={styles.card}
        actions={[
          <Button
            key="source-code"
            color="primary"
            variant="text"
            onClick={() => showModal(sourceCodeModalTitle, sourceCodeModalContent)}
          >
            {intl.formatMessage({ id: 'page.extension-point-list.card-btn.api-define' })}
          </Button>,

          <Button
            key="default-implementation"
            color="primary"
            variant="text"
            onClick={() => showModal(defaultImplCodeModalTitle, defaultImplCodeModalContent)}
          >
            {intl.formatMessage({ id: 'page.extension-point-list.card-btn.default-impl' })}
          </Button>,
        ]}
      >
        <Card.Meta
          avatar={<LetterAvatar letter={avatarLetter} />}
          title={<a>{item.classInfo?.name}</a>}
          description={
            <Paragraph className={styles.item} ellipsis={{ rows: 4 }}>
              {item.classInfo?.comment}
            </Paragraph>
          }
        />
      </Card>
    );
  };

  return (
    <>
      <PageContent<API.ExtensionPointInfo>
        id={(item) => item.id}
        searchPlaceholderI18nId="page.extension-point-list.search-placeholder"
        pageHeadContent={pageHeadContent}
        dataFetcher={getAllExtensionPoints}
        searchItemFilter={searchItemFilter}
        renderItem={renderListItemContent}
      />

      <Modal width={modalWidth} title={modalInfo.title} footer={null} open={isModalOpen} onCancel={handleCancel}>
        {modalInfo.content}
      </Modal>
    </>
  );
};

export default ExtensionPointListPage;

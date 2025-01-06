import { CodeHighlight } from '@/components';
import LetterAvatar from '@/components/LetterAvatar';
import PageContent from '@/components/PageContent';
import { getAllAbilities } from '@/services/easy-extension-api/api';
import { parseModulePath } from '@/utils/path_util';
import { FormattedMessage } from '@@/exports';
import { Button, Card, Modal, Typography } from 'antd';
import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import useStyles from './style';
import { fullNameToShortName } from "@/utils/string_util";

const { Paragraph } = Typography;

const searchItemFilter = (item: API.AbilityInfo, keyword: string): boolean => {
  if (!item || !keyword) {
    return true;
  }
  const items = keyword.split(':');
  if (items.length > 1) {
    const k = items[0];
    const v = items[1];
    if (k === 'code') {
      return item.code === v;
    }
    if (k === 'fullName') {
      return item.classInfo?.fullName === v;
    }
  }
  const code = item.code || '';
  const name = item.classInfo?.name || '';
  const comment = item.classInfo?.comment || '';
  return (
    code.toLowerCase().includes(keyword.toLowerCase()) ||
    name.toLowerCase().includes(keyword.toLowerCase()) ||
    comment.toLowerCase().includes(keyword.toLowerCase())
  );
};

interface ModalInfo {
  title: string;
  content: React.ReactNode;
}

const AbilityListPage = () => {
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
        <FormattedMessage id="page.abilities.desc" />
      </p>
    </div>
  );

  const renderListItemContent = (item: API.AbilityInfo) => {
    const avatarLetter = item.classInfo?.name?.charAt(0)?.toUpperCase();
    const sourceCodeModalTitle = intl.formatMessage({
      id: 'page.abilities.card-modal.api-define',
    });
    const sourceCodeModalContent = item.classInfo?.sourceCode && (
      <CodeHighlight code={item.classInfo?.sourceCode || ''} language="java" />
    );
    const implExtModalTitle = intl.formatMessage({
      id: 'page.abilities.card-modal.ext-impl',
    });
    const implExtModalContent = item.implExtensionPoints?.length > 0 && (
      <ul className={styles.extensionImplementationContent}>
        {item.implExtensionPoints?.map((extFullName, idx) => {
          return (
            <li key={idx}>
              <a
                href={parseModulePath('extension-points', {
                  keyword: 'fullName:' + extFullName,
                })}
                target="_blank"
                rel="noreferrer"
              >
                {fullNameToShortName(extFullName)}
              </a>
            </li>
          );
        })}
      </ul>
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
            {intl.formatMessage({ id: 'page.abilities.card-btn.api-define' })}
          </Button>,

          <Button
            key="extension-implementation"
            color="primary"
            variant="text"
            onClick={() => showModal(implExtModalTitle, implExtModalContent)}
          >
            {intl.formatMessage({ id: 'page.abilities.card-btn.ext-impl' })}
          </Button>,
        ]}
      >
        <Card.Meta
          avatar={<LetterAvatar letter={avatarLetter} />}
          title={<a>{item.code}</a>}
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
      <PageContent<API.AbilityInfo>
        id={(item) => item.code}
        searchPlaceholderI18nId="page.abilities.search-placeholder"
        pageHeadContent={pageHeadContent}
        dataFetcher={getAllAbilities}
        searchItemFilter={searchItemFilter}
        renderItem={renderListItemContent}
      />

      <Modal   width={modalWidth} title={modalInfo.title} footer={null} open={isModalOpen} onCancel={handleCancel}>
        {modalInfo.content}
      </Modal>
    </>
  );
};

export default AbilityListPage;

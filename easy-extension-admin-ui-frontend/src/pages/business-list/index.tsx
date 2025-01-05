import { CodeHighlight } from '@/components';
import LetterAvatar from '@/components/LetterAvatar';
import PageContent from '@/components/PageContent';
import { getAllAbilities, getAllBusiness, getAllExtensionPoints } from '@/services/easy-extension-api/api';
import { parseModulePath } from '@/utils/path_util';
import { fullNameToShortName } from '@/utils/string_util';
import { FormattedMessage } from '@@/exports';
import { Button, Card, Modal, Typography } from 'antd';
import React, { useEffect, useState } from 'react';
import { useIntl } from 'react-intl';
import ExtensionPointInfoTable from './ExtensionPointInfoTable';
import useStyles from './style';

const { Paragraph } = Typography;

interface ModalInfo {
  title: string;
  content: React.ReactNode;
}

const searchItemFilter = (item: API.BusinessInfo, keyword: string): boolean => {
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
      return item.classInfo?.name === v;
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

const BusinessListPage = () => {
  const { styles } = useStyles({});
  const intl = useIntl();
  const [modalWidth, setModalWidth] = useState(800);

  const [abilities, setAbilities] = useState<Array<API.AbilityInfo>>([]);
  useEffect(() => {
    getAllAbilities().then((response) => {
      const data = response.data;
      setAbilities(data);
    });
  }, []);

  const [extensionPoints, setExtensionPoints] = useState<Array<API.ExtensionPointInfo>>([]);
  useEffect(() => {
    getAllExtensionPoints().then((response) => {
      const data = response.data;
      setExtensionPoints(data);
    });
  }, []);


  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalInfo, setModalInfo] = useState<ModalInfo>({ title: '', content: null });

  const showModal = (title: string, content: React.ReactNode, modalWidth: number = 800) => {
    setIsModalOpen(true);
    setModalInfo({ title, content });
    setModalWidth(modalWidth)
  };

  const handleCancel = () => {
    setIsModalOpen(false);
    setModalInfo({ title: '', content: null });
  };

  const pageHeadContent = (
    <div className={styles.pageHeaderContent}>
      <p>
        <FormattedMessage id="page.businesses.desc" />
      </p>
    </div>
  );

  const renderListItemContent = (item: API.BusinessInfo) => {
    const avatarLetter = item.classInfo?.name?.charAt(0)?.toUpperCase();
    const sourceCodeModalTitle = intl.formatMessage({
      id: 'page.businesses.card-modal.source-code',
    });
    const sourceCodeModalContent = item.classInfo?.sourceCode && (
      <CodeHighlight code={item.classInfo?.sourceCode || ''} language="java" />
    );
    const implExtModalTitle = intl.formatMessage({
      id: 'page.businesses.card-modal.ext-impl',
    });
    const implExtModalContent =
      item.implExtensionPoints?.length > 0 ? (
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
      ) : (
        <p>
          {intl.formatMessage({
            id: 'page.businesses.card-modal.none-abilities',
          })}
        </p>
      );
    const usedAbilitiesModalTitle = intl.formatMessage({
      id: 'page.businesses.card-modal.used-abilities',
    });

    const usedAbilitiesModalContent = <ExtensionPointInfoTable businessInfo={item} abilities={abilities} extensionPoints={extensionPoints} />;

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
            {intl.formatMessage({ id: 'page.businesses.card-btn.source-code' })}
          </Button>,

          <Button
            key="extension-implementation"
            color="primary"
            variant="text"
            onClick={() => showModal(implExtModalTitle, implExtModalContent)}
          >
            {intl.formatMessage({ id: 'page.businesses.card-btn.ext-impl' })}
          </Button>,

          <Button
            key="used-abilities"
            color="primary"
            variant="text"
            onClick={() => showModal(usedAbilitiesModalTitle, usedAbilitiesModalContent, 1000)}
          >
            {intl.formatMessage({ id: 'page.businesses.card-btn.used-abilities' })}
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
      <PageContent<API.BusinessInfo>
        id={(item) => item.code}
        searchPlaceholderI18nId="page.businesses.search-placeholder"
        pageHeadContent={pageHeadContent}
        dataFetcher={getAllBusiness}
        searchItemFilter={searchItemFilter}
        renderItem={renderListItemContent}
      />

      <Modal
        width={modalWidth}
        title={modalInfo.title}
        footer={null}
        open={isModalOpen}
        onCancel={handleCancel}
      >
        {modalInfo.content}
      </Modal>
    </>
  );
};

export default BusinessListPage;

import type { PageContentProps } from '@/components/PageContent/types';
import { getPageData } from '@/utils/page_util';
import { PageContainer } from '@ant-design/pro-components';
import { useRequest } from '@umijs/max';
import { Input, List, Pagination, PaginationProps } from 'antd';
import { useEffect, useState } from 'react';
import { useIntl } from 'react-intl';
import { useSearchParams } from 'react-router-dom';
import useStyles from './style';

function PageContent<T>(props: PageContentProps<T>) {
  const {
    id,
    searchPlaceholderI18nId,
    pageHeadContent,
    initPageNo = 1,
    initPageSize = 8,
    dataFetcher,
    searchItemFilter,
    renderItem,
  } = props;

  const pageSize = initPageSize;

  const { styles } = useStyles({});
  const intl = useIntl();

  // url search param control
  const [searchParams, setSearchParams] = useSearchParams();

  // search keyword control
  const [keyword, setKeyword] = useState('');

  // search loading control
  const [searchLoading, setSearchLoading] = useState<boolean>(false);

  // datasource that filtered by search keyword
  const [dataSource, setDataSource] = useState<Array<any>>([]);

  // datasource that show in one page
  const [showDataSource, setShowDataSource] = useState<Array<any>>([]);

  // page control
  const [page, setPage] = useState(1);

  // handle page change event
  const handlePageChange: PaginationProps['onChange'] = (p) => {
    setPage(p);
  };

  // handle keyword change event
  const handleInputChange = (event: any) => {
    setKeyword(event.target.value);
  };

  // set show datasource when page change
  useEffect(() => {
    setShowDataSource(getPageData(dataSource, page, pageSize));
  }, [page]);

  // datasource change (i.e. filter by search keyword)
  // set page and last page can not hold => then page change will set show datasource
  // set show datasource and last page can hold
  useEffect(() => {
    if ((page - 1) * pageSize > dataSource.length) {
      const lastPage = Math.ceil(dataSource.length / pageSize);
      setPage(lastPage);
    } else {
      setShowDataSource(getPageData(dataSource, page, pageSize));
    }
  }, [dataSource]);


  // filter data by keyword, use custom filter
  const filterDataByKeyword = (data: T[], keyword: string): T[] => {
    return data?.filter((item) => {
      if (!item) {
        return true;
      }
      return searchItemFilter(item, keyword);
    });
  };

  // fetch data from api, filter data by url search keyword
  // set datasource => then set show datasource
  // set keyword
  const { data, loading } = useRequest(dataFetcher, {
    onSuccess: (data) => {
      const keyword = searchParams.get('keyword') || '';
      const filteredData = filterDataByKeyword(data, keyword);
      setDataSource(filteredData);
      setKeyword(keyword);
    },
  });

  // handle search submit
  // set search loading
  // set url query param
  // set keyword
  // set datasource => then set show datasource
  // set timeout then clos search loading
  const handleSearchSubmit = (keyword: string) => {
    setSearchLoading(true);
    setSearchParams({ keyword });
    setKeyword(keyword);
    const filteredData = filterDataByKeyword(data || [], keyword);
    setDataSource(filteredData);
    setTimeout(() => {
      setSearchLoading(false);
    }, 100);
  };


  // header content node
  const headContent = (
    <>
      {pageHeadContent}

      <div className={styles.searchContentContainer}>
        <Input.Search
          placeholder={intl.formatMessage({ id: searchPlaceholderI18nId })}
          enterButton={intl.formatMessage({ id: 'component.search.button-name' })}
          size="large"
          onSearch={handleSearchSubmit}
          className={styles.searchContent}
          value={keyword}
          onChange={handleInputChange}
        />
      </div>
    </>
  );

  return (
    <PageContainer content={headContent}>
      <div className={styles.cardList}>
        <List<T>
          rowKey={id}
          loading={loading || searchLoading}
          grid={{
            gutter: 16,
            xs: 1,
            sm: 2,
            md: 3,
            lg: 3,
            xl: 4,
            xxl: 4,
          }}
          dataSource={[...showDataSource]}
          renderItem={(item, index) => {
            return <List.Item key={index}>{renderItem && renderItem(item, index)}</List.Item>;
          }}
        />
      </div>

      <div className={styles.pagination}>
        <Pagination
          current={page}
          pageSize={initPageSize}
          onChange={handlePageChange}
          defaultCurrent={initPageNo}
          defaultPageSize={initPageSize}
          total={dataSource.length}
          showTotal={(total) => (
            <p style={{ transform: 'translateY(2px)' }}>
              {intl.formatMessage({ id: 'component.pagination.show-total' }, { total })}
            </p>
          )}
        />
      </div>
    </PageContainer>
  );
}

export default PageContent;

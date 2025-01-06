import {fullNameToShortName} from '@/utils/string_util';
import {Table, TableColumnsType, Tag, Tooltip} from 'antd';
import React from 'react';

import {parseModulePath} from '@/utils/path_util';
import {createStyles} from 'antd-style';
import {useIntl} from 'react-intl';
import {QuestionCircleOutlined, QuestionCircleTwoTone} from "@ant-design/icons";

const useStyle = createStyles(({css}) => {
  const antCls = 'ant';
  return {
    rowBiz: {
      backgroundColor: '#ffe7e7',
    },
    customTable: css`
      ${antCls}-table {
        ${antCls}-table-container {
          ${antCls}-table-body,
          ${antCls}-table-content {
            scrollbar-width: thin;
            scrollbar-color: #eaeaea transparent;
            scrollbar-gutter: stable;
          }
        }
      }
    `,
  };
});

export interface TableDataInfo {
  dataSource: TableDataType[];
  columns: TableColumnsType<TableDataType>;
}

export interface TableDataType {
  key: React.Key;
  code: string;
  priority: number;
  instanceType: 'business' | 'ability';

  [extensionFullName: string]: string | number | React.Key;
}

const buildBusinessUsedAbilitiesTableDatasource = (
  businessInfo: API.BusinessInfo,
  abilities: Array<API.AbilityInfo>,
  extensionPoints: Array<API.ExtensionPointInfo>,
  tableColumnExtImpl: string,
  tableColumnPriority: string,
  tableColumnExtType: string,
  extConflictNote: string,
): TableDataInfo => {
  const abilityMap = new Map<string, API.AbilityInfo>();
  abilities.forEach((abilityInfo) => {
    abilityMap.set(abilityInfo.code, abilityInfo);
  });
  const extensionPointMap = new Map<string, API.ExtensionPointInfo>();
  extensionPoints.forEach((extensionPointInfo) => {
    extensionPointMap.set(extensionPointInfo?.classInfo?.fullName || '', extensionPointInfo);
  });

  const columns: TableColumnsType<TableDataType> = [
    {
      title: tableColumnExtImpl,
      width: 100,
      dataIndex: 'code',
      key: 'code',
      fixed: 'left',
      ellipsis: true,
      render: (_, {code, instanceType}) => {
        return instanceType === 'business' ? (
          <Tooltip
            title={<pre>{businessInfo?.classInfo?.comment}</pre>}
            overlayStyle={{maxWidth: '800px'}}
          >
            <p>{code}</p>
          </Tooltip>
        ) : (
          <Tooltip
            title={<pre>{abilityMap.get(code)?.classInfo?.comment}</pre>}
            overlayStyle={{maxWidth: '800px'}}
          >
            <a
              href={parseModulePath('abilities', {
                keyword: 'code:' + code,
              })}
              target="_blank"
              rel="noreferrer"
            >
              {code}
            </a>
          </Tooltip>
        );
      },
    },
    {
      title: tableColumnPriority,
      width: 80,
      dataIndex: 'priority',
      key: 'priority',
      ellipsis: true,
    },
    {
      title: tableColumnExtType,
      width: 80,
      dataIndex: 'instanceType',
      key: 'instanceType',
      ellipsis: true,
      render: (_, {instanceType}) => (
        <Tag color={instanceType === 'business' ? 'volcano' : 'green'}>{instanceType}</Tag>
      ),
    },
  ];

  const dataSource: Array<TableDataType> = [];

  let businessInstance: TableDataType = {
    key: 'biz::' + businessInfo.code,
    code: businessInfo.code,
    priority: businessInfo.priority,
    instanceType: 'business',
  };

  const extensionNameSet = new Set<string>([]);

  businessInfo.implExtensionPoints.forEach((extFullName) => {
    businessInstance[extFullName] = '✅';
    if (!extensionNameSet.has(extFullName)) {
      extensionNameSet.add(extFullName);
    }
  });
  dataSource.push(businessInstance);

  businessInfo.usedAbilities?.forEach((usedAbility) => {
    const abilityInfo = abilityMap.get(usedAbility.abilityCode);
    let abilityInstance: TableDataType = {
      key: 'ability::' + usedAbility.abilityCode,
      code: usedAbility.abilityCode,
      priority: usedAbility.priority,
      instanceType: 'ability',
    };

    abilityInfo?.implExtensionPoints?.forEach((extFullName) => {
      abilityInstance[extFullName] = '✅';
      if (!extensionNameSet.has(extFullName)) {
        extensionNameSet.add(extFullName);
      }
    });
    dataSource.push(abilityInstance);
  });

  dataSource.sort((a, b) => a.priority - b.priority);

  const extensionNameList = Array.from(extensionNameSet);
  extensionNameList.sort((a, b) => {
    const aN = fullNameToShortName(a);
    const bN = fullNameToShortName(b);
    return aN === bN ? a.localeCompare(b) : aN.localeCompare(bN);
  });

  const rowNums = dataSource.length;
  const borderColor = '#ff6666';

  extensionNameList.forEach((fullName) => {
    let extInstanceCnt = 0;
    dataSource.forEach((d) => {
      if (d[fullName] === '✅') {
        extInstanceCnt += 1;
      }
    })

    const link = (
      <>
        <Tooltip
          title={<pre>{extensionPointMap.get(fullName)?.classInfo?.comment}</pre>}
          overlayStyle={{maxWidth: '800px'}}
        >
          <a
            href={parseModulePath('extension-points', {
              keyword: 'fullName:' + fullName,
            })}
            target="_blank"
            rel="noreferrer"
          >
            {fullNameToShortName(fullName)}
          </a>
        </Tooltip>

        {extInstanceCnt > 1 && (
          <Tooltip
            title={extConflictNote}
          >
            <QuestionCircleTwoTone style={{marginLeft: 5}} twoToneColor={borderColor}/>
          </Tooltip>
        )}
      </>
    );



    columns.push({
      title: link,
      width: 160,
      dataIndex: fullName,
      key: fullName,
      ellipsis: true,
      onCell: (data, index) => {
        return extInstanceCnt <= 1 ? {} : {
          style: {
            borderLeft: `1px solid ${borderColor}`, // 设置背景色
            borderRight: `1px solid ${borderColor}`, // 设置背景色
            borderBottom: index === rowNums - 1 ? `1px solid ${borderColor}` : '',
          }
        }
      },
      onHeaderCell: (data, index) => {
        return extInstanceCnt <= 1 ? {} : {
          style: {
            borderTop: `1px solid ${borderColor}`, // 设置背景色
            borderLeft: `1px solid ${borderColor}`, // 设置背景色
            borderRight: `1px solid ${borderColor}`, // 设置背景色
          }
        }
      },
    });
  });


  return {dataSource, columns};
};

interface ExtensionPointInfoTableProps {
  businessInfo: API.BusinessInfo;
  abilities: Array<API.AbilityInfo>;
  extensionPoints: Array<API.ExtensionPointInfo>;
}

const ExtensionPointInfoTable: React.FC<ExtensionPointInfoTableProps> = ({
                                                                           businessInfo,
                                                                           abilities,
                                                                           extensionPoints,
                                                                         }) => {
  const {styles} = useStyle();
  const intl = useIntl();
  const tableColumnExtImpl = intl.formatMessage({
    id: 'page.businesses.extension-point-info-table.column.ext-impl',
  });
  const tableColumnPriority = intl.formatMessage({
    id: 'page.businesses.extension-point-info-table.column.priority',
  });
  const tableColumnExtType = intl.formatMessage({
    id: 'page.businesses.extension-point-info-table.column.ext-type',
  });
  const extConflictNote = intl.formatMessage({
    id: 'page.businesses.extension-point-info-table.conflict.note',
  });
  const tableDataInfo = buildBusinessUsedAbilitiesTableDatasource(
    businessInfo,
    abilities,
    extensionPoints,
    tableColumnExtImpl,
    tableColumnPriority,
    tableColumnExtType,
    extConflictNote
  );

  const {columns, dataSource} = tableDataInfo;
  return (
    <>
      <div style={{marginBottom: 2, fontSize: 12, color: '#565656'}}>
        {intl.formatMessage({
          id: 'page.businesses.extension-point-info-table.desc',
        })}
      </div>
      <div style={{marginBottom: 10, fontSize: 12, color: '#565656'}}>
        {intl.formatMessage({
          id: 'page.businesses.extension-point-info-table.tip',
        })}
      </div>
      <Table<TableDataType>
        className={styles.customTable}
        pagination={false}
        columns={columns}
        dataSource={dataSource}
        scroll={{x: 'max-content'}}
        // rowClassName={(record) => record.instanceType === 'business'? styles.rowBiz : ''}
      />
    </>
  );
};

export default ExtensionPointInfoTable;

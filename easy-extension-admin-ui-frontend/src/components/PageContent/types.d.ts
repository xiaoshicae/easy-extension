import React from 'react';

interface PageContentProps<T = any> {
  pageHeadContent?: React.ReactNode;
  initPageNo?: number;
  initPageSize?: number;
  dataSource?: T[];
  id: (item: T) => React.Key;
  searchPlaceholderI18nId?: string;

  dataFetcher: (options?: { [key: string]: any }) => Promise<{ data: T[] }>;
  searchItemFilter: (item: T, keyword: string) => boolean;

  renderItem?: (item: T, index: number) => React.ReactNode;
}

export type { PageContentProps };

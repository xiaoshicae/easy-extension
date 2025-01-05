
const getPageData = <T>(data: Array<T>, page: number, pageSize: number): Array<any> => {
  if (data === undefined || data.length === 0) {
    return [];
  }
  const start = (page - 1) * pageSize;
  const end = start + pageSize;
  return data.slice(start, end);
};

export { getPageData };

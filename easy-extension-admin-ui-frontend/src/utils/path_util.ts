function parseModulePath(m: string, param: { [key: string]: string }): string {
  const currentPath = window.location.pathname;
  const pathParts = currentPath.split('/');
  const newPathParts = pathParts.slice(0, -1);
  newPathParts.push(m);

  const params = new URLSearchParams();
  Object.entries(param).forEach(([key, v]) => {
    params.set(key, v);
  });
  const queryString = params.toString();
  const baseUrl = newPathParts.join('/');
  return `${baseUrl}?${queryString}`;
}

export { parseModulePath };

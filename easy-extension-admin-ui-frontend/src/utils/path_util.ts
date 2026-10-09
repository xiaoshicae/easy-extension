/**
 * Link to another page of the admin UI, e.g. `parseModulePath('extension-points', { keyword: 'fullName:x.y.Z' })`.
 * The UI uses hash routing (config.ts `history.type = 'hash'`), so the route and its query go after `#`:
 * `<current index.html path>#/extension-points?keyword=...`. The server only ever serves index.html.
 */
function parseModulePath(m: string, param: { [key: string]: string }): string {
  const params = new URLSearchParams();
  Object.entries(param).forEach(([key, v]) => {
    params.set(key, v);
  });
  const queryString = params.toString();
  return `${window.location.pathname}#/${m}${queryString ? `?${queryString}` : ''}`;
}

export { parseModulePath };

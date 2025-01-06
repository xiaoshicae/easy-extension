const EASY_EXTENSION_ADMIN_UI = '/easy-extension-admin-ui/';

export function resolveBasePath(): string {
  const pathname = window.location.pathname;
  const index = pathname.indexOf(EASY_EXTENSION_ADMIN_UI);
  return pathname.substring(0, index);
}

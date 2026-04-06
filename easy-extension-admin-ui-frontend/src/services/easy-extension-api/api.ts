import { request } from '@umijs/max';
import { resolveBasePath } from './util';

/** 配置信息 */
export async function getConfigInfo(options?: { [key: string]: any }) {
  return request<{
    data: API.ConfigInfo;
  }>(resolveBasePath() + '/easy-extension-api/config-info', {
    method: 'GET',
    ...(options || {}),
  });
}

/** 扩展点实现生效条件的匹配参数 */
export async function getMatcherParamInfo(options?: { [key: string]: any }) {
  return request<{
    data: API.MatcherParamInfo;
  }>(resolveBasePath() + '/easy-extension-api/matcher-param', {
    method: 'GET',
    ...(options || {}),
  });
}

/** 扩展点默认实现 */
export async function getDefaultImplInfo(options?: { [key: string]: any }) {
  return request<{
    data: API.DefaultImplInfo;
  }>(resolveBasePath() + '/easy-extension-api/default-implementation', {
    method: 'GET',
    ...(options || {}),
  });
}

/** 刷新缓存 */
export async function refreshCache(options?: { [key: string]: any }) {
  return request<{
    data: null;
  }>(resolveBasePath() + '/easy-extension-api/cache/refresh', {
    method: 'POST',
    ...(options || {}),
  });
}

/** 扩展点列表 (支持分页) */
export async function getAllExtensionPoints(params?: { offset?: number; limit?: number }, options?: { [key: string]: any }) {
  return request<{
    data: Array<API.ExtensionPointInfo>;
  }>(resolveBasePath() + '/easy-extension-api/extension-points', {
    method: 'GET',
    params: { offset: 0, limit: 50, ...params },
    ...(options || {}),
  });
}

/** 能力列表 (支持分页) */
export async function getAllAbilities(params?: { offset?: number; limit?: number }, options?: { [key: string]: any }) {
  return request<{
    data: Array<API.AbilityInfo>;
  }>(resolveBasePath() + '/easy-extension-api/abilities', {
    method: 'GET',
    params: { offset: 0, limit: 50, ...params },
    ...(options || {}),
  });
}

/** 业务列表 (支持分页) */
export async function getAllBusiness(params?: { offset?: number; limit?: number }, options?: { [key: string]: any }) {
  return request<{
    data: Array<API.BusinessInfo>;
  }>(resolveBasePath() + '/easy-extension-api/businesses', {
    method: 'GET',
    params: { offset: 0, limit: 50, ...params },
    ...(options || {}),
  });
}

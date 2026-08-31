/* ============================================================================
 * api.js —— 统一的网络请求封装（全站唯一和后端打交道的地方）
 * ============================================================================
 * 小白理解：
 * 1. 浏览器和后端交换数据用的是 fetch 函数（浏览器自带）。但每个页面如果都
 *    直接写 fetch，就会到处重复“拼 URL、加请求头、解析 JSON、处理错误”。
 *    所以我们把这套流程集中封装在这里，页面代码只需要一行：
 *        const data = await api.get('/api/books', { pageNum: 1 });
 * 2. 后端所有的接口都返回一个“统一信封”：
 *        { code: 1, message: "success", data: xxx }   ← 成功（code=1）
 *        { code: 500, message: "书名不能为空", data: null } ← 失败
 *    本封装负责“拆信封”：成功时直接把里面的 data 返回给页面；
 *    失败时弹出错误提示并抛出异常（页面用 try/catch 接住）。
 * 3. 需要登录的接口要带请求头 Authorization: Bearer <token>，也在这里统一加，
 *    页面代码不用操心。
 *
 * ⚠ 两个后端特殊行为（这里集中处理，其他文件就不用再关心）：
 *    a) 未登录/token 过期时，后端拦截器返回的是 {"code":40101,"msg":"..."}
 *       ——注意错误信息在 msg 字段而不是 message 字段！
 *       我们遇到 40101/40102 就清除本地会话并跳到登录页。
 *    b) 所有 JSON 字段都是下划线命名（selling_price 这种），
 *       这是后端全局配置 Jackson SNAKE_CASE 决定的。
 * ========================================================================== */

import { getToken, clearSession } from './store.js';
import { toast } from './ui.js';

/**
 * 把参数对象拼成 URL 查询字符串。
 * 例如 { pageNum: 1, sortBy: 'updated_at desc' } → "pageNum=1&sortBy=updated_at%20desc"
 * 空值（null / undefined / 空字符串）的参数直接跳过，不拼进 URL。
 * 用 URLSearchParams 会自动做 URL 编码（中文、空格都没问题）。
 */
function buildQuery(params) {
  if (!params) return '';
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined || value === '') continue;
    search.append(key, value);
  }
  const str = search.toString();
  return str ? `?${str}` : '';
}

/**
 * 真正发请求的核心函数（一般不直接调用，用下面导出的 api.get 等快捷方法）。
 *
 * @param {string}  path   接口路径，如 '/api/books'
 * @param {object}  opts   配置项：
 *   - method: 'GET' | 'POST' | 'PUT' | 'DELETE'
 *   - params: 拼到 URL 上的查询参数对象（GET 请求的参数都走这里）
 *   - body:   请求体（JS 对象或字符串），会自动 JSON.stringify
 *   - silent: true 时失败不弹 toast（由调用方自己处理错误，比如管理后台探测权限）
 * @returns {Promise<any>} 成功时返回后端 data 字段的内容
 */
async function request(path, { method = 'GET', params, body, silent = false } = {}) {
  /* ---------- 第 1 步：准备请求头 ---------- */
  const headers = {};
  if (getToken()) {
    /* 带上通行证。后端拦截器兼容 "Bearer xxx" 格式 */
    headers['Authorization'] = `Bearer ${getToken()}`;
  }
  if (body !== undefined) {
    /* 发 JSON 时要告诉后端内容的类型 */
    headers['Content-Type'] = 'application/json';
  }

  /* ---------- 第 2 步：发出请求 ----------
     注意 body 的处理：JSON.stringify(字符串) 会得到 '"xxx"'（带引号的 JSON 字符串字面量），
     这正好满足后端“取消订单”接口的要求——它用 @RequestBody String 接收，
     期望请求体是一个 JSON 字符串而不是对象。传对象则正常得到对象 JSON。 */
  let res;
  try {
    res = await fetch(`${path}${buildQuery(params)}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    /* 走到这里一般是：后端没启动、断网、跨域被拦等“根本连不上”的情况 */
    if (!silent) toast('网络异常，请确认后端已启动', 'error');
    throw e;
  }

  /* ---------- 第 3 步：解析响应 JSON ---------- */
  let json;
  try {
    json = await res.json();
  } catch {
    /* 后端返回了非 JSON 内容（一般不会发生），当作未知错误 */
    if (!silent) toast('响应格式异常', 'error');
    throw new Error('响应格式异常');
  }

  /* ---------- 第 4 步：按“信封”约定处理 ---------- */

  /* 4.1 成功：code=1，直接把 data 拆出来返回 */
  if (json.code === 1) {
    return json.data;
  }

  /* 4.2 未登录 / token 过期（后端拦截器返回 40101/40102，错误信息在 msg 字段）。
     处理方式：清空本地登录状态，跳转到登录页，并记住当前页面地址，
     登录成功后可以跳回来。 */
  const msg = json.message || json.msg || '操作失败';
  if (json.code === 40101 || json.code === 40102) {
    clearSession();
    sessionStorage.setItem('redirect_after_login', location.hash);
    toast('请先登录', 'warning');
    location.hash = '#/login';
    const err = new Error(msg);
    err.code = json.code;
    throw err;
  }

  /* 4.3 其他业务错误（后端统一 code=500 + message；管理员校验是 40003 + msg）。
     默认弹提示；如果调用方声明 silent，就把带 code 的错误抛出去自己处理。 */
  if (!silent) toast(msg, 'error');
  const err = new Error(msg);
  err.code = json.code;
  throw err;
}

/* ---------------------------------------------------------------------------
 * 对外暴露的快捷方法（页面代码都用这几个）
 * ------------------------------------------------------------------------- */
export const api = {
  /** GET 请求：api.get('/api/books', { pageNum: 1 }) */
  get: (path, params, opts) => request(path, { method: 'GET', params, ...opts }),

  /** POST 请求：api.post('/api/cart', { book_id: 'xxx' }) */
  post: (path, body, opts) => request(path, { method: 'POST', body, ...opts }),

  /**
   * PUT 请求：api.put('/api/orders/3/confirm')
   * 也可以带查询参数：api.put('/api/users/me', undefined, { bio: 'xxx' })
   * 还可以传原始字符串作为请求体（取消订单接口需要 JSON 字符串字面量）：
   *     api.put('/api/orders/3/cancel', '不想要了')
   */
  put: (path, body, params, opts) => request(path, { method: 'PUT', body, params, ...opts }),

  /** DELETE 请求：api.del('/api/cart/xxx') */
  del: (path, params, opts) => request(path, { method: 'DELETE', params, ...opts }),

  /**
   * 上传图片（multipart/form-data 格式，后端 POST /api/upload/image）
   * @param {File} file 用户在 <input type="file"> 里选的文件
   * @returns {Promise<{id:string, url:string, thumbnail_url:string|null}>}
   *
   * 注意：用 FormData 时浏览器会自动设置带 boundary 的 Content-Type，
   * 我们【不能】手动设置 Content-Type，所以这里不走 request()，单独写。
   */
  async uploadImage(file) {
    const form = new FormData();     /* 表单数据容器 */
    form.append('file', file);       /* 后端用 @RequestParam("file") 接收，字段名必须叫 file */

    const res = await fetch('/api/upload/image', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${getToken()}` },
      body: form,
    });
    const json = await res.json();
    if (json.code !== 1) {
      toast(json.message || '图片上传失败', 'error');
      throw new Error(json.message || '图片上传失败');
    }
    return json.data;                /* { id, url, thumbnail_url } */
  },
};

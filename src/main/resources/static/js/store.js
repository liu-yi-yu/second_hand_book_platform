/* ============================================================================
 * store.js —— 会话管理（把“当前登录的用户”保存在浏览器里）
 * ============================================================================
 * 小白理解：
 * 1. 登录成功后，后端会发给我们一个 token（可以理解为一张“电子通行证”）。
 *    之后每次调用需要登录的接口，都要在请求头里带上它，后端才知道“你是谁”。
 * 2. token 存在浏览器的 localStorage 里——它的特点是：即使关掉浏览器再打开，
 *    数据还在（除非手动清除）。这样用户就不需要每次访问都重新登录。
 * 3. localStorage 只能存字符串，所以存对象时要 JSON.stringify（存）/
 *    JSON.parse（取）转换一下。
 * ========================================================================== */

/* 存储用的键名。加一个统一前缀，避免和浏览器里其他项目的数据撞名 */
const TOKEN_KEY = 'yutest1_token';
const USER_KEY  = 'yutest1_user';

/* ---------------------------- token 相关 ---------------------------- */

/** 保存 token（登录/注册成功时调用） */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

/** 读取 token。没登录时返回 null */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

/** 是否已登录：判断标准很简单——本地有没有 token */
export function isLoggedIn() {
  return !!getToken();   /* !! 把字符串转成 true/false */
}

/* ---------------------------- 用户信息相关 ---------------------------- */

/**
 * 保存当前用户的简要信息（id、用户名、头像、信誉分等）。
 * 有了它，顶栏就能直接显示头像和昵称，而不用每次都请求后端。
 */
export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user ?? {}));
}

/** 读取当前用户信息对象（没登录返回 null） */
export function getUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;   /* 万一数据损坏，当作未登录处理，避免报错崩溃 */
  }
}

/** 更新用户的某几个字段（例如改了头像后局部更新） */
export function patchUser(fields) {
  const user = getUser() ?? {};
  setUser({ ...user, ...fields });
}

/* ---------------------------- 退出登录 ---------------------------- */

/**
 * 清空本地会话（token + 用户信息）。
 * 注意：这只是“忘记”登录状态；后端的 token 本身仍然有效（后端没有做
 * token 作废接口），所以离开共用电脑时建议用户自行注意。
 */
export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

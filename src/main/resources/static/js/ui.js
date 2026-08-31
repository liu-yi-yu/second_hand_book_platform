/* ============================================================================
 * ui.js —— 通用 UI 工具箱（弹提示、弹窗、格式化、分页、顶栏渲染……）
 * ============================================================================
 * 小白理解：
 * 这里放的是“每个页面都会用到”的小工具。写一遍，到处 import。
 * 页面代码里常见的用法：
 *   import { toast, confirmDialog, formatDate, avatarHtml } from '../ui.js';
 *
 * 注意：本文件【刻意不 import api.js】，只依赖 store/constants，
 * 这样模块之间不会出现循环引用（A 引 B、B 又引 A 的死循环）。
 * ========================================================================== */

import { getUser, isLoggedIn } from './store.js';
import { creditInfo } from './constants.js';

/* ==========================================================================
 * 一、toast（右上角滑出的小提示条，2.8 秒后自动消失）
 * ======================================================================== */

/**
 * 弹一个提示。
 * @param {string} msg  提示文字
 * @param {'success'|'error'|'info'|'warning'} type 类型（决定颜色）
 */
export function toast(msg, type = 'info') {
  const box = document.getElementById('toast-container');
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = msg;                    /* textContent 防止把内容当 HTML 执行（防 XSS） */
  box.appendChild(el);
  setTimeout(() => el.remove(), 2800);     /* 2.8 秒后自动移除 */
}

/* ==========================================================================
 * 二、弹窗（modal）
 * ======================================================================== */

/**
 * 往页面根部插入一个弹窗，返回 { el, close }。
 * - el：弹窗的 DOM 元素（页面可以在里面塞表单等复杂内容）
 * - close()：关闭并移除弹窗
 */
export function openModal(innerHtml) {
  const container = document.getElementById('modal-container');
  const mask = document.createElement('div');
  mask.className = 'modal-mask';
  mask.innerHTML = `<div class="modal">${innerHtml}</div>`;
  container.appendChild(mask);
  return { el: mask.querySelector('.modal'), close: () => mask.remove() };
}

/**
 * 确认框：类似浏览器 confirm()，但更好看、可自定义按钮文字。
 * 返回 Promise：点了“确定”得到 true，点了“取消”或关闭得到 false。
 * 用法：if (await confirmDialog({ title:'下架书籍', message:'确定要下架吗？', danger:true })) {...}
 */
export function confirmDialog({ title = '确认操作', message = '', okText = '确定', cancelText = '取消', danger = false } = {}) {
  return new Promise(resolve => {
    const { el, close } = openModal(`
      <div class="modal-title">${escapeHtml(title)}</div>
      <div style="color:var(--text-light)">${escapeHtml(message)}</div>
      <div class="modal-footer">
        <button class="btn btn-outline" data-act="cancel">${escapeHtml(cancelText)}</button>
        <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-act="ok">${escapeHtml(okText)}</button>
      </div>
    `);
    el.querySelector('[data-act="ok"]').onclick = () => { close(); resolve(true); };
    el.querySelector('[data-act="cancel"]').onclick = () => { close(); resolve(false); };
  });
}

/**
 * 输入框弹窗：让用户输一段文字（例如“取消原因”“强制下架原因”）。
 * 返回 Promise：确定 → 输入的字符串（去首尾空格）；取消 → null。
 * required=true 时不填内容点确定会提示并不关闭。
 */
export function promptDialog({ title = '请输入', label = '', placeholder = '', required = false, multiline = true, okText = '确定' } = {}) {
  return new Promise(resolve => {
    const field = multiline
      ? `<textarea id="prompt-input" class="form-textarea" placeholder="${escapeHtml(placeholder)}"></textarea>`
      : `<input id="prompt-input" class="form-input" placeholder="${escapeHtml(placeholder)}" />`;
    const { el, close } = openModal(`
      <div class="modal-title">${escapeHtml(title)}</div>
      ${label ? `<div class="form-label">${escapeHtml(label)}</div>` : ''}
      ${field}
      <div class="modal-footer">
        <button class="btn btn-outline" data-act="cancel">取消</button>
        <button class="btn btn-primary" data-act="ok">${escapeHtml(okText)}</button>
      </div>
    `);
    el.querySelector('[data-act="cancel"]').onclick = () => { close(); resolve(null); };
    el.querySelector('[data-act="ok"]').onclick = () => {
      const value = el.querySelector('#prompt-input').value.trim();
      if (required && !value) { toast('内容不能为空', 'warning'); return; }
      close();
      resolve(value);
    };
  });
}

/* ==========================================================================
 * 三、格式化小工具
 * ======================================================================== */

/**
 * 转义 HTML 特殊字符：把 < > & " 转成安全实体。
 * 凡是把“用户输入的内容”插进 innerHTML 的地方，都必须先用它包一层，
 * 否则有人发一条 <script> 消息就能攻击其他用户（XSS 跨站脚本攻击）。
 */
export function escapeHtml(str) {
  return String(str ?? '')
    .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

/**
 * 格式化时间。
 * 后端返回的时间可能是 "2026-08-30T12:34:56.123"（ISO 格式，带 T）
 * 也可能是 "2026-08-30 12:34:56.0"（MySQL 直接转字符串），还经常是 null
 * （后端有些字段类型转换失败拿不到值）。这里统一成 "2026-08-30 12:34"，
 * 空值显示 "—"。
 */
export function formatDate(str) {
  if (!str) return '—';
  return String(str).replace('T', ' ').split('.')[0].slice(0, 16);
}

/** 金额显示：后端价格是字符串（如 "25.5"），统一格式化成 ¥25.50 */
export function money(v) {
  const n = Number(v);
  if (Number.isNaN(n)) return `¥${escapeHtml(v ?? '—')}`;
  return `¥${n.toFixed(2)}`;
}

/* ==========================================================================
 * 四、头像与封面
 * ======================================================================== */

/**
 * 生成头像 HTML。
 * 有图片 URL 就显示图片；没有就取用户名第一个字符画一个彩色圆圈。
 * @param {string} name  用户名（取首字符用）
 * @param {string} url   头像 URL，可为 null
 * @param {'avatar-sm'|'avatar-md'|'avatar-lg'} cls 尺寸类名
 */
export function avatarHtml(name, url, cls = 'avatar-md') {
  const first = escapeHtml((name || '?').charAt(0).toUpperCase());
  if (url) {
    /* 实现思路：文字头像先画好，图片绝对定位盖在上面；
       如果图片加载失败（onerror），就把 img 从 DOM 里移除，自然露出下面的文字头像 */
    return `<span class="avatar ${cls}">${first}
              <img src="${escapeHtml(url)}" alt="" style="position:absolute;inset:0"
                   onerror="this.remove()">
            </span>`;
  }
  return `<span class="avatar ${cls}">${first}</span>`;
}

/** 书籍封面占位图（内联 SVG，不需要任何图片文件）。
 *  ⚠ 后端目前不生成缩略图，thumbnail_url 恒为 null，所以大部分封面都会走占位图。 */
export const COVER_PLACEHOLDER =
  'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 400">
       <rect width="300" height="400" fill="#eef2ff"/>
       <rect x="60" y="70" width="180" height="260" rx="8" fill="#c7d2fe"/>
       <rect x="60" y="70" width="24" height="260" fill="#a5b4fc"/>
       <circle cx="210" cy="130" r="26" fill="#eef2ff"/>
       <text x="150" y="290" font-size="30" text-anchor="middle" fill="#6366f1" font-family="sans-serif">二手书</text>
     </svg>`
  );

/** 生成封面 <img>：有 URL 用 URL，没有就用占位图 */
export function coverHtml(url, cls = '', alt = '') {
  return `<img class="${cls}" src="${url ? escapeHtml(url) : COVER_PLACEHOLDER}" alt="${escapeHtml(alt)}">`;
}

/* ==========================================================================
 * 五、徽章（状态小标签）与星级
 * ======================================================================== */

/** 生成状态徽章：<span class="badge badge-green">在售</span> */
export function badge(text, color = 'gray') {
  return `<span class="badge badge-${color}">${escapeHtml(text)}</span>`;
}

/** 只读星级展示：renderStars(4) → ★★★★☆（前 4 个亮） */
export function renderStars(rating) {
  let html = '';
  for (let i = 1; i <= 5; i++) {
    html += `<span class="${i <= rating ? 'on' : ''}">★</span>`;
  }
  return `<span class="stars">${html}</span>`;
}

/**
 * 可交互的打星组件（写评价用）。
 * @param {HTMLElement} container 容器元素（内部会被填入 5 颗星）
 * @param {number} initial 初始星级
 * @param {(value:number)=>void} onPick 点选后的回调
 */
export function bindStarInput(container, initial = 5, onPick) {
  let value = initial;
  const render = () => {
    container.innerHTML = `<div class="star-input">${
      [1, 2, 3, 4, 5].map(i => `<span data-v="${i}" class="${i <= value ? 'on' : ''}">★</span>`).join('')
    }</div>`;
    container.querySelectorAll('span').forEach(star => {
      star.onclick = () => { value = Number(star.dataset.v); render(); onPick?.(value); };
    });
  };
  render();
  return () => value;   /* 返回一个“读取当前值”的小函数 */
}

/* ==========================================================================
 * 六、分页条
 * ======================================================================== */

/**
 * 渲染分页按钮。
 * @param {HTMLElement} el     分页条容器
 * @param {number} page        当前页码（从 1 开始）
 * @param {number} totalPages  总页数
 * @param {(newPage:number)=>void} onChange 点了某页后的回调
 *
 * 说明：后端 PageVO 只返回 records 和 total，所以总页数要前端自己算：
 *      totalPages = Math.ceil(total / pageSize)
 */
export function renderPagination(el, page, totalPages, onChange) {
  if (!el) return;
  if (!totalPages || totalPages <= 1) { el.innerHTML = ''; return; }

  /* 只显示当前页附近的一小段页码，避免几十页把页面撑爆 */
  const start = Math.max(1, page - 2);
  const end = Math.min(totalPages, start + 4);
  let html = `<button class="page-btn" data-p="${page - 1}" ${page <= 1 ? 'disabled' : ''}>上一页</button>`;
  for (let p = start; p <= end; p++) {
    html += `<button class="page-btn ${p === page ? 'current' : ''}" data-p="${p}">${p}</button>`;
  }
  html += `<button class="page-btn" data-p="${page + 1}" ${page >= totalPages ? 'disabled' : ''}>下一页</button>`;
  el.innerHTML = html;

  el.querySelectorAll('.page-btn').forEach(btn => {
    btn.onclick = () => {
      const p = Number(btn.dataset.p);
      if (p >= 1 && p <= totalPages && p !== page) onChange(p);
    };
  });
}

/** 空状态占位块（列表没数据时显示） */
export function emptyBlock(text = '暂无数据', icon = '📭') {
  return `<div class="empty"><div class="icon">${icon}</div>${escapeHtml(text)}</div>`;
}

/** 加载中占位块 */
export function loadingBlock(text = '加载中…') {
  return `<div class="loading">${escapeHtml(text)}</div>`;
}

/** 防抖：连续触发时只执行最后一次（搜索框输入用）。
 *  比如 300ms 内打了 5 个字，只会在停顿后发 1 次搜索请求。 */
export function debounce(fn, wait = 300) {
  let timer = null;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
}

/* ==========================================================================
 * 七、顶部导航栏渲染
 * ========================================================================== */

/**
 * 根据登录状态和当前路由，重新绘制顶部导航栏。
 * 什么时候调用：程序启动、每次路由切换、登录/注册成功、退出登录。
 */
export function renderNavbar() {
  const bar = document.getElementById('navbar');
  const user = getUser();
  const hash = location.hash || '#/';
  const path = hash.split('?')[0];   /* 去掉查询部分再比较，保证高亮判断准确 */

  /* 判断某个导航项是否处于“当前页面”状态（前缀匹配） */
  const active = prefix => (path === prefix || path.startsWith(prefix + '/') ? ' active' : '');

  /* 左侧主导航（登录后才显示需要登录的入口） */
  const links = `
    <span class="nav-link${active('#/')}" data-nav="#/">🏠 首页</span>
    ${isLoggedIn() ? `
      <span class="nav-link${active('#/cart')}" data-nav="#/cart">🛒 购物车</span>
      <span class="nav-link${active('#/orders')}" data-nav="#/orders">📋 我的订单
        <i class="nav-badge" id="badge-chat" style="display:none">0</i>
      </span>
      <span class="nav-link${active('#/notifications')}" data-nav="#/notifications">🔔 通知
        <i class="nav-badge" id="badge-notification" style="display:none">0</i>
      </span>
      <span class="nav-link${active('#/admin')}" data-nav="#/admin">🛡️ 管理后台</span>
    ` : ''}
  `;

  /* 右侧用户区：已登录显示头像+昵称+退出；未登录显示登录/注册按钮 */
  const userArea = isLoggedIn() && user ? `
    <span class="nav-user">
      ${avatarHtml(user.username, user.avatar || user.avatar_url, 'avatar-sm')}
      <span class="nav-username" data-nav="#/profile">${escapeHtml(user.username)}</span>
      <button class="btn btn-outline btn-sm" id="logout-btn">退出</button>
    </span>
  ` : `
    <span class="nav-user">
      <button class="btn btn-outline btn-sm" data-nav="#/login">登录</button>
      <button class="btn btn-primary btn-sm" data-nav="#/register">注册</button>
    </span>
  `;

  bar.innerHTML = `
    <div class="navbar-inner">
      <span class="nav-logo" data-nav="#/">📚 校园二手书</span>
      <nav class="nav-links">${links}</nav>
      ${userArea}
    </div>
  `;

  /* 统一处理点击跳转：所有带 data-nav 属性的元素，点了就切换 hash */
  bar.querySelectorAll('[data-nav]').forEach(item => {
    item.onclick = () => { location.hash = item.dataset.nav; };
  });

  /* 退出登录：清会话 → 断开聊天连接 → 回登录页 */
  const logoutBtn = bar.querySelector('#logout-btn');
  if (logoutBtn) {
    logoutBtn.onclick = () => {
      clearSessionAndGo();
    };
  }
}

/**
 * 退出登录的完整动作。
 * 动态 import ws.js 避免循环依赖（ws.js 不依赖 ui.js，这里其实可以静态引，
 * 但动态引入的好处是：即使将来 ws.js 反过来引用了 ui.js 也不会出问题）。
 */
export async function clearSessionAndGo() {
  const { clearSession } = await import('./store.js');
  const { disconnectChat } = await import('./ws.js');
  disconnectChat();
  clearSession();
  renderNavbar();
  location.hash = '#/login';
  toast('已退出登录', 'info');
}

/**
 * 更新顶栏的小红点徽标。
 * @param {'notification'|'chat'} which 哪个徽标
 * @param {number} count 数量（0 时隐藏）
 */
export function setNavBadge(which, count) {
  const el = document.getElementById(`badge-${which}`);
  if (!el) return;
  el.textContent = count > 99 ? '99+' : String(count);
  el.style.display = count > 0 ? '' : 'none';
}

/* ============================================================================
 * router.js —— hash 路由器（页面切换的“总调度”）
 * ============================================================================
 * 小白理解：
 * 1. 单页应用里“切换页面”= 改变地址栏 # 后面的部分 + 往 #app 容器里画新内容。
 *    本文件就做这件事：
 *      - 维护一张“地址 → 页面(视图函数)”的对照表（下面的 routes）
 *      - 监听 hashchange 事件：地址一变，找到对应视图，调用它的 render()
 *      - 登录守卫：标了 auth:true 的页面，没登录就踢去登录页
 * 2. 路由参数：'#/books/abc123' 里那个 abc123 用 ':id' 占位符匹配，
 *    匹配到后以 params.id 传给视图。
 * 3. 想跳转页面？不需要 import 本文件，直接改地址即可：
 *        location.hash = '#/orders';     // 触发 hashchange → 自动渲染订单页
 * ========================================================================== */

import { isLoggedIn, getToken } from './store.js';
import { renderNavbar, toast } from './ui.js';
import { connectChat } from './ws.js';

/* ---------------- 各页面的视图函数（views 目录，每页一个文件） ---------------- */
import { renderAuth }      from './views/auth.js';
import { renderHome }      from './views/home.js';
import { renderBookDetail }from './views/bookDetail.js';
import { renderPublish }   from './views/publish.js';
import { renderCart }      from './views/cart.js';
import { renderOrders }    from './views/orders.js';
import { renderOrderChat } from './views/orderChat.js';
import { renderProfile }   from './views/profile.js';
import { renderUserProfile } from './views/userProfile.js';
import { renderNotifications } from './views/notifications.js';
import { renderAdmin }     from './views/admin.js';

/* ---------------------------------------------------------------------------
 * 路由表。
 *  - path：'#' 开头的地址，':' 开头的段是参数占位符
 *  - view：该地址对应的渲染函数（接收 (容器, 参数对象)）
 *  - auth：true 表示必须登录才能访问
 * 注意：'#/books/:id/edit' 必须放在 '#/books/:id' 前面，
 *      否则更短的路由会先把 'edit' 当成别的参数匹配走。
 * ------------------------------------------------------------------------- */
const routes = [
  { path: '#/',                 view: renderHome,          auth: false },
  { path: '#/login',            view: (el) => renderAuth(el, 'login'),    auth: false },
  { path: '#/register',         view: (el) => renderAuth(el, 'register'), auth: false },
  { path: '#/books/:id/edit',   view: renderPublish,       auth: true  },
  { path: '#/books/:id',        view: renderBookDetail,    auth: false },
  { path: '#/publish',          view: renderPublish,       auth: true  },
  { path: '#/cart',             view: renderCart,          auth: true  },
  { path: '#/orders/:id',       view: renderOrderChat,     auth: true  },
  { path: '#/orders',           view: renderOrders,        auth: true  },
  { path: '#/profile',          view: renderProfile,       auth: true  },
  { path: '#/users/:id',        view: renderUserProfile,   auth: false },
  { path: '#/notifications',    view: renderNotifications, auth: true  },
  { path: '#/admin/:tab',       view: renderAdmin,         auth: true  },
  { path: '#/admin',            view: renderAdmin,         auth: true  },
];

/**
 * 把当前 hash 与路由表逐条匹配。
 * @returns {null | {route, params}} 匹配结果（没匹配到返回 null）
 */
function matchRoute() {
  /* 取地址栏 hash，例如 '#/books/abc?x=1'。
     没有 hash（第一次打开 http://localhost:8081/）时当作 '#/'。 */
  let raw = location.hash || '#/';
  if (!raw.startsWith('#')) raw = '#' + raw;
  /* 去掉 hash 里的查询部分（'#/orders?x=1' → '#/orders'），并统一去尾斜杠。
     注意 '#/' 本身不能裁（裁完变成 '#' 就匹配不到首页了），所以长度大于 2 才裁。 */
  let path = raw.split('?')[0];
  if (path.length > 2 && path.endsWith('/')) path = path.slice(0, -1);

  const currentSegs = path.split('/');          /* ['#', 'books', 'abc'] */

  for (const route of routes) {
    const routeSegs = route.path.split('/');    /* ['#', 'books', ':id'] */
    if (routeSegs.length !== currentSegs.length) continue;   /* 段数不同肯定不匹配 */

    const params = {};
    let ok = true;
    for (let i = 0; i < routeSegs.length; i++) {
      if (routeSegs[i].startsWith(':')) {
        /* 占位符：捕获当前段的值，例如 :id ← abc */
        params[routeSegs[i].slice(1)] = decodeURIComponent(currentSegs[i]);
      } else if (routeSegs[i] !== currentSegs[i]) {
        ok = false;                             /* 普通段必须完全一致 */
        break;
      }
    }
    if (ok) return { route, params };
  }
  return null;
}

/** 当前渲染函数可能注册的“离开页面时要做的清理动作”（聊天页注销监听器用） */
let viewCleanup = null;

/** 供视图注册清理函数：路由切换前会先调用它 */
export function setViewCleanup(fn) { viewCleanup = fn; }

/** 手动执行一次路由渲染（程序启动时调用一次；之后都靠 hashchange 事件驱动） */
export function renderCurrentView() {
  const app = document.getElementById('app');

  /* 1) 离开旧页面前执行清理（比如注销 WebSocket 监听器） */
  if (viewCleanup) { try { viewCleanup(); } catch { /* 忽略 */ } viewCleanup = null; }

  /* 2) 匹配路由 */
  const matched = matchRoute();

  /* 3) 顶栏重新渲染（更新高亮/登录态/头像） */
  renderNavbar();

  /* 4) 没匹配到 → 显示 404 */
  if (!matched) {
    app.innerHTML = `
      <div class="empty">
        <div class="icon">🧭</div>
        <p>页面不存在：${location.hash}</p>
        <p><a href="#/">回到首页</a></p>
      </div>`;
    return;
  }

  /* 5) 登录守卫：需要登录但本地没有 token → 去登录页（记住来源，登录后跳回） */
  if (matched.route.auth && !isLoggedIn()) {
    sessionStorage.setItem('redirect_after_login', location.hash);
    toast('请先登录', 'warning');
    location.hash = '#/login';
    return;
  }

  /* 6) 顺手保证聊天长连接在线（登录状态下进任何页面都尝试连接，
        重复调用不会建第二条连接，ws.js 里做了判断） */
  if (isLoggedIn()) connectChat(getToken());

  /* 7) 调用视图渲染。render 内部自己 fetch 数据，所以是 async ——
        这里故意不 await，让页面先显示“加载中”骨架，数据到了再填充 */
  matched.route.view(app, matched.params).catch(err => {
    /* 视图内部一般已 toast 过错误；这里兜底画个错误占位，避免白屏 */
    if (err && err.code !== 40101 && err.code !== 40102) {
      app.innerHTML = `<div class="empty"><div class="icon">😵</div>
        <p>页面加载失败：${(err && err.message) || '未知错误'}</p></div>`;
    }
  });
}

/** 启动路由器：立即渲染一次 + 监听以后的变化 */
export function initRouter() {
  renderCurrentView();
  window.addEventListener('hashchange', renderCurrentView);
}

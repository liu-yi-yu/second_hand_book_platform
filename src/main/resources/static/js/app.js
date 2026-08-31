/* ============================================================================
 * app.js —— 程序入口（index.html 最后加载的就是它）
 * ============================================================================
 * 小白理解：
 * 这个文件做四件事：
 *   1. 启动路由器（根据当前地址渲染出第一个页面）
 *   2. 已登录的话：连上聊天 WebSocket + 拉一次未读徽标
 *   3. 每 30 秒自动刷新一次顶栏的未读数（通知未读 + 聊天未读）
 *   4. 全局监听“收到新消息”：不在聊天页时弹提示、刷新徽标
 * 其它业务逻辑一概不写，都分给 api/ui/ws/router/views 各自的文件。
 * ========================================================================== */

import { isLoggedIn, getToken } from './store.js';
import { setNavBadge, renderNavbar, toast } from './ui.js';
import { initRouter } from './router.js';
import { connectChat, onNewMessage } from './ws.js';
import { api } from './api.js';

/* ---------------------------------------------------------------------------
 * 徽标刷新：问后端两个数字，更新顶栏红点
 *   - 通知未读数：GET /api/notifications → data.unread_count
 *   - 聊天未读数：GET /api/messages/unread-count → data.count
 * 挂到 window 上是为了让各个视图文件不用 import app.js（避免循环引用）
 * 也能调用：window.refreshBadges()
 * ------------------------------------------------------------------------- */
export async function refreshBadges() {
  if (!isLoggedIn()) return;

  /* 两个请求互不依赖，用 Promise.allSettled 并行发、谁失败都不影响谁 */
  const [notiRes, chatRes] = await Promise.allSettled([
    api.get('/api/notifications', { page: 1, pageSize: 1 }, { silent: true }),
    api.get('/api/messages/unread-count', null, { silent: true }),
  ]);

  /* 通知未读：后端返回 { page_vo:{records,total}, unread_count } */
  if (notiRes.status === 'fulfilled' && notiRes.value) {
    setNavBadge('notification', notiRes.value.unread_count ?? 0);
  }
  /* 聊天未读：后端返回 { count, by_orders:[...] } */
  if (chatRes.status === 'fulfilled' && chatRes.value) {
    setNavBadge('chat', chatRes.value.count ?? 0);
  }
}
window.refreshBadges = refreshBadges;   /* 提供给视图文件调用 */

/* ---------------------------------------------------------------------------
 * 全局“收到新消息”处理：
 * 聊天页（orderChat.js）自己会注册监听器，把气泡画进聊天窗口；
 * 这里负责“聊天页之外”的部分——弹个提示、刷新徽标。
 * 用 window.__activeChatOrderId 区分：聊天页渲染时会把当前订单 ID 写在这个
 * 全局变量上，离开时清掉。
 * ------------------------------------------------------------------------- */
onNewMessage(message => {
  refreshBadges();
  const active = window.__activeChatOrderId;
  if (!active || String(active) !== String(message.order_id)) {
    toast(`💬 ${message.sender_name || '有人'}给你发来新消息`, 'info');
  }
});

/* ---------------------------------------------------------------------------
 * 启动！
 * ------------------------------------------------------------------------- */
renderNavbar();      /* 先画一次顶栏（之后每次路由切换 router.js 会重画） */
initRouter();        /* 根据当前地址渲染第一个页面，并开始监听 hash 变化 */

/* 已登录：连接聊天 + 立刻拉一次徽标，之后每 30 秒刷新 */
if (isLoggedIn()) {
  connectChat(getToken());
  refreshBadges();
  setInterval(refreshBadges, 30000);
}

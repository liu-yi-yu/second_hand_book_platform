/* ============================================================================
 * views/notifications.js —— 通知中心
 * ============================================================================
 * 对应后端接口：
 *   GET /api/notifications?page=1&pageSize=20
 *       返回（PageNotificationVO，结构和普通分页不一样！）：
 *         { page_vo: { records: [通知], total }, unread_count: 数字 }
 *       通知字段：{ id(整数), type, title, content, is_read,
 *                  related_order_id, related_book_id, created_at }
 *       ⚠ 后端默认只查询【未读】的通知（isRead=0 过滤），
 *         所以这个页面实际是“未读收件箱”，看完一条它会消失。
 *       ⚠ title 字段是必有的；content 可能为 null（新消息类通知后端没填）。
 *
 *   PUT /api/notifications/read    body { ids: [通知ID] } 或 {}（全部已读）
 * ========================================================================== */

import { api } from '../api.js';
import { NOTIFICATION_ICON, notificationLabel } from '../constants.js';
import { escapeHtml, formatDate, emptyBlock, loadingBlock, renderPagination } from '../ui.js';

const PAGE_SIZE = 15;
let page = 1;      /* 当前页码（模块级，切换页面回来不丢） */

/**
 * 渲染通知页
 * @param {HTMLElement} app 页面容器
 */
export async function renderNotifications(app) {
  app.innerHTML = loadingBlock();

  await load(app);

  async function load() {
    let data;
    try {
      data = await api.get('/api/notifications', { page, pageSize: PAGE_SIZE });
    } catch {
      app.innerHTML = emptyBlock('通知加载失败', '⚠️');
      return;
    }

    /* 拆开双层结构：{ page_vo:{records,total}, unread_count } */
    const records = data?.page_vo?.records ?? [];
    const unreadCount = data?.unread_count ?? 0;

    app.innerHTML = `
      <div class="page-title">🔔 通知
        <span style="font-size:13px;color:var(--text-light);font-weight:400">未读 ${unreadCount} 条</span>
        ${records.length > 0 ? `<button class="btn btn-outline btn-sm" style="margin-left:12px" id="read-all">全部标记已读</button>` : ''}
      </div>

      <div class="alert alert-info">
        提示：点击某条通知会自动标记为已读，并跳转到对应的订单/书籍。
      </div>

      <div id="noti-list">
        ${records.length === 0
          ? emptyBlock('没有未读通知，所有消息都处理完啦', '🎉')
          : records.map(renderItem).join('')}
      </div>
      <div class="pagination" id="pager"></div>
    `;

    /* ---------------- 交互 ---------------- */

    /* 全部已读：后端约定 body 传 {}（或不带 ids）时，把当前用户所有未读都标为已读 */
    const readAllBtn = document.getElementById('read-all');
    if (readAllBtn) {
      readAllBtn.onclick = async () => {
        try {
          const affected = await api.put('/api/notifications/read', {});
          toast(`已标记 ${affected ?? 0} 条为已读`, 'success');
          window.refreshBadges?.();
          page = 1;
          load();     /* 后端只返回未读 → 全部已读后列表自然清空 */
        } catch { /* 错误已提示 */ }
      };
    }

    /* 点击单条通知：标记已读 → 跳转到关联的订单或书籍 */
    app.querySelectorAll('.noti-item').forEach(item => {
      item.onclick = async () => {
        const { id, order_id, book_id } = item.dataset;
        /* 后端 updateNotifications 的 SQL 是 “where is_read=0 and id in (...)”，
           传已读过的 id 也不会出错，可以放心调用 */
        api.put('/api/notifications/read', { ids: [Number(id)] })
          .then(() => window.refreshBadges?.())
          .catch(() => {});

        if (order_id)       location.hash = `#/orders/${order_id}`;    /* 订单详情+聊天 */
        else if (book_id)   location.hash = `#/books/${book_id}`;      /* 书籍详情 */
        else load();   /* 没有关联对象就只是标记已读，刷新列表 */
      };
    });

    /* 分页 */
    renderPagination(
      document.getElementById('pager'),
      page,
      Math.ceil((data?.page_vo?.total ?? 0) / PAGE_SIZE),
      p => { page = p; load(); }
    );
  }

  /** 单条通知 */
  function renderItem(n) {
    return `
      <div class="noti-item unread"
           data-id="${n.id}"
           data-order-id="${n.related_order_id ?? ''}"
           data-book-id="${escapeHtml(n.related_book_id ?? '')}">
        <div class="noti-icon">${NOTIFICATION_ICON[n.type] ?? '📢'}</div>
        <div style="flex:1;min-width:0">
          <div class="title">${escapeHtml(n.title || notificationLabel(n.type))}</div>
          <div class="content">${escapeHtml(n.content || notificationLabel(n.type))}</div>
        </div>
        <span class="time">${formatDate(n.created_at)}</span>
        <span class="dot-unread"></span>
      </div>
    `;
  }
}

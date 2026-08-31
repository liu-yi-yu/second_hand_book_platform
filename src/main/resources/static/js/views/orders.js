/* ============================================================================
 * views/orders.js —— 我的订单列表（我买的 / 我卖的）
 * ============================================================================
 * 对应后端接口：
 *   GET /api/orders?role=buyer|seller&status=xxx&pageNum=1&pageSize=20
 *       - role：buyer=我买的；seller=我卖的（必传）
 *       - status：可逗号分隔多个（本页只单选一个或全部）
 *       - ⚠ 列表为空时后端返回 data=null（不是空 records），必须兜底！
 *       每项（OrderListVO）：
 *         { id(整数), book_id, book_title, book_cover_image, amount,
 *           status, counterparty_name, counterparty_avatar, created_at,
 *           confirmed_at, shipped_at, received_at, completed_at }
 *         ⚠ created_at 可能是 null（后端类型转换缺失），显示 "—"
 *         ⚠ 没有 buyer/seller 完整对象，只有“对方(counterparty)”信息，
 *           所以操作按钮需要按当前 tab 的角色判断。
 *
 *   订单操作（都是 PUT，无请求体）：
 *     PUT /api/orders/{id}/confirm   卖家确认（仅 pending）
 *     PUT /api/orders/{id}/ship      卖家发货（仅 confirmed）
 *     PUT /api/orders/{id}/receive   买家收货（仅 shipped）
 *     PUT /api/orders/{id}/cancel    取消（仅 pending）⚠ 请求体是 JSON 字符串！
 *
 *   评价：POST /api/reviews  body { order_id, rating, content }
 *         ⚠ 仅订单状态为 received 时后端才允许评价
 * ========================================================================== */

import { api } from '../api.js';
import { getUser } from '../store.js';
import {
  ORDER_STATUS_MAP, ORDER_STATUS_COLOR, orderStatusLabel,
  ORDER_FLOW,
} from '../constants.js';
import {
  coverHtml, escapeHtml, formatDate, badge, toast, money,
  confirmDialog, promptDialog, openModal, bindStarInput,
  renderPagination, emptyBlock, loadingBlock,
} from '../ui.js';

const PAGE_SIZE = 10;

/* 记住用户停留在哪个 tab / 哪个状态筛选 / 第几页（切换页面回来不丢） */
const state = { role: 'buyer', status: '', page: 1 };

/**
 * 渲染订单列表页
 * @param {HTMLElement} app 页面容器
 */
export async function renderOrders(app) {
  app.innerHTML = loadingBlock();

  /* ---------------- 1. 骨架：角色 tab + 状态筛选 + 列表 + 分页 ---------------- */
  app.innerHTML = `
    <div class="page-title">📋 我的订单</div>

    <div class="card">
      <div class="filter-bar">
        <div class="admin-tabs" style="margin:0">
          <span class="admin-tab ${state.role === 'buyer' ? 'active' : ''}" data-role="buyer">我买的</span>
          <span class="admin-tab ${state.role === 'seller' ? 'active' : ''}" data-role="seller">我卖的</span>
        </div>

        <select class="form-select" id="f-status">
          <option value="">全部状态</option>
          ${Object.entries(ORDER_STATUS_MAP)
            .map(([v, label]) => `<option value="${v}" ${state.status === v ? 'selected' : ''}>${label}</option>`)
            .join('')}
        </select>
      </div>
    </div>

    <div class="card"><div id="order-list"></div><div class="pagination" id="pager"></div></div>
  `;

  /* tab 切换 */
  document.querySelectorAll('[data-role]').forEach(tab => {
    tab.onclick = () => {
      state.role = tab.dataset.role;
      state.page = 1;
      renderOrders(app);       /* 重新渲染整个页面（tab 高亮也要更新） */
    };
  });

  /* 状态筛选 */
  document.getElementById('f-status').onchange = e => {
    state.status = e.target.value;
    state.page = 1;
    loadList();
  };

  await loadList();

  /* ------------------------------------------------------------------------
   * 内部：加载订单列表
   * ---------------------------------------------------------------------- */
  async function loadList() {
    const box = document.getElementById('order-list');
    box.innerHTML = loadingBlock();

    let data;
    try {
      data = await api.get('/api/orders', {
        role: state.role,
        status: state.status || undefined,
        pageNum: state.page,
        pageSize: PAGE_SIZE,
      });
    } catch {
      box.innerHTML = emptyBlock('加载失败', '⚠️');
      return;
    }

    /* ⚠ 后端查不到订单时返回 null，这里必须兜底成空数组 */
    const records = data?.records ?? [];
    const total = data?.total ?? 0;

    if (records.length === 0) {
      box.innerHTML = emptyBlock('暂无订单', state.role === 'buyer' ? '🛍️' : '📦');
      document.getElementById('pager').innerHTML = '';
      return;
    }

    box.innerHTML = `<div class="table-wrap"><table class="table">
      <thead><tr>
        <th>书籍</th><th>金额</th>
        <th>${state.role === 'buyer' ? '卖家' : '买家'}</th>
        <th>状态</th><th>下单时间</th><th style="width:230px">操作</th>
      </tr></thead>
      <tbody>
        ${records.map(o => renderRow(o)).join('')}
      </tbody>
    </table></div>`;

    /* 操作按钮绑定（按钮的生成逻辑在下面的共享函数里，订单详情页也复用） */
    records.forEach(o => {
      const container = box.querySelector(`[data-actions="${o.id}"]`);
      bindOrderActions(o, state.role, container, () => loadList());
    });

    /* 点击书名/封面 → 订单详情+聊天页 */
    box.querySelectorAll('[data-order]').forEach(row => {
      row.style.cursor = 'pointer';
      row.onclick = e => {
        /* 点在操作按钮上时不跳转（按钮有自己的 onclick，且事件会冒泡上来） */
        if (e.target.closest('button')) return;
        location.hash = `#/orders/${row.dataset.order}`;
      };
    });

    renderPagination(
      document.getElementById('pager'),
      state.page,
      Math.ceil(total / PAGE_SIZE),
      p => { state.page = p; loadList(); }
    );
  }

  /** 表格的一行 */
  function renderRow(o) {
    return `
      <tr data-order="${o.id}">
        <td><div class="book-cell">
          ${coverHtml(o.book_cover_image, '', o.book_title)}
          <div>
            <div class="t">${escapeHtml(o.book_title ?? '—')}</div>
            <div style="color:var(--text-light);font-size:12px">订单号 #${o.id}</div>
          </div>
        </div></td>
        <td><b style="color:var(--danger)">${money(o.amount)}</b></td>
        <td>${escapeHtml(o.counterparty_name ?? '—')}</td>
        <td>${badge(orderStatusLabel(o.status), ORDER_STATUS_COLOR[o.status] ?? 'gray')}</td>
        <td>${formatDate(o.created_at)}</td>
        <td class="actions" data-actions="${o.id}"></td>
      </tr>
    `;
  }
}

/* ==========================================================================
 * 【共享】订单操作按钮 —— 订单列表页和订单详情页（聊天页）都使用
 * ========================================================================== */

/**
 * 生成“角色 × 状态”允许的操作按钮，并绑定事件。
 *
 * 按钮规则（与后端 upStatus 的校验一一对应，后端不允许的按钮干脆不显示）：
 *   卖家 + pending   → [确认订单] [取消订单]
 *   卖家 + confirmed → [发货]
 *   买家 + pending   → [取消订单]
 *   买家 + shipped   → [确认收货]
 *   双方 + received  → [评价]（后端只允许 received 状态评价）
 *   所有状态         → [详情/聊天] 由列表行的点击跳转承担，这里不重复画
 *
 * @param {object}  order     订单数据（OrderListVO 或 OrderVO 均可，需要 id/status）
 * @param {'buyer'|'seller'} role 当前用户在这笔订单里的角色
 * @param {HTMLElement} container 按钮插入的容器
 * @param {()=>void} refresh 操作成功后的刷新回调
 */
export function bindOrderActions(order, role, container, refresh) {
  if (!container) return;
  const id = order.id;
  let html = '';

  if (role === 'seller' && order.status === 'pending') {
    html += `<button class="btn btn-primary btn-sm" data-op="confirm">确认订单</button>`;
    html += `<button class="btn btn-outline btn-sm" data-op="cancel">取消</button>`;
  }
  if (role === 'seller' && order.status === 'confirmed') {
    html += `<button class="btn btn-primary btn-sm" data-op="ship">发货</button>`;
  }
  if (role === 'buyer' && order.status === 'pending') {
    html += `<button class="btn btn-outline btn-sm" data-op="cancel">取消订单</button>`;
  }
  if (role === 'buyer' && order.status === 'shipped') {
    html += `<button class="btn btn-primary btn-sm" data-op="receive">确认收货</button>`;
  }
  if (order.status === 'received') {
    /* 双方都可以评价；后端会拒绝重复评价（每单每人一次） */
    html += `<button class="btn btn-outline btn-sm" data-op="review">⭐ 评价</button>`;
  }

  container.innerHTML = html || `<span style="color:var(--text-light);font-size:12px">暂无操作</span>`;

  /* ---- 各按钮的事件 ---- */
  container.querySelectorAll('button[data-op]').forEach(btn => {
    btn.onclick = async e => {
      e.stopPropagation();          /* 别触发行点击跳详情 */
      const op = btn.dataset.op;

      try {
        if (op === 'confirm') {
          /* 卖家确认订单 */
          const ok = await confirmDialog({
            title: '确认订单',
            message: '确认后请在 72 小时内发货，超时订单会被自动取消并扣除信誉分。',
          });
          if (!ok) return;
          await api.put(`/api/orders/${id}/confirm`);
          toast('已确认订单，请尽快发货', 'success');

        } else if (op === 'ship') {
          /* 卖家发货 */
          const ok = await confirmDialog({ title: '发货', message: '确认已经把书交给买家了吗？' });
          if (!ok) return;
          await api.put(`/api/orders/${id}/ship`);
          toast('已发货，等待买家收货', 'success');

        } else if (op === 'receive') {
          /* 买家确认收货。收货后 24 小时订单自动“交易完成” */
          const ok = await confirmDialog({
            title: '确认收货',
            message: '请确认已收到书且品相无误。收货后 24 小时订单将自动完成，届时可以互相评价。',
          });
          if (!ok) return;
          await api.put(`/api/orders/${id}/receive`);
          toast('已确认收货，24 小时后订单自动完成', 'success');

        } else if (op === 'cancel') {
          /* 取消订单：⚠ 后端 @RequestBody String —— 请求体必须是 JSON 字符串！
             api.put 传一个 JS 字符串，会被 JSON.stringify 成 "xxx"，正合适。 */
          const reason = await promptDialog({
            title: '取消订单',
            label: '取消原因（可选）',
            placeholder: '例如：不想要了 / 拍错了',
          });
          if (reason === null) return;   /* 用户点了取消 */
          await api.put(`/api/orders/${id}/cancel`, reason);
          toast('订单已取消', 'success');

        } else if (op === 'review') {
          await openReviewDialog(order, refresh);
          return;   /* 评价弹窗内部已处理刷新 */
        }

        refresh();            /* 状态变了，刷新列表 */
        window.refreshBadges?.();
      } catch { /* 错误已统一提示 */ }
    };
  });
}

/* ==========================================================================
 * 【共享】评价弹窗（打星 + 文字评价）
 * ========================================================================== */

/**
 * 打开评价弹窗。
 * @param {object} order 订单（需要 id）
 * @param {()=>void} onDone 提交成功后的回调
 */
export function openReviewDialog(order, onDone) {
  let rating = 5;   /* 默认 5 星 */
  const { el, close } = openModal(`
    <div class="modal-title">⭐ 评价这笔交易</div>
    <div class="form-item">
      <label class="form-label">交易体验评分</label>
      <div id="review-stars"></div>
    </div>
    <div class="form-item">
      <label class="form-label">评价内容（可选，最多 500 字）</label>
      <textarea class="form-textarea" id="review-content"
                placeholder="书的状态、对方的沟通与发货速度……"></textarea>
    </div>
    <div class="modal-footer">
      <button class="btn btn-outline" data-act="cancel">取消</button>
      <button class="btn btn-primary" data-act="submit">提交评价</button>
    </div>
  `);

  /* 渲染可点击的星星 */
  const getRating = bindStarInput(el.querySelector('#review-stars'), rating, v => { rating = v; });

  el.querySelector('[data-act="cancel"]').onclick = close;
  el.querySelector('[data-act="submit"]').onclick = async () => {
    const content = el.querySelector('#review-content').value.trim();
    try {
      await api.post('/api/reviews', {
        order_id: order.id,       /* 下划线：后端 ReviewCreateDTO.orderId */
        rating: getRating(),
        content: content || null,
      });
      toast('评价成功，谢谢你的反馈！', 'success');
      close();
      onDone?.();
    } catch { /* 错误已统一提示（如“已评价过”“订单状态不允许”） */ }
  };
}

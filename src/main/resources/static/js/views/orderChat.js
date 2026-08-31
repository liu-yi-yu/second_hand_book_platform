/* ============================================================================
 * views/orderChat.js —— 订单详情 + 实时聊天室（整个项目最“复杂”的页面）
 * ============================================================================
 * 对应后端接口：
 *   GET /api/orders/{orderId}
 *       返回 OrderVO：
 *         { id, buyer:{id,username,avatar_url}, seller:{...},
 *           book:{id,title,author,status,condition,cover_image},
 *           amount, status, cancel_reason,
 *           confirmed_at, shipped_at, received_at, completed_at, cancelled_at,
 *           created_at(可能为 null) }
 *
 *   GET /api/orders/{orderId}/messages?limit=50     拉历史消息
 *       返回 { message: [消息数组], has_more: 布尔 }
 *       ⚠ 字段名就叫 message（不是 messages）！
 *       ⚠ 消息按时间【最新在前】排序 → 渲染时要 reverse 成“旧在上”。
 *       ⚠ 没有任何消息时后端返回 data=null。
 *       消息字段：{ id(字符串), order_id, sender_id, receiver_id,
 *                  content, client_id, created_at }
 *
 *   PUT /api/orders/{orderId}/messages/read         进入聊天页时标记已读
 *
 *   WebSocket 实时收发见 js/ws.js（本页面只负责“画”消息）。
 * ========================================================================== */

import { api } from '../api.js';
import { getUser } from '../store.js';
import {
  ORDER_FLOW, orderStatusLabel, ORDER_STATUS_COLOR, conditionLabel,
} from '../constants.js';
import {
  coverHtml, escapeHtml, formatDate, badge, avatarHtml, money,
  toast, loadingBlock, emptyBlock,
} from '../ui.js';
import { setViewCleanup } from '../router.js';

/* 下面这些从别的视图文件/模块导入 */
import { bindOrderActions } from './orders.js';
import { onNewMessage, onStatusChange, sendChatMessage } from '../ws.js';

const PAGE_LIMIT = 50;    /* 一次拉取的历史消息条数（后端最大 100） */

/**
 * 渲染订单详情+聊天页
 * @param {HTMLElement} app 页面容器
 * @param {{id:string}} params 路由参数（订单 ID，字符串形式的整数）
 */
export async function renderOrderChat(app, params) {
  const orderId = Number(params.id);        /* ⚠ 后端订单 ID 是整数，转一下数字 */
  const me = getUser();

  app.innerHTML = loadingBlock();

  /* ---------------- 1. 拉订单详情 ---------------- */
  let order;
  try {
    order = await api.get(`/api/orders/${orderId}`);
  } catch {
    return;   /* “订单不存在/不是你的订单”等错误已提示 */
  }
  if (!order) {
    app.innerHTML = emptyBlock('订单不存在或你没有权限查看', '🔒');
    return;
  }

  /* 我在这笔订单里是买家还是卖家？（后端保证只有这两个人能看到订单） */
  const role = order.buyer?.id === me.id ? 'buyer' : 'seller';
  /* 聊天对象 = 对方 */
  const partner = role === 'buyer' ? order.seller : order.buyer;
  const partnerName = partner?.username ?? '对方';

  /* ---------------- 2. 页面骨架 ---------------- */
  app.innerHTML = `
    <div class="page-title">订单 #${order.id}
      ${badge(orderStatusLabel(order.status), ORDER_STATUS_COLOR[order.status] ?? 'gray')}
    </div>

    <!-- 订单信息卡 -->
    <div class="card">
      <div style="display:flex;gap:14px;align-items:center;flex-wrap:wrap">
        ${coverHtml(order.book?.cover_image, '', order.book?.title)}
        <div style="flex:1;min-width:200px">
          <div style="font-weight:700;font-size:15px;cursor:pointer" id="go-book">
            ${escapeHtml(order.book?.title ?? '—')}
          </div>
          <div style="color:var(--text-light);font-size:13px">
            ${conditionLabel(order.book?.condition)} · ${money(order.amount)}
          </div>
          <div style="display:flex;gap:16px;margin-top:6px;font-size:13px;align-items:center">
            <span>${avatarHtml(order.buyer?.username, order.buyer?.avatar_url, 'avatar-sm')}
              ${escapeHtml(order.buyer?.username ?? '—')}（买家）</span>
            <span>${avatarHtml(order.seller?.username, order.seller?.avatar_url, 'avatar-sm')}
              ${escapeHtml(order.seller?.username ?? '—')}（卖家）</span>
          </div>
        </div>
        <div class="actions" id="order-actions"></div>
      </div>

      <!-- 状态步骤条（已取消的订单不画步骤，直接显示取消原因） -->
      ${order.status === 'cancelled'
        ? `<div class="alert alert-danger" style="margin-top:14px">订单已取消　取消原因：${escapeHtml(order.cancel_reason || '未填写')}</div>`
        : renderSteps(order.status)}
    </div>

    <!-- 聊天卡 -->
    <div class="card">
      <div class="card-title">💬 与 ${escapeHtml(partnerName)} 沟通
        <span id="ws-status" style="font-size:12px;color:var(--text-light);font-weight:400"></span>
      </div>
      <div class="chat-box" id="chat-box"></div>
      <div id="chat-input-area"></div>
    </div>
  `;

  /* 点书名跳书籍详情 */
  document.getElementById('go-book').onclick = () => {
    if (order.book?.id) location.hash = `#/books/${order.book.id}`;
  };

  /* 操作按钮（确认/发货/收货/取消/评价 —— 逻辑与订单列表共用） */
  bindOrderActions(order, role, document.getElementById('order-actions'),
    () => renderOrderChat(app, params));     /* 操作后重新渲染整页刷新状态 */

  /* ---------------- 3. 聊天区 ---------------- */
  const chatBox = document.getElementById('chat-box');
  const myId = me.id;

  /** 画一条消息气泡。dir: 'left'=对方 | 'right'=我 */
  function bubble(msg, dir, pending = false) {
    const row = document.createElement('div');
    row.className = `chat-row ${dir === 'right' ? 'mine' : ''}`;
    row.innerHTML = `
      <div style="min-width:0">
        ${dir === 'left' ? `<div class="chat-name">${escapeHtml(partnerName)}</div>` : ''}
        <div class="chat-bubble ${pending ? 'pending' : ''}"></div>
        <div class="chat-time">${pending ? '发送中…' : escapeHtml(formatDate(msg.created_at))}</div>
      </div>`;
    /* 用 textContent 塞正文，防止消息里带 HTML 被执行（XSS 防护） */
    row.querySelector('.chat-bubble').textContent = msg.content ?? '';
    chatBox.appendChild(row);
    chatBox.scrollTop = chatBox.scrollHeight;   /* 自动滚到底部 */
    return row;
  }

  /* 3.1 拉历史消息 */
  try {
    const data = await api.get(`/api/orders/${orderId}/messages`, { limit: PAGE_LIMIT });

    if (data && Array.isArray(data.message) && data.message.length > 0) {
      /* 后端最新在前 → 反转成“旧在上、新在下”再画 */
      const ordered = [...data.message].reverse();
      ordered.forEach(m => bubble(m, m.sender_id === myId ? 'right' : 'left'));

      /* has_more=true 说明还有更早的。⚠ 注意：后端分页参数疑似有 bug
         （把总条数当页码传给了 PageHelper），历史消息可能拉不全，
         这里如实提示，不影响实时消息。 */
      if (data.has_more) {
        chatBox.insertAdjacentHTML('afterbegin',
          `<div style="text-align:center;color:var(--text-light);font-size:12px">— 仅显示最近 ${PAGE_LIMIT} 条 —</div>`);
      }
    } else {
      chatBox.innerHTML = `<div style="text-align:center;color:var(--text-light);margin:auto">还没有消息，打个招呼吧～</div>`;
    }
  } catch {
    chatBox.innerHTML = `<div style="text-align:center;color:var(--text-light);margin:auto">历史消息加载失败</div>`;
  }

  /* 3.2 进入聊天页 → 把这个订单的未读标记为已读 */
  api.put(`/api/orders/${orderId}/messages/read`).then(() => window.refreshBadges?.()).catch(() => {});

  /* 3.3 输入区：已完成/已取消的订单只读（后端也会拒绝发送） */
  const inputArea = document.getElementById('chat-input-area');
  if (order.status === 'completed' || order.status === 'cancelled') {
    inputArea.innerHTML = `<div class="chat-readonly">订单已${order.status === 'completed' ? '完成' : '取消'}，聊天已关闭（仅可查看历史消息）</div>`;
  } else {
    inputArea.innerHTML = `
      <div class="chat-input-bar">
        <textarea class="form-textarea" id="chat-input" maxlength="2000"
                  placeholder="输入消息，回车发送（Shift+回车换行）"></textarea>
        <button class="btn btn-primary btn-lg" id="chat-send">发送</button>
      </div>`;

    const input = document.getElementById('chat-input');
    const sendBtn = document.getElementById('chat-send');

    /** 发送动作：乐观上屏 → WS 发送 → ack 转正 */
    async function doSend() {
      const content = input.value.trim();
      if (!content) return;
      input.value = '';

      /* (1) 乐观上屏：先画出来（半透明），生成 client_id 用于对账 */
      const clientId = (crypto.randomUUID)
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      const row = bubble({ content }, 'right', true);

      /* 清掉“还没有消息”占位 */
      const placeholder = chatBox.querySelector('div[style*="margin:auto"]');
      if (placeholder) placeholder.remove();

      try {
        /* (2) 通过 WebSocket 发给后端 */
        const ack = await sendChatMessage(orderId, content);
        /* (3) 收到回执：把“发送中…”换成服务器时间，去掉半透明 */
        row.classList.remove('pending');
        row.querySelector('.chat-bubble').classList.remove('pending');
        row.querySelector('.chat-time').textContent = formatDate(ack.created_at);
      } catch (err) {
        toast(err.message || '发送失败', 'error');
        row.querySelector('.chat-time').textContent = '发送失败';
      }
    }

    sendBtn.onclick = doSend;
    input.addEventListener('keydown', e => {
      if (e.key === 'Enter' && !e.shiftKey) {    /* 回车发送，Shift+回车换行 */
        e.preventDefault();
        doSend();
      }
    });
  }

  /* 3.4 实时接收：注册 WebSocket 监听器 */
  const offNew = onNewMessage(message => {
    /* 只处理“本订单”的消息；其他订单的由 app.js 全局提示 */
    if (String(message.order_id) !== String(orderId)) return;

    /* 空占位清掉 */
    const ph = chatBox.querySelector('div[style*="margin:auto"]');
    if (ph) ph.remove();

    /* 画气泡：分清左右 */
    const dir = message.sender_id === myId ? 'right' : 'left';
    bubble({
      content: message.content,
      created_at: message.created_at,
    }, dir, dir === 'right');   /* 万一收到自己消息的推送也按普通消息画 */

    /* 对方发的消息 → 立刻标记已读，并刷新顶栏徽标 */
    if (message.sender_id !== myId) {
      api.put(`/api/orders/${orderId}/messages/read`).then(() => window.refreshBadges?.()).catch(() => {});
    }
  });

  /* 3.5 连接状态提示（“已断线，重连中…”） */
  const statusEl = document.getElementById('ws-status');
  const offStatus = onStatusChange(status => {
    statusEl.textContent = status === 'connected' ? '· 已连接' : '· 已断线，自动重连中…';
    statusEl.style.color = status === 'connected' ? 'var(--success)' : 'var(--danger)';
  });

  /* 3.6 标记“当前聊天页”给 app.js（避免在聊天页还弹全局消息提醒） */
  window.__activeChatOrderId = orderId;

  /* 3.7 离开页面时的清理：注销监听器（router.js 切页前会调用） */
  setViewCleanup(() => {
    offNew();
    offStatus();
    window.__activeChatOrderId = null;
  });
}

/** 订单状态步骤条：pending → confirmed → shipped → received → completed */
function renderSteps(status) {
  const idx = ORDER_FLOW.indexOf(status);
  /* cancelled 等异常状态不在流程里，外层已处理 */
  if (idx < 0) return '';

  return `
    <div class="steps">
      ${ORDER_FLOW.map((s, i) => `
        <div class="step ${i < idx ? 'done' : ''} ${i === idx ? 'current' : ''}">
          <div class="dot">${i < idx ? '✓' : i + 1}</div>
          <div>${orderStatusLabel(s)}</div>
        </div>
      `).join('')}
    </div>
  `;
}

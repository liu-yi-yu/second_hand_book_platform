/* ============================================================================
 * views/admin.js —— 管理后台（4 个 tab：数据看板 / 用户管理 / 书籍审核 / 订单仲裁）
 * ============================================================================
 * 对应后端接口（全部需要 admin 角色，后端 AdminCheckInterceptor 校验，
 * 不是管理员会返回 {"code":40003,"msg":"..."}）：
 *
 *   GET  /api/admin/dashboard                  数据看板
 *        { total_users, active_users7days, total_books_selling,
 *          total_orders, completed_orders, total_sales,
 *          order_status_count: [{count, status}],        各状态订单数
 *          new_orders7days: [n1..n7] }                   近 7 天每天新增（倒序）
 *
 *   GET  /api/admin/users?page=&pageSize=&keyword=&role=&status=
 *        → PageVO { records: [UserProfileVO], total }
 *        ⚠ UserProfileVO 里【没有 role/status 字段】，所以列表显示不出
 *          用户的角色和启用状态（筛选参数是支持 status 的）。
 *   PUT  /api/admin/users/{userId}/status      body {"status":"active"|"disabled"}
 *        ⚠ 管理员不能禁用自己（后端会拒绝）
 *
 *   GET  /api/admin/books                      ⚠ 后端写死只返回前 15 条
 *        → PageVO { records: [BookListVO], total }
 *   PUT  /api/admin/books/{bookId}/remove      强制下架
 *
 *   GET  /api/admin/orders                     ⚠ 只返回前 15 条，
 *                                              且没有书名/买卖双方字段（select *）
 *   PUT  /api/admin/orders/{orderId}/cancel?reason=xxx
 *        ⚠ reason 是【查询参数】不是请求体！
 * ========================================================================== */

import { api } from '../api.js';
import {
  ORDER_STATUS_MAP, ORDER_STATUS_COLOR, orderStatusLabel,
  bookStatusLabel, BOOK_STATUS_COLOR, categoryLabel, conditionLabel,
} from '../constants.js';
import {
  escapeHtml, formatDate, badge, coverHtml, avatarHtml, toast, money,
  confirmDialog, promptDialog, renderPagination, emptyBlock, loadingBlock,
} from '../ui.js';

/* 子页面 tab 与路由的对应：#/admin 或 #/admin/dashboard 等 */
const TABS = [
  { key: 'dashboard', label: '📊 数据看板' },
  { key: 'users',     label: '👥 用户管理' },
  { key: 'books',     label: '📚 书籍审核' },
  { key: 'orders',    label: '⚖️ 订单仲裁' },
];

/**
 * 渲染管理后台
 * @param {HTMLElement} app 页面容器
 * @param {{tab?:string}} params 路由参数，params.tab = dashboard/users/books/orders
 */
export async function renderAdmin(app, params) {
  const tab = params?.tab ?? 'dashboard';
  if (!TABS.some(t => t.key === tab)) {
    return renderAdmin(app, { tab: 'dashboard' });
  }

  app.innerHTML = `
    <div class="page-title">🛡️ 管理后台</div>
    <div class="admin-tabs">
      ${TABS.map(t => `<span class="admin-tab ${t.key === tab ? 'active' : ''}"
                            data-tab="${t.key}">${t.label}</span>`).join('')}
    </div>
    <div id="admin-body">${loadingBlock()}</div>
  `;

  /* tab 切换 = 改 hash → 路由器重新进到本视图（#/admin/xxx 会匹配 #/admin/:tab） */
  app.querySelectorAll('[data-tab]').forEach(el => {
    el.onclick = () => { location.hash = `#/admin/${el.dataset.tab}`; };
  });

  const body = document.getElementById('admin-body');

  /* ---------------- 按 tab 分发 ---------------- */
  try {
    if (tab === 'dashboard') await renderDashboard(body);
    if (tab === 'users')     await renderUsers(body);
    if (tab === 'books')     await renderBooks(body);
    if (tab === 'orders')    await renderOrdersAdmin(body);
  } catch (err) {
    /* 40003 = 不是管理员（后端拦截器返回的），画一个友好提示页 */
    if (err && err.code === 40003) {
      body.innerHTML = `<div class="card">${emptyBlock('你没有管理员权限，此页面仅对 admin 角色开放', '🔒')}</div>`;
      return;
    }
    body.innerHTML = emptyBlock('加载失败，请稍后重试', '⚠️');
  }
}

/* ==========================================================================
 * Tab 1：数据看板
 * ========================================================================== */
async function renderDashboard(body) {
  const d = await api.get('/api/admin/dashboard');
  if (!d) { body.innerHTML = emptyBlock('暂无数据'); return; }

  /* 各状态订单数 → 画横向柱状对比（纯 div，不引图表库） */
  const statusCounts = d.order_status_count ?? [];
  const maxStatus = Math.max(1, ...statusCounts.map(s => s.count ?? 0));

  /* 近 7 天新增订单 → 画纵向柱状图。
     ⚠ 后端返回的是按天分组的数量数组（created_at 倒序），没有日期字段，
       就按“6 天前 … 今天”标注（最右是数组第一个元素=最近的一天）。 */
  const daily = (d.new_orders7days ?? []).slice(0, 7);       /* 保险起见截取 7 个 */
  const dailyAsc = [...daily].reverse();                     /* 反转成时间正序画图 */
  const maxDaily = Math.max(1, ...dailyAsc);

  body.innerHTML = `
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card"><div class="label">注册用户总数</div><div class="value">${d.total_users ?? 0}</div></div>
      <div class="stat-card"><div class="label">近 7 天活跃用户</div><div class="value">${d.active_users7days ?? 0}</div></div>
      <div class="stat-card"><div class="label">在售书籍</div><div class="value">${d.total_books_selling ?? 0}</div></div>
      <div class="stat-card"><div class="label">总订单数</div><div class="value">${d.total_orders ?? 0}</div></div>
      <div class="stat-card"><div class="label">已完成订单</div><div class="value">${d.completed_orders ?? 0}</div></div>
      <div class="stat-card"><div class="label">总交易金额</div><div class="value money">${money(d.total_sales ?? 0)}</div></div>
    </div>

    <!-- 各状态订单数量（横向条） -->
    <div class="card">
      <div class="card-title">各状态订单数量</div>
      ${statusCounts.length === 0 ? emptyBlock('还没有订单数据') : statusCounts.map(s => `
        <div class="dist-row">
          <span style="width:90px">${orderStatusLabel(s.status) ?? s.status}</span>
          <div class="dist-bar" style="height:14px">
            <div style="width:${Math.round(((s.count ?? 0) / maxStatus) * 100)}%;background:var(--primary)"></div>
          </div>
          <span style="width:36px;text-align:right;font-weight:700">${s.count ?? 0}</span>
        </div>`).join('')}
    </div>

    <!-- 近 7 天新增订单（纵向柱状图） -->
    <div class="card">
      <div class="card-title">近 7 天新增订单</div>
      <div class="bar-chart">
        ${dailyAsc.map((n, i) => `
          <div class="bar-item">
            <div class="bar-value">${n ?? 0}</div>
            <div class="bar" style="height:${Math.round(((n ?? 0) / maxDaily) * 100)}%"></div>
            <div class="bar-label">${6 - i === 0 ? '今天' : `${6 - i} 天前`}</div>
          </div>`).join('')}
      </div>
    </div>
  `;
}

/* ==========================================================================
 * Tab 2：用户管理
 * ========================================================================== */
async function renderUsers(body) {
  /* 本 tab 内部的筛选/分页状态 */
  const filters = { keyword: '', status: '', page: 1 };
  const PAGE_SIZE = 15;

  body.innerHTML = `
    <div class="card">
      <div class="filter-bar">
        <input class="form-input" id="u-keyword" type="text" placeholder="按用户名/邮箱搜索" style="width:220px" />
        <select class="form-select" id="u-status">
          <option value="">全部状态</option>
          <option value="active">已启用</option>
          <option value="disabled">已禁用</option>
        </select>
        <button class="btn btn-primary" id="u-search">搜索</button>
      </div>
      <div class="alert alert-info" style="margin:0">
        说明：后端返回的用户字段不含角色/状态，因此列表无法显示“是否管理员/是否禁用”，
        但筛选和禁用操作均有效。
      </div>
    </div>
    <div class="card"><div id="u-list"></div><div class="pagination" id="u-pager"></div></div>
  `;

  document.getElementById('u-search').onclick = () => {
    filters.keyword = document.getElementById('u-keyword').value.trim();
    filters.status = document.getElementById('u-status').value;
    filters.page = 1;
    load();
  };
  /* 回车搜索 */
  document.getElementById('u-keyword').addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('u-search').click();
  });

  await load();

  async function load() {
    const box = document.getElementById('u-list');
    box.innerHTML = loadingBlock();

    let data;
    try {
      data = await api.get('/api/admin/users', {
        page: filters.page,
        pageSize: PAGE_SIZE,
        keyword: filters.keyword || undefined,
        status: filters.status || undefined,
      });
    } catch (err) {
      box.innerHTML = emptyBlock(err.code === 40003 ? '没有管理员权限' : '加载失败');
      return;
    }

    const records = data?.records ?? [];

    if (records.length === 0) {
      box.innerHTML = emptyBlock('没有找到用户', '👥');
      document.getElementById('u-pager').innerHTML = '';
      return;
    }

    box.innerHTML = `<div class="table-wrap"><table class="table">
      <thead><tr><th>用户</th><th>信誉分</th><th>在售/已售</th><th>注册时间</th><th style="width:130px">操作</th></tr></thead>
      <tbody>
        ${records.map(u => `
          <tr>
            <td><div class="book-cell">
              ${avatarHtml(u.username, u.avatar_url, 'avatar-md')}
              <div>
                <div class="t">${escapeHtml(u.username)}</div>
                <div style="color:var(--text-light);font-size:12px">ID: ${escapeHtml(u.id)}</div>
              </div>
            </div></td>
            <td>${u.credit_score ?? '—'}</td>
            <td>${u.selling_count ?? 0} / ${u.sold_count ?? 0}</td>
            <td>${formatDate(u.created_at)}</td>
            <td class="actions">
              <!-- 禁用/启用按钮：一次只显示一个（不知道当前状态，管理员按需点） -->
              <button class="btn btn-danger btn-sm" data-disable="${escapeHtml(u.id)}" data-name="${escapeHtml(u.username)}">禁用</button>
              <button class="btn btn-outline btn-sm" data-enable="${escapeHtml(u.id)}" data-name="${escapeHtml(u.username)}">启用</button>
            </td>
          </tr>`).join('')}
      </tbody>
    </table></div>`;

    /* 禁用：确认后 PUT {"status":"disabled"}。管理员禁用自己后端会拒绝。 */
    box.querySelectorAll('[data-disable]').forEach(btn => {
      btn.onclick = async () => {
        const ok = await confirmDialog({
          title: '禁用用户',
          message: `确定禁用「${btn.dataset.name}」吗？该用户将无法登录，其所有在售书籍会自动下架。`,
          danger: true, okText: '确定禁用',
        });
        if (!ok) return;
        try {
          await api.put(`/api/admin/users/${encodeURIComponent(btn.dataset.disable)}/status`, { status: 'disabled' });
          toast('已禁用', 'success');
          load();
        } catch { /* 错误已提示（比如禁用了自己） */ }
      };
    });

    /* 启用：PUT {"status":"active"}，其下架的书会重新上架（后端处理） */
    box.querySelectorAll('[data-enable]').forEach(btn => {
      btn.onclick = async () => {
        const ok = await confirmDialog({ title: '启用用户', message: `确定恢复「${btn.dataset.name}」的登录权限吗？` });
        if (!ok) return;
        try {
          await api.put(`/api/admin/users/${encodeURIComponent(btn.dataset.enable)}/status`, { status: 'active' });
          toast('已启用', 'success');
          load();
        } catch { /* 错误已提示 */ }
      };
    });

    renderPagination(
      document.getElementById('u-pager'),
      filters.page,
      Math.ceil((data?.total ?? 0) / PAGE_SIZE),
      p => { filters.page = p; load(); }
    );
  }
}

/* ==========================================================================
 * Tab 3：书籍审核（强制下架）
 * ========================================================================== */
async function renderBooks(body) {
  body.innerHTML = loadingBlock();

  let data;
  try {
    data = await api.get('/api/admin/books');
  } catch (err) {
    body.innerHTML = emptyBlock(err.code === 40003 ? '没有管理员权限' : '加载失败');
    return;
  }

  const records = data?.records ?? [];

  if (records.length === 0) {
    body.innerHTML = `<div class="card">${emptyBlock('没有书籍数据', '📚')}</div>`;
    return;
  }

  body.innerHTML = `
    <div class="alert alert-warning">⚠ 后端此接口固定返回前 15 条书籍，暂不支持翻页/搜索。</div>
    <div class="card"><div class="table-wrap"><table class="table">
      <thead><tr><th>书籍</th><th>分类/成色</th><th>售价</th><th>状态</th><th>浏览</th><th style="width:120px">操作</th></tr></thead>
      <tbody>
        ${records.map(b => `
          <tr>
            <td><div class="book-cell">
              ${coverHtml(b.cover_image, '', b.title)}
              <div>
                <div class="t" style="cursor:pointer" data-id="${escapeHtml(b.id)}">${escapeHtml(b.title)}</div>
                <div style="color:var(--text-light);font-size:12px">卖家ID: ${escapeHtml(b.seller_id ?? '—')}</div>
              </div>
            </div></td>
            <td>${categoryLabel(b.category)} / ${conditionLabel(b.condition)}</td>
            <td><b style="color:var(--danger)">${money(b.selling_price)}</b></td>
            <td>${badge(bookStatusLabel(b.status), BOOK_STATUS_COLOR[b.status] ?? 'gray')}</td>
            <td>${b.view_count ?? 0}</td>
            <td class="actions">
              ${b.status !== 'removed'
                ? `<button class="btn btn-danger btn-sm" data-remove="${escapeHtml(b.id)}" data-title="${escapeHtml(b.title)}">强制下架</button>`
                : '<span style="color:var(--text-light);font-size:12px">已下架</span>'}
            </td>
          </tr>`).join('')}
      </tbody>
    </table></div></div>
  `;

  /* 点书名去详情页 */
  body.querySelectorAll('.t[data-id]').forEach(el => {
    el.onclick = () => { location.hash = `#/books/${el.dataset.id}`; };
  });

  /* 强制下架 */
  body.querySelectorAll('[data-remove]').forEach(btn => {
    btn.onclick = async () => {
      const ok = await confirmDialog({
        title: '强制下架',
        message: `确定强制下架「${btn.dataset.title}」吗？该书会立即对买家不可见。`,
        danger: true, okText: '确定下架',
      });
      if (!ok) return;
      try {
        await api.put(`/api/admin/books/${encodeURIComponent(btn.dataset.remove)}/remove`);
        toast('已强制下架', 'success');
        renderBooks(body);   /* 刷新列表 */
      } catch { /* 错误已提示 */ }
    };
  });
}

/* ==========================================================================
 * Tab 4：订单仲裁（强制取消）
 * ========================================================================== */
async function renderOrdersAdmin(body) {
  body.innerHTML = loadingBlock();

  let data;
  try {
    data = await api.get('/api/admin/orders');
  } catch (err) {
    body.innerHTML = emptyBlock(err.code === 40003 ? '没有管理员权限' : '加载失败');
    return;
  }

  const records = data?.records ?? [];

  if (records.length === 0) {
    body.innerHTML = `<div class="card">${emptyBlock('没有订单数据', '⚖️')}</div>`;
    return;
  }

  body.innerHTML = `
    <div class="alert alert-warning">
      ⚠ 后端此接口固定返回前 15 条订单；且目前不返回书名/买卖双方字段，
      仅能按订单号、金额、状态进行仲裁。
    </div>
    <div class="card"><div class="table-wrap"><table class="table">
      <thead><tr><th>订单号</th><th>书籍ID</th><th>金额</th><th>状态</th><th>下单时间</th><th style="width:130px">操作</th></tr></thead>
      <tbody>
        ${records.map(o => `
          <tr>
            <td>#${o.id}</td>
            <td style="font-size:12px;word-break:break-all">${escapeHtml(o.book_id ?? '—')}</td>
            <td><b style="color:var(--danger)">${money(o.amount)}</b></td>
            <td>${badge(orderStatusLabel(o.status), ORDER_STATUS_COLOR[o.status] ?? 'gray')}</td>
            <td>${formatDate(o.created_at)}</td>
            <td class="actions">
              ${o.status !== 'cancelled' && o.status !== 'completed'
                ? `<button class="btn btn-danger btn-sm" data-cancel="${o.id}">强制取消</button>`
                : '<span style="color:var(--text-light);font-size:12px">—</span>'}
            </td>
          </tr>`).join('')}
      </tbody>
    </table></div></div>
  `;

  /* 强制取消：⚠ reason 是查询参数（不是请求体）！必填。 */
  body.querySelectorAll('[data-cancel]').forEach(btn => {
    btn.onclick = async () => {
      const reason = await promptDialog({
        title: `强制取消订单 #${btn.dataset.cancel}`,
        label: '取消原因（必填，将记录到操作日志）',
        placeholder: '例如：违规交易 / 用户仲裁',
        required: true,
      });
      if (reason === null) return;
      try {
        await api.put(`/api/admin/orders/${btn.dataset.cancel}/cancel`, undefined, { reason });
        toast('订单已强制取消，书籍恢复在售', 'success');
        renderOrdersAdmin(body);
      } catch { /* 错误已提示 */ }
    };
  });
}

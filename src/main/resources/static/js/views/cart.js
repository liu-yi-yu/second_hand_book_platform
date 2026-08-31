/* ============================================================================
 * views/cart.js —— 购物车
 * ============================================================================
 * 对应后端接口：
 *   GET  /api/cart                  → 购物车列表（数组，空车时 data=null）
 *        每项：{ id, book:{id,title,author,status,condition,cover_image},
 *                seller:{id,username,avatar_url}, selling_price, created_at }
 *        ⚠ 后端已经把“非在售”的书过滤掉了，所以列表里都是在售书；
 *          cover_image 恒为 null → 用占位图。
 *   DELETE /api/cart/{book_id}      → 移除一项（按书籍 ID 删）
 *   POST /api/cart/checkout-preview body { book_ids: [...] }
 *                                   → { cart_item_vo_list, total_price, count }
 *                                     结算预览：总价、件数（下单前的确认页）
 *   POST /api/orders                body { book_ids: [...] }
 *                                   → 创建订单（返回订单数组或 null）
 * ========================================================================== */

import { api } from '../api.js';
import { getUser } from '../store.js';
import { conditionLabel } from '../constants.js';
import { coverHtml, escapeHtml, formatDate, toast, confirmDialog, emptyBlock, loadingBlock, money } from '../ui.js';

/* 勾选状态按“书籍 ID”记，渲染时用 */
let selectedIds = new Set();

/**
 * 渲染购物车页
 * @param {HTMLElement} app 页面容器
 */
export async function renderCart(app) {
  app.innerHTML = loadingBlock();

  /* ---------------- 1. 拉取购物车 ---------------- */
  let items;
  try {
    items = await api.get('/api/cart');
  } catch {
    return;
  }
  items = items ?? [];          /* 后端空车返回 null，统一成空数组 */

  if (items.length === 0) {
    app.innerHTML = `
      <div class="page-title">🛒 购物车</div>
      ${emptyBlock('购物车空空如也，去首页淘本书吧', '🛒')}
      <div style="text-align:center"><a href="#/">去逛逛 →</a></div>
    `;
    return;
  }

  /* 新加载时默认全部勾选（保留用户上次的勾选：只保留仍存在的书） */
  const validIds = new Set(items.map(it => it.book.id));
  selectedIds = new Set([...selectedIds].filter(id => validIds.has(id)));
  if (selectedIds.size === 0) items.forEach(it => selectedIds.add(it.book.id));

  /* ---------------- 2. 页面骨架 ---------------- */
  app.innerHTML = `
    <div class="page-title">🛒 购物车（${items.length} 件）</div>
    <div id="cart-list"></div>

    <!-- 底部结算条：吸底显示合计与“去结算”按钮 -->
    <div class="cart-footer">
      <label style="display:flex;align-items:center;gap:6px">
        <input type="checkbox" class="checkbox-lg" id="check-all" checked /> 全选
      </label>
      <div class="total">已选 <b id="sel-count">0</b> 件　合计：<b id="sel-total">¥0.00</b></div>
      <button class="btn btn-primary btn-lg" id="btn-checkout">去结算</button>
    </div>
  `;

  renderList();

  /* ---------------- 3. 交互 ---------------- */

  /* 全选/取消全选 */
  document.getElementById('check-all').onchange = e => {
    if (e.target.checked) items.forEach(it => selectedIds.add(it.book.id));
    else selectedIds.clear();
    renderList();
  };

  /* 去结算：先调“结算预览”接口拿总价，弹确认框，确认后真正下单 */
  document.getElementById('btn-checkout').onclick = async () => {
    if (selectedIds.size === 0) {
      toast('请先勾选要购买的书籍', 'warning');
      return;
    }

    const btn = document.getElementById('btn-checkout');
    btn.disabled = true;
    let preview;
    try {
      preview = await api.post('/api/cart/checkout-preview', { book_ids: [...selectedIds] });
    } catch {
      btn.disabled = false;
      return;
    }

    /* 后端预览返回 { cart_item_vo_list, total_price, count }。
       可能所有书都被别人买走了 → 列表为空 */
    if (!preview || !preview.cart_item_vo_list || preview.cart_item_vo_list.length === 0) {
      toast('勾选的书籍已失效（可能已售出），请刷新购物车', 'error');
      btn.disabled = false;
      return;
    }

    /* 确认弹窗：列出每本书 + 总价 */
    const rows = preview.cart_item_vo_list.map(it => `
      <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px dashed var(--border)">
        <span>《${escapeHtml(it.book.title)}》<span style="color:var(--text-light);font-size:12px">　${escapeHtml(it.seller?.username ?? '')}</span></span>
        <b>${money(it.selling_price)}</b>
      </div>`).join('');

    const { el, close } = openConfirm(rows, preview);

    el.querySelector('[data-act="submit"]').onclick = async () => {
      close();
      await createOrder([...selectedIds], btn);
    };
    el.querySelector('[data-act="cancel"]').onclick = () => {
      close();
      btn.disabled = false;   /* 用户取消结算：恢复“去结算”按钮可点击 */
    };
  };

  /** 结算确认弹窗（简单版，不用 openModal 以便展示动态总价） */
  function openConfirm(rowsHtml, preview) {
    /* 复用 ui.js 的 openModal —— 这里直接内联构造，和 confirmDialog 类似但内容更丰富 */
    const container = document.getElementById('modal-container');
    const mask = document.createElement('div');
    mask.className = 'modal-mask';
    mask.innerHTML = `
      <div class="modal">
        <div class="modal-title">确认订单（${preview.count} 件）</div>
        ${rowsHtml}
        <div style="text-align:right;margin-top:12px;font-size:15px">
          合计：<b style="color:var(--danger);font-size:22px">${money(preview.total_price)}</b>
        </div>
        <div class="alert alert-info" style="margin-top:10px">
          不同卖家的书会自动拆分成多笔订单，每笔需卖家确认后发货。
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" data-act="cancel">再想想</button>
          <button class="btn btn-primary" data-act="submit">提交订单</button>
        </div>
      </div>`;
    container.appendChild(mask);
    return { el: mask.querySelector('.modal'), close: () => mask.remove() };
  }

  /** 真正下单 */
  async function createOrder(bookIds, checkoutBtn) {
    /* 信誉分低于 60 的买家下单前警告（文档 3.10 软约束） */
    const me = getUser();
    if ((me?.score ?? 100) < 60) {
      const go = await confirmDialog({
        title: '信誉分提醒',
        message: `你的信誉分只有 ${me.score} 分（低于 60），卖家可能会拒绝交易，确定继续下单吗？`,
        okText: '继续下单',
      });
      if (!go) { checkoutBtn.disabled = false; return; }
    }

    try {
      const orders = await api.post('/api/orders', { book_ids: bookIds });
      if (orders && orders.length > 0) {
        toast(`下单成功！共创建 ${orders.length} 笔订单`, 'success');
        selectedIds.clear();          /* 下单成功清空勾选（后端也自动清了购物车） */
        location.hash = '#/orders';   /* 去订单列表等卖家确认 */
      } else {
        /* 后端乐观锁冲突时返回 null：说明书被别人抢先买了 */
        toast('下单失败，部分书籍已被他人购买', 'error');
        renderCart(app);              /* 重新拉购物车刷新状态 */
      }
    } catch {
      checkoutBtn.disabled = false;
    }
  }

  /* ---------------- 4. 渲染列表 ---------------- */
  function renderList() {
    document.getElementById('cart-list').innerHTML = items.map(it => `
      <div class="cart-item">
        <input type="checkbox" class="checkbox-lg item-check"
               data-id="${escapeHtml(it.book.id)}" ${selectedIds.has(it.book.id) ? 'checked' : ''} />
        ${coverHtml(it.book.cover_image, '', it.book.title)}
        <div class="info">
          <div class="title" data-id="${escapeHtml(it.book.id)}">${escapeHtml(it.book.title)}</div>
          <div style="color:var(--text-light);font-size:12px">
            《${escapeHtml(it.book.author)}》 · ${conditionLabel(it.book.condition)} ·
            卖家：${escapeHtml(it.seller?.username ?? '—')} ·
            加入于 ${formatDate(it.created_at)}
          </div>
        </div>
        <div style="text-align:right">
          <div class="price">${money(it.selling_price)}</div>
          <button class="btn btn-outline btn-sm" style="margin-top:6px" data-remove="${escapeHtml(it.book.id)}">移除</button>
        </div>
      </div>
    `).join('');

    updateFooter();

    /* 勾选某个条目 */
    document.querySelectorAll('.item-check').forEach(chk => {
      chk.onchange = () => {
        if (chk.checked) selectedIds.add(chk.dataset.id);
        else selectedIds.delete(chk.dataset.id);
        /* 同步“全选”框的勾选状态 */
        document.getElementById('check-all').checked = selectedIds.size === items.length;
        updateFooter();
      };
    });

    /* 点击书名去详情页 */
    document.querySelectorAll('.cart-item .title').forEach(t => {
      t.onclick = () => { location.hash = `#/books/${t.dataset.id}`; };
    });

    /* 移除：DELETE /api/cart/{book_id}，后端幂等（删不存在的也不报错） */
    document.querySelectorAll('[data-remove]').forEach(btn => {
      btn.onclick = async () => {
        try {
          await api.del(`/api/cart/${encodeURIComponent(btn.dataset.remove)}`);
          selectedIds.delete(btn.dataset.remove);
          items = items.filter(it => it.book.id !== btn.dataset.remove);
          if (items.length === 0) { renderCart(app); return; }   /* 删空了重画整页 */
          renderList();
          toast('已移除', 'success');
        } catch { /* 错误已统一提示 */ }
      };
    });
  }

  /** 更新底部“已选件数 / 合计金额” */
  function updateFooter() {
    const selItems = items.filter(it => selectedIds.has(it.book.id));
    const total = selItems.reduce((sum, it) => sum + Number(it.selling_price ?? 0), 0);
    document.getElementById('sel-count').textContent = selItems.length;
    document.getElementById('sel-total').textContent = money(total);
  }
}

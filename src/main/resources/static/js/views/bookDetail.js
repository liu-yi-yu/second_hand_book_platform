/* ============================================================================
 * views/bookDetail.js —— 书籍详情页
 * ============================================================================
 * 对应后端接口：
 *   GET /api/books/{bookId}
 *       返回 BookVO（价格是字符串！）：
 *         id, title, author, isbn, original_price, selling_price,
 *         condition, category, description, status(selling/sold/removed),
 *         view_count, images:[{id, url, thumbnail_url}],
 *         seller:{ id, username, avatar_url },   ← 卖家信息（嵌套对象）
 *         is_favorited, created_at, updated_at
 *       后端每访问一次详情页会把浏览量 +1（BookViewCounter），前端不用管。
 *
 *   加入购物车：POST /api/cart        body { book_id }
 *   立即购买  ：POST /api/orders      body { book_ids: [id] }
 *   下架      ：DELETE /api/books/{id}
 *   重新上架  ：PUT /api/books/{id}   body {}   ← 空对象即可：
 *       后端 updateBook 发现原状态是 removed 会自动把状态改回 selling，
 *       且 SQL 动态拼接时 null 字段都会被跳过，所以空对象不会破坏其他字段。
 * ========================================================================== */

import { api } from '../api.js';
import { getUser } from '../store.js';
import {
  categoryLabel, conditionLabel,
  bookStatusLabel, BOOK_STATUS_COLOR,
} from '../constants.js';
import {
  coverHtml, escapeHtml, formatDate, money,
  toast, confirmDialog, badge, loadingBlock, COVER_PLACEHOLDER,
} from '../ui.js';

/** 当前详情页显示的大图 URL（缩略图切换用） */
let currentMainImage = null;

/**
 * 渲染书籍详情
 * @param {HTMLElement} app 页面容器
 * @param {{id:string}} params 路由参数 params.id = 书籍 ID
 */
export async function renderBookDetail(app, params) {
  const bookId = params.id;
  app.innerHTML = loadingBlock();

  /* ---------------- 1. 拉取书籍详情 ---------------- */
  let book;
  try {
    book = await api.get(`/api/books/${encodeURIComponent(bookId)}`);
  } catch {
    return;   /* api.js 已提示过错误（如“书籍不存在”） */
  }

  const me = getUser();
  /* 是否是我自己发布的书：比较卖家 ID 和当前登录用户 ID */
  const isMine = me && book.seller && book.seller.id === me.id;
  const isSelling = book.status === 'selling';

  /* 默认大图 = 第一张图；没有图就用占位图 */
  currentMainImage = book.images?.[0]?.url || null;

  /* ---------------- 2. 静态骨架 ---------------- */
  app.innerHTML = `
    <div class="detail-layout">
      <!-- 左侧：图片区 -->
      <div>
        <img id="main-img" class="detail-main-img"
             src="${currentMainImage ? escapeHtml(currentMainImage) : COVER_PLACEHOLDER}"
             alt="${escapeHtml(book.title)}" />
        <div class="detail-thumbs" id="thumbs">
          ${(book.images ?? []).map(img => `
            <img src="${escapeHtml(img.url)}" data-url="${escapeHtml(img.url)}"
                 class="${img.url === currentMainImage ? 'active' : ''}" alt="" />
          `).join('')}
        </div>
      </div>

      <!-- 右侧：信息区 -->
      <div>
        <div class="card">
          <h2 style="margin-bottom:6px">${escapeHtml(book.title)}</h2>
          <div style="color:var(--text-light);margin-bottom:10px">
            《${escapeHtml(book.author)}》
            ${badge(conditionLabel(book.condition), 'blue')}
            ${badge(bookStatusLabel(book.status), BOOK_STATUS_COLOR[book.status] ?? 'gray')}
          </div>

          <div class="detail-price">
            <span class="price-symbol">¥</span>${escapeHtml(book.selling_price ?? '—')}
            ${book.original_price ? `<span class="price-original" style="font-size:14px;color:var(--text-light);text-decoration:line-through;margin-left:10px">原价 ${money(book.original_price)}</span>` : ''}
          </div>

          <!-- 键值对信息表 -->
          <div class="kv-list" style="margin-top:14px">
            <span class="k">分类</span><span>${categoryLabel(book.category)}</span>
            <span class="k">ISBN</span><span>${escapeHtml(book.isbn || '—')}</span>
            <span class="k">浏览量</span><span>${book.view_count ?? 0}</span>
            <span class="k">发布时间</span><span>${formatDate(book.created_at)}</span>
          </div>

          <!-- 操作按钮区：根据“是否我的书 / 状态”渲染不同按钮 -->
          <div class="actions" style="margin-top:18px" id="detail-actions"></div>
        </div>

        <!-- 卖家卡片：点击去卖家主页 -->
        ${book.seller ? `
        <div class="card">
          <div class="card-title">卖家信息</div>
          <div class="seller-card" id="seller-card">
            <span class="avatar avatar-md">
              ${book.seller.avatar_url
                ? `<img src="${escapeHtml(book.seller.avatar_url)}" alt="" style="position:absolute;inset:0" onerror="this.remove()">`
                : ''}
              ${escapeHtml((book.seller.username || '?').charAt(0).toUpperCase())}
            </span>
            <div>
              <div style="font-weight:600">${escapeHtml(book.seller.username)}</div>
              <div style="font-size:12px;color:var(--text-light)">点击查看主页与信誉评价</div>
            </div>
          </div>
        </div>` : ''}

        <!-- 描述 -->
        <div class="card">
          <div class="card-title">书籍描述</div>
          <div class="desc-text">${escapeHtml(book.description || '卖家没有填写描述。')}</div>
        </div>
      </div>
    </div>
  `;

  /* ---------------- 3. 交互绑定 ---------------- */

  /* 3.1 点击缩略图切换大图 */
  document.querySelectorAll('#thumbs img').forEach(thumb => {
    thumb.onclick = () => {
      currentMainImage = thumb.dataset.url;
      document.getElementById('main-img').src = currentMainImage;
      document.querySelectorAll('#thumbs img').forEach(t => t.classList.toggle('active', t === thumb));
    };
  });

  /* 3.2 卖家卡片 → 卖家主页 */
  const sellerCard = document.getElementById('seller-card');
  if (sellerCard) {
    sellerCard.onclick = () => { location.hash = `#/users/${book.seller.id}`; };
  }

  /* 3.3 操作按钮 */
  renderActions();

  /* ------------------------------------------------------------------------
   * 内部函数：按角色/状态渲染操作按钮
   * ---------------------------------------------------------------------- */
  function renderActions() {
    const box = document.getElementById('detail-actions');
    let html = '';

    if (isMine) {
      /* ---- 我的书：编辑 / 下架 / 重新上架 ---- */
      if (book.status === 'removed') {
        /* 已下架：修改会自动变回在售，所以这里给“重新上架”快捷按钮 */
        html += `<button class="btn btn-primary" id="act-relist">重新上架</button>`;
      }
      html += `<button class="btn btn-outline" id="act-edit">编辑</button>`;
      if (book.status !== 'sold') {
        /* 已售出的书后端不允许下架（会报“书籍已售出”），干脆不显示按钮 */
        html += `<button class="btn btn-danger" id="act-remove">下架</button>`;
      }
    } else if (isSelling) {
      /* ---- 别人的在售书：加购 / 立即购买 ---- */
      html += `
        <button class="btn btn-outline btn-lg" id="act-add-cart">🛒 加入购物车</button>
        <button class="btn btn-primary btn-lg" id="act-buy">立即购买</button>
      `;
    } else {
      html += `<span style="color:var(--text-light)">该书当前${bookStatusLabel(book.status)}，暂不可购买</span>`;
    }
    box.innerHTML = html;

    /* ---- 按钮事件 ---- */

    /* 加入购物车：POST /api/cart，body 是 { book_id }（下划线！） */
    const addCartBtn = document.getElementById('act-add-cart');
    if (addCartBtn) {
      addCartBtn.onclick = async () => {
        if (!requireLogin()) return;
        addCartBtn.disabled = true;
        try {
          const count = await api.post('/api/cart', { book_id: book.id });
          /* 后端返回购物车当前总数（一个数字） */
          toast(`已加入购物车（共 ${count} 件）`, 'success');
          window.refreshBadges?.();
        } catch { /* 错误已统一提示 */ }
        addCartBtn.disabled = false;
      };
    }

    /* 立即购买：跳过购物车直接下单 */
    const buyBtn = document.getElementById('act-buy');
    if (buyBtn) {
      buyBtn.onclick = async () => {
        if (!requireLogin()) return;
        /* 文档 3.10：信誉分低于 60 的买家下单时前端给个警告（软约束） */
        if ((me?.score ?? 100) < 60) {
          const go = await confirmDialog({
            title: '信誉分提醒',
            message: `你的信誉分只有 ${me.score} 分（低于 60），交易可能不受信任，确定继续下单吗？`,
            okText: '继续下单',
          });
          if (!go) return;
        }
        buyBtn.disabled = true;
        try {
          const orders = await api.post('/api/orders', { book_ids: [book.id] });
          /* 成功返回本次创建的订单数组；失败时后端可能返回 null */
          if (orders && orders.length > 0) {
            toast('下单成功，等待卖家确认', 'success');
            location.hash = '#/orders';
          } else {
            toast('下单失败，书籍可能已被他人抢先购买', 'error');
          }
        } catch { /* 错误已统一提示 */ }
        buyBtn.disabled = false;
      };
    }

    /* 编辑 → 跳编辑页（publish.js 复用） */
    const editBtn = document.getElementById('act-edit');
    if (editBtn) {
      editBtn.onclick = () => { location.hash = `#/books/${book.id}/edit`; };
    }

    /* 下架：确认后 DELETE（后端是逻辑下架，状态变 removed） */
    const removeBtn = document.getElementById('act-remove');
    if (removeBtn) {
      removeBtn.onclick = async () => {
        const ok = await confirmDialog({
          title: '下架书籍',
          message: '下架后买家将无法看到这本书。确定下架吗？',
          danger: true, okText: '确定下架',
        });
        if (!ok) return;
        try {
          await api.del(`/api/books/${encodeURIComponent(book.id)}`);
          toast('已下架', 'success');
          renderBookDetail(app, params);   /* 重新拉取详情刷新状态 */
        } catch { /* 错误已统一提示 */ }
      };
    }

    /* 重新上架：PUT 空 body，后端自动把 removed 改回 selling */
    const relistBtn = document.getElementById('act-relist');
    if (relistBtn) {
      relistBtn.onclick = async () => {
        try {
          await api.put(`/api/books/${encodeURIComponent(book.id)}`, {});
          toast('已重新上架', 'success');
          renderBookDetail(app, params);
        } catch { /* 错误已统一提示 */ }
      };
    }
  }

  /** 未登录时跳登录页（并记住回来地址） */
  function requireLogin() {
    if (me) return true;
    sessionStorage.setItem('redirect_after_login', location.hash);
    toast('请先登录', 'warning');
    location.hash = '#/login';
    return false;
  }
}

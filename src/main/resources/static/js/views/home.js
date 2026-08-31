/* ============================================================================
 * views/home.js —— 首页（书籍列表：搜索 + 筛选 + 排序 + 分页）
 * ============================================================================
 * 对应后端接口：
 *   GET /api/books
 *       参数（全部拼在 URL 上，注意是驼峰 pageNum/pageSize，不是文档里的 page/page_size）：
 *         keyword   搜索词（后端在 title/author/description 里模糊匹配）
 *         category  分类筛选（literature/textbook/... 小写英文）
 *         condition 成色筛选（brand_new/like_new/used/old）
 *         minPrice / maxPrice  价格区间 ⚠ 后端实际按【原价】过滤
 *         sortBy    排序 ⚠ 传的是 SQL 片段，见 constants.js 的 SORT_OPTIONS
 *         pageNum / pageSize   分页
 *   返回分页结构：{ records: [书籍卡片数据], total: 总条数 }
 *
 *   搜索建议：GET /api/books/search/suggestions?keyword=xxx（后端固定取 10 条）
 *
 * 每条书籍数据（BookListVO）里我们用到的字段：
 *   id / title / author / selling_price(字符串) / original_price(字符串) /
 *   condition / category / view_count / cover_image(后端恒为 null，用占位图) /
 *   seller_name(后端目前没填，可能为 null) / created_at
 * ========================================================================== */

import { api } from '../api.js';
import {
  CATEGORIES, CONDITIONS, SORT_OPTIONS,
  categoryLabel, conditionLabel,
} from '../constants.js';
import { coverHtml, escapeHtml, formatDate, renderPagination, emptyBlock, loadingBlock, debounce } from '../ui.js';

/* 每页条数。后端 PageHelper 默认 20，最大 50，这里取 12 凑成整齐的网格 */
const PAGE_SIZE = 12;

/* 把筛选条件存在模块级变量里：跳去详情页再回来，筛选条件还保留着（体验更好） */
const state = {
  keyword: '',
  category: '',
  condition: '',
  minPrice: '',
  maxPrice: '',
  sortBy: 'updated_at desc',   /* 默认“最新发布”，对应后端 SortBy.NEWEST */
  page: 1,
};

/**
 * 渲染首页
 * @param {HTMLElement} app 页面容器
 */
export async function renderHome(app) {
  /* ---------------- 1. 静态骨架：筛选栏 + 列表容器 + 分页容器 ---------------- */
  app.innerHTML = `
    <div class="page-title">淘二手书</div>

    <!-- 筛选栏 -->
    <div class="card">
      <div class="filter-bar">
        <!-- 搜索框：外面包一层 position-relative 是为了让“搜索建议下拉框”能
             绝对定位贴在输入框下方 -->
        <div class="position-relative">
          <input class="form-input" id="f-keyword" type="text"
                 placeholder="搜索书名 / 作者 / 描述" value="${escapeHtml(state.keyword)}" />
          <div id="suggest-box"></div>  <!-- 搜索建议下拉框挂载点 -->
        </div>

        <select class="form-select" id="f-category">
          <option value="">全部分类</option>
          ${CATEGORIES.map(c => `<option value="${c.value}" ${state.category === c.value ? 'selected' : ''}>${c.label}</option>`).join('')}
        </select>

        <select class="form-select" id="f-condition">
          <option value="">全部成色</option>
          ${CONDITIONS.map(c => `<option value="${c.value}" ${state.condition === c.value ? 'selected' : ''}>${c.label}</option>`).join('')}
        </select>

        <!-- 价格区间：注意后端按“原价”过滤，写进提示文字里 -->
        <div class="filter-price">
          <input class="form-input" id="f-min" type="number" min="0" step="0.01"
                 placeholder="最低价" value="${state.minPrice}" />
          <span>—</span>
          <input class="form-input" id="f-max" type="number" min="0" step="0.01"
                 placeholder="最高价" value="${state.maxPrice}" />
        </div>

        <select class="form-select" id="f-sort">
          ${SORT_OPTIONS.map(s => `<option value="${s.value}" ${state.sortBy === s.value ? 'selected' : ''}>${s.label}</option>`).join('')}
        </select>

        <button class="btn btn-primary" id="btn-search">搜索</button>
        <button class="btn btn-outline" id="btn-reset">重置</button>
      </div>
    </div>

    <!-- 书籍网格 + 分页：内容由 loadBooks() 填充 -->
    <div id="book-grid"></div>
    <div class="pagination" id="pager"></div>
  `;

  /* ---------------- 2. 绑定交互 ---------------- */

  /* 点击“搜索”按钮：回到第 1 页重新查 */
  document.getElementById('btn-search').onclick = () => {
    state.keyword  = document.getElementById('f-keyword').value.trim();
    state.category = document.getElementById('f-category').value;
    state.condition= document.getElementById('f-condition').value;
    state.minPrice = document.getElementById('f-min').value;
    state.maxPrice = document.getElementById('f-max').value;
    state.sortBy   = document.getElementById('f-sort').value;
    state.page = 1;
    loadBooks();
    hideSuggest();
  };

  /* 回车 = 点搜索 */
  document.getElementById('f-keyword').addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('btn-search').click();
  });

  /* 重置：清空所有条件（sortBy 回到默认值），回第 1 页 */
  document.getElementById('btn-reset').onclick = () => {
    Object.assign(state, { keyword: '', category: '', condition: '', minPrice: '', maxPrice: '', sortBy: 'updated_at desc', page: 1 });
    document.getElementById('f-keyword').value = '';
    document.getElementById('f-category').value = '';
    document.getElementById('f-condition').value = '';
    document.getElementById('f-min').value = '';
    document.getElementById('f-max').value = '';
    document.getElementById('f-sort').value = state.sortBy;
    loadBooks();
  };

  /* 搜索建议：输入停顿 300ms 后自动请求一次（防抖，避免每敲一个字就发请求） */
  document.getElementById('f-keyword').addEventListener('input', debounce(e => {
    const kw = e.target.value.trim();
    if (!kw) { hideSuggest(); return; }
    loadSuggest(kw);
  }, 300));

  /* 点击页面其他地方时收起搜索建议 */
  document.addEventListener('click', onDocClickHide);

  /* ---------------- 3. 首次加载数据 ---------------- */
  await loadBooks();

  /* ------------------------------------------------------------------------
   * 内部函数：查询并渲染书籍列表
   * ---------------------------------------------------------------------- */
  async function loadBooks() {
    const grid = document.getElementById('book-grid');
    grid.innerHTML = loadingBlock();

    try {
      const data = await api.get('/api/books', {
        keyword: state.keyword,
        category: state.category,
        condition: state.condition,
        minPrice: state.minPrice,
        maxPrice: state.maxPrice,
        sortBy: state.sortBy,
        pageNum: state.page,
        pageSize: PAGE_SIZE,
      });

      const records = data?.records ?? [];
      const total = data?.total ?? 0;

      if (records.length === 0) {
        grid.innerHTML = emptyBlock('没有找到相关书籍，换个关键词试试吧', '🔍');
        document.getElementById('pager').innerHTML = '';
        return;
      }

      /* 渲染书籍卡片网格 */
      grid.innerHTML = `<div class="book-grid">${records.map(renderCard).join('')}</div>`;

      /* 给每张卡片绑点击跳详情 */
      grid.querySelectorAll('.book-card').forEach(card => {
        card.onclick = () => { location.hash = `#/books/${card.dataset.id}`; };
      });

      /* 分页条：后端只给 total，总页数 = 向上取整(total / 每页条数) */
      renderPagination(
        document.getElementById('pager'),
        state.page,
        Math.ceil(total / PAGE_SIZE),
        p => { state.page = p; loadBooks(); }
      );
    } catch (err) {
      grid.innerHTML = emptyBlock('加载失败，请稍后重试', '⚠️');
    }
  }

  /** 单张书籍卡片 */
  function renderCard(b) {
    return `
      <div class="book-card" data-id="${escapeHtml(b.id)}">
        ${coverHtml(b.cover_image, 'cover', b.title)}
        <div class="info">
          <div class="title">${escapeHtml(b.title)}</div>
          <div class="author">《${escapeHtml(b.author)}》 · ${conditionLabel(b.condition)}</div>
          <div class="price-row">
            <span class="price"><span class="price-symbol">¥</span>${escapeHtml(b.selling_price ?? '—')}</span>
            ${b.original_price ? `<span class="price-original">原价 ¥${escapeHtml(b.original_price)}</span>` : ''}
          </div>
          <div class="meta">
            <span>${categoryLabel(b.category)}</span>
            <span>👀 ${b.view_count ?? 0}</span>
          </div>
          ${b.seller_name ? `<div class="meta"><span>卖家：${escapeHtml(b.seller_name)}</span><span>${formatDate(b.created_at)}</span></div>` : ''}
        </div>
      </div>
    `;
  }

  /* ------------------------------------------------------------------------
   * 内部函数：搜索建议
   * ---------------------------------------------------------------------- */
  async function loadSuggest(kw) {
    try {
      const data = await api.get('/api/books/search/suggestions', { keyword: kw }, { silent: true });
      const titles = (data?.records ?? []).map(b => b.title).filter(Boolean);
      if (titles.length === 0) { hideSuggest(); return; }

      const box = document.getElementById('suggest-box');
      box.innerHTML = `
        <div class="suggest-box">
          ${titles.map(t => `<div class="sug-item" data-t="${escapeHtml(t)}">${escapeHtml(t)}</div>`).join('')}
        </div>`;
      /* 点某条建议 → 直接拿它当关键词搜索 */
      box.querySelectorAll('.sug-item').forEach(item => {
        item.onclick = e => {
          e.stopPropagation();   /* 别让这次点击触发“点别处收起”的逻辑 */
          document.getElementById('f-keyword').value = item.dataset.t;
          document.getElementById('btn-search').click();
        };
      });
    } catch { /* 建议加载失败无所谓，静默忽略 */ }
  }

  function hideSuggest() {
    const box = document.getElementById('suggest-box');
    if (box) box.innerHTML = '';
  }

  function onDocClickHide(e) {
    /* 点在搜索区域外 → 收起建议。视图被切换时移除这个全局监听 */
    if (!e.target.closest('.position-relative')) hideSuggest();
    if (!document.body.contains(document.getElementById('f-keyword'))) {
      document.removeEventListener('click', onDocClickHide);
    }
  }
}

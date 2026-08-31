/* ============================================================================
 * views/userProfile.js —— 他人主页（公开页面，无需登录）+ 共享的“评价列表”
 * ============================================================================
 * 对应后端接口：
 *   GET /api/users/{userId}
 *       返回 UserProfileVO：
 *         { id, username, avatar_url, bio, credit_score,
 *           selling_count(在售数), sold_count(已售数), created_at }
 *       ⚠ 注意 /users/me 返回的字段名不同（avatar/score），两边不要搞混。
 *
 *   GET /api/users/{userId}/reviews?page=1&pageSize=20
 *       返回 { records:[ReviewVO], total, avg_rating, review_count, rating_map }
 *       ReviewVO：{ id, order_id, reviewer:{id,username,avatar_url},
 *                  rating, content, created_at }
 *       rating_map：{"1": 0, "2": 1, ..., "5": 10} 各星级的数量
 * ========================================================================== */

import { api } from '../api.js';
import { creditInfo } from '../constants.js';
import {
  escapeHtml, formatDate, badge, avatarHtml,
  renderStars, renderPagination, emptyBlock, loadingBlock,
} from '../ui.js';

/**
 * 渲染他人主页
 * @param {HTMLElement} app 页面容器
 * @param {{id:string}} params 路由参数（用户 ID）
 */
export async function renderUserProfile(app, params) {
  const userId = params.id;
  app.innerHTML = loadingBlock();

  let user;
  try {
    user = await api.get(`/api/users/${encodeURIComponent(userId)}`);
  } catch {
    return;
  }

  const credit = creditInfo(user.credit_score);

  app.innerHTML = `
    <div class="card" style="display:flex;gap:20px;align-items:center;flex-wrap:wrap">
      ${avatarHtml(user.username, user.avatar_url, 'avatar-lg')}
      <div style="flex:1;min-width:220px">
        <div style="font-size:18px;font-weight:800">
          ${escapeHtml(user.username)}
          ${badge(credit.text, credit.color)}
        </div>
        <div style="color:var(--text-light);font-size:13px;margin-top:4px">
          信誉分 <b>${user.credit_score ?? '—'}</b> ·
          在售 ${user.selling_count ?? 0} 本 ·
          已售 ${user.sold_count ?? 0} 本 ·
          加入于 ${formatDate(user.created_at)}
        </div>
        <div style="margin-top:8px;color:var(--text)">${escapeHtml(user.bio || '这个人很懒，什么都没写～')}</div>
      </div>
    </div>

    <!-- 评价列表（共享组件，我的资料页也用它） -->
    <div class="card">
      <div class="card-title">⭐ 收到的评价</div>
      <div id="reviews-box">${loadingBlock()}</div>
    </div>
  `;

  renderReviewsSection(document.getElementById('reviews-box'), userId);
}

/* ==========================================================================
 * 【共享】“收到的评价”区块：汇总（平均分/数量/分布） + 分页列表
 * 我的资料页（profile.js）也复用这个函数。
 * ========================================================================== */

/**
 * 渲染评价列表到指定容器
 * @param {HTMLElement} container 容器
 * @param {string} userId  被查看的用户 ID
 * @param {number} pageSize 每页条数
 */
export async function renderReviewsSection(container, userId, pageSize = 10) {
  container.innerHTML = loadingBlock();

  let page = 1;   /* 当前页码（切换页码只重画列表部分） */

  await load();

  async function load() {
    let data;
    try {
      data = await api.get(`/api/users/${encodeURIComponent(userId)}/reviews`, { page, pageSize });
    } catch {
      container.innerHTML = emptyBlock('评价加载失败', '⚠️');
      return;
    }

    const records = data?.records ?? [];

    /* ---------- 汇总区（平均分 + 分布条） ---------- */
    const avg = data?.avg_rating ?? 0;
    const count = data?.review_count ?? 0;
    const dist = data?.rating_map ?? {};

    let summaryHtml = '';
    if (count > 0) {
      summaryHtml = `
        <div style="display:flex;gap:24px;align-items:center;flex-wrap:wrap;margin-bottom:16px">
          <div style="text-align:center">
            <div style="font-size:34px;font-weight:800;color:var(--star)">${Number(avg).toFixed(1)}</div>
            <div>${renderStars(Math.round(avg))}</div>
            <div style="color:var(--text-light);font-size:12px">${count} 条评价</div>
          </div>
          <div style="flex:1;min-width:220px">
            <!-- 从 5 星到 1 星画分布条：条宽 = 该星数量 / 总数 * 100% -->
            ${[5, 4, 3, 2, 1].map(star => `
              <div class="dist-row">
                <span style="width:34px">${star} 星</span>
                <div class="dist-bar"><div style="width:${Math.round(((dist[String(star)] ?? 0) / count) * 100)}%"></div></div>
                <span style="width:24px;text-align:right">${dist[String(star)] ?? 0}</span>
              </div>
            `).join('')}
          </div>
        </div>`;
    }

    /* ---------- 列表区 ---------- */
    const listHtml = records.length === 0
      ? emptyBlock('还没有收到评价', '⭐')
      : records.map(r => `
          <div style="display:flex;gap:10px;padding:12px 0;border-bottom:1px solid var(--border)">
            ${avatarHtml(r.reviewer?.username, r.reviewer?.avatar_url, 'avatar-sm')}
            <div style="flex:1">
              <div style="display:flex;gap:8px;align-items:center">
                <b>${escapeHtml(r.reviewer?.username ?? '匿名')}</b>
                ${renderStars(r.rating)}
                <span style="color:var(--text-light);font-size:12px;margin-left:auto">${formatDate(r.created_at)}</span>
              </div>
              <div style="font-size:13px;margin-top:2px">${escapeHtml(r.content || '')}</div>
            </div>
          </div>`).join('');

    container.innerHTML = `
      ${summaryHtml}
      ${listHtml}
      <div class="pagination" id="reviews-pager"></div>
    `;

    /* 分页：后端返回的 total 是本页条数（后端小 quirk），但 records 是准确的，
       这里用 total 做总页数估算即可（数据量小时无影响） */
    renderPagination(
      container.querySelector('#reviews-pager'),
      page,
      Math.ceil((data?.total ?? 0) / pageSize) || (records.length === pageSize ? page + 1 : page),
      p => { page = p; load(); }
    );
  }
}

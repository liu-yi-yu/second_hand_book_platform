/* ============================================================================
 * views/profile.js —— 我的资料（查看 + 编辑 + 头像上传 + 我收到的评价）
 * ============================================================================
 * 对应后端接口：
 *   GET /api/users/me
 *       返回 UserGetVO —— ⚠ 字段名和其他接口不一样，容易踩坑：
 *         { id, username, email, avatar(不是 avatar_url！), score(不是 credit_score！),
 *           selling_count, sold_count, bio, created_at, created }
 *
 *   PUT /api/users/me
 *       ⚠⚠ 两个特别注意：
 *       1. Controller 方法参数上【没有 @RequestBody】，Spring 会从
 *          “查询参数/表单”里取值，所以请求要写成  PUT /api/users/me?bio=xxx&avatarUrl=yyy
 *          （参数名是驼峰 avatarUrl，对应 UserUpdateDTO 的属性名）
 *       2. 后端 UserMapper.update 的 SQL 目前【缺少 WHERE 条件】（已知 bug）：
 *          一旦传了字段，会把【所有用户】的资料一起改掉！
 *          → 界面上放置了醒目的风险警告，提交前还要求二次确认。
 *          （前端功能保留，但强烈建议先修复后端这条 SQL 再真实使用。）
 *
 *   头像上传：POST /api/upload/image（拿到 URL 后再通过上面的接口更新头像字段）
 * ========================================================================== */

import { api } from '../api.js';
import { getUser, patchUser } from '../store.js';
import { creditInfo } from '../constants.js';
import {
  escapeHtml, formatDate, badge, avatarHtml, toast,
  confirmDialog, loadingBlock,
} from '../ui.js';
import { renderNavbar } from '../ui.js';
import { renderReviewsSection } from './userProfile.js';

/**
 * 渲染我的资料页
 * @param {HTMLElement} app 页面容器
 */
export async function renderProfile(app) {
  app.innerHTML = loadingBlock();

  /* ---------------- 1. 拉取最新资料 ---------------- */
  let me;
  try {
    me = await api.get('/api/users/me');
  } catch {
    return;
  }
  if (!me) {
    app.innerHTML = loadingBlock('未获取到用户信息');
    return;
  }

  /* 同步一份到本地 store（顶栏头像/昵称用） */
  patchUser({
    id: me.id, username: me.username, email: me.email,
    avatar: me.avatar, score: me.score, bio: me.bio,
  });

  const credit = creditInfo(me.score);

  /* ---------------- 2. 页面骨架 ---------------- */
  app.innerHTML = `
    <div class="page-title">👤 我的资料</div>

    <div class="card" style="display:flex;gap:20px;align-items:center;flex-wrap:wrap">
      <div style="position:relative">
        <span id="my-avatar">${avatarHtml(me.username, me.avatar, 'avatar-lg')}</span>
      </div>
      <div style="flex:1;min-width:240px">
        <div style="font-size:18px;font-weight:800">
          ${escapeHtml(me.username)}
          ${badge(credit.text, credit.color)}　<span style="font-size:13px;color:var(--text-light)">信誉分 ${me.score ?? '—'}</span>
        </div>
        <div style="color:var(--text-light);font-size:13px;margin-top:4px">
          ${escapeHtml(me.email ?? '')} · 注册于 ${formatDate(me.created_at || me.created)}
        </div>
        <div style="display:flex;gap:20px;margin-top:10px;font-size:14px">
          <span>🛍️ 在售 <b>${me.selling_count ?? 0}</b> 本</span>
          <span>💰 已售 <b>${me.sold_count ?? 0}</b> 本</span>
        </div>
        <div style="margin-top:8px">${escapeHtml(me.bio || '还没有个人简介～')}</div>
      </div>
      <div class="actions">
        <button class="btn btn-outline" id="btn-avatar">更换头像</button>
        <button class="btn btn-outline" id="btn-logout">退出登录</button>
      </div>
    </div>

    <!-- 编辑资料卡（带风险警告） -->
    <div class="card" style="max-width:720px">
      <div class="card-title">编辑资料</div>

      <div class="alert alert-danger">
        ⚠️ <b>后端已知问题</b>：修改资料接口对应的 SQL 缺少 WHERE 条件，
        提交后可能会把<b>所有用户</b>的简介/头像一并修改。功能已保留，
        建议先修复后端 <code>UserMapper.xml</code> 的 update 语句后再使用。
      </div>

      <div class="form-item">
        <label class="form-label">个人简介（最多 200 字）</label>
        <textarea class="form-textarea" id="f-bio" maxlength="200"
                  placeholder="介绍一下自己，让买家/卖家更信任你">${escapeHtml(me.bio ?? '')}</textarea>
      </div>
      <button class="btn btn-primary" id="btn-save">保存</button>
    </div>

    <!-- 我收到的评价（复用他人主页的共享组件） -->
    <div class="card">
      <div class="card-title">⭐ 我收到的评价</div>
      <div id="my-reviews">${loadingBlock()}</div>
    </div>
  `;

  /* ---------------- 3. 交互 ---------------- */

  /* 3.1 更换头像：先上传图片拿 URL，再写进资料 */
  document.getElementById('btn-avatar').onclick = () => {
    /* 动态创建一个文件选择框，选完即上传 */
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/jpeg,image/png,image/webp';
    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) return;
      if (file.size > 10 * 1024 * 1024) { toast('图片不能超过 10MB', 'warning'); return; }
      try {
        toast('正在上传头像…', 'info');
        const img = await api.uploadImage(file);          /* { id, url } */
        await saveProfile({ avatarUrl: img.url });        /* 更新头像字段 */
      } catch { /* 错误已提示 */ }
    };
    input.click();
  };

  /* 3.2 保存简介 */
  document.getElementById('btn-save').onclick = async () => {
    const bio = document.getElementById('f-bio').value.trim();
    /* 因为后端有“会改到所有用户”的 bug，这里必须二次确认 */
    const ok = await confirmDialog({
      title: '确认保存？',
      message: '再次提醒：后端修改资料的 SQL 目前缺少 WHERE 条件，可能影响所有用户的资料。确定继续吗？',
      okText: '我已知晓，继续保存',
      danger: true,
    });
    if (!ok) return;
    await saveProfile({ bio });
  };

  /* 3.3 退出登录 */
  document.getElementById('btn-logout').onclick = async () => {
    const { clearSessionAndGo } = await import('../ui.js');
    clearSessionAndGo();
  };

  /* 3.4 我收到的评价（共享组件） */
  renderReviewsSection(document.getElementById('my-reviews'), me.id);

  /* ------------------------------------------------------------------------
   * 内部：保存资料。
   * ⚠ 走查询参数（不是 JSON 请求体！）：PUT /api/users/me?bio=...&avatarUrl=...
   *   参数名用驼峰（avatarUrl），和 UserUpdateDTO 的属性名一致。
   *   只传有值的参数，避免把另一个字段意外置空。
   * ---------------------------------------------------------------------- */
  async function saveProfile(fields) {
    try {
      const params = {};
      if (fields.bio !== undefined && fields.bio !== '') params.bio = fields.bio;
      if (fields.avatarUrl) params.avatarUrl = fields.avatarUrl;

      await api.put('/api/users/me', undefined, params);

      /* 更新本地 store + 顶栏 + 页面头像 */
      patchUser({ bio: params.bio ?? getUser()?.bio, avatar: params.avatarUrl ?? getUser()?.avatar });
      renderNavbar();
      toast('已保存', 'success');
      renderProfile(app);   /* 重新渲染整页，让头像/简介显示新值 */
    } catch { /* 错误已提示 */ }
  }
}

/* ============================================================================
 * views/auth.js —— 登录页 + 注册页（两个页面共用这一个文件）
 * ============================================================================
 * 对应后端接口：
 *   登录：POST /api/auth/login     body { username, password }
 *         ⚠ 注意：后端是用【用户名】查库的（selectByUsername），
 *           不是需求文档里写的邮箱！所以表单字段是用户名。
 *   注册：POST /api/auth/register  body { username, email, password }
 *
 * 后端返回（两个接口都一样）：
 *   { code:1, data:{ token, id, username, email, avatar_url, ... } }
 *   ——token 拿到的那一刻就算“登录成功”，我们把 token 和用户信息存进
 *     localStorage（store.js），然后跳回之前的页面或首页。
 * ========================================================================== */

import { api } from '../api.js';
import { setToken, setUser } from '../store.js';
import { toast, renderNavbar } from '../ui.js';

/**
 * 渲染登录/注册页
 * @param {HTMLElement} app    页面容器
 * @param {'login'|'register'} mode 当前是登录还是注册
 */
export async function renderAuth(app, mode) {
  const isLogin = mode === 'login';

  app.innerHTML = `
    <div class="auth-wrap">
      <div class="auth-card">
        <div class="auth-title">📚 校园二手书</div>
        <div class="auth-sub">${isLogin ? '登录你的账号，继续淘好书' : '注册一个新账号，开始卖闲置书'}</div>

        <!-- 表单：novalidate 表示“别用浏览器默认校验”，我们自己控制提示样式 -->
        <form id="auth-form" novalidate>
          <div class="form-item">
            <label class="form-label">用户名<span class="required">*</span></label>
            <input class="form-input" name="username" id="f-username"
                   placeholder="3~20 位字母、数字或下划线" autocomplete="username" />
          </div>

          <!-- 邮箱：只有注册需要（后端按用户名登录，不需要邮箱） -->
          ${isLogin ? '' : `
          <div class="form-item">
            <label class="form-label">邮箱<span class="required">*</span></label>
            <input class="form-input" name="email" id="f-email"
                   type="email" placeholder="example@mail.com" autocomplete="email" />
          </div>`}

          <div class="form-item">
            <label class="form-label">密码<span class="required">*</span></label>
            <input class="form-input" name="password" id="f-password"
                   type="password" placeholder="8~64 位" autocomplete="current-password" />
          </div>

          <button type="submit" class="btn btn-primary btn-lg btn-block" id="auth-submit">
            ${isLogin ? '登 录' : '注 册'}
          </button>
        </form>

        <div class="auth-switch">
          ${isLogin
            ? `还没有账号？<a href="#/register">去注册 →</a>`
            : `已有账号？<a href="#/login">去登录 →</a>`}
        </div>
      </div>
    </div>
  `;

  /* ------------------------------------------------------------------------
   * 提交处理
   * ---------------------------------------------------------------------- */
  document.getElementById('auth-form').addEventListener('submit', async e => {
    e.preventDefault();   /* 阻止表单“真的提交导致整页刷新”，我们用 fetch */

    /* 收集输入，去掉首尾空格 */
    const username = document.getElementById('f-username').value.trim();
    const password = document.getElementById('f-password').value;
    const emailInput = document.getElementById('f-email');
    const email = emailInput ? emailInput.value.trim() : '';

    /* -------- 前端先做一轮基础校验（省得来回请求后端） -------- */
    if (username.length < 3 || username.length > 20) {
      toast('用户名长度必须在 3~20 位之间', 'warning'); return;
    }
    if (password.length < 8 || password.length > 64) {
      toast('密码长度必须在 8~64 位之间', 'warning'); return;
    }
    if (!isLogin && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      toast('邮箱格式不正确', 'warning'); return;
    }

    /* 提交期间禁用按钮，防止连点重复提交 */
    const btn = document.getElementById('auth-submit');
    btn.disabled = true;
    btn.textContent = '请稍候…';

    try {
      /* -------- 调后端 -------- */
      const data = isLogin
        ? await api.post('/api/auth/login', { username, password })
        : await api.post('/api/auth/register', { username, email, password });

      /* -------- 保存会话 --------
         data 里有 token、id、username、email、avatar_url 等字段
         （下划线命名，Jackson SNAKE_CASE 全局配置的结果） */
      setToken(data.token);
      setUser({
        id: data.id,
        username: data.username,
        email: data.email,
        avatar_url: data.avatar_url,
      });

      /* 顺手拉一次完整资料（含信誉分等），把 store 补充完整。
         用 catch 吞掉失败：即使这步失败也不影响登录本身。 */
      api.get('/api/users/me').then(me => {
        if (me) setUser({ ...data, ...me, avatar_url: me.avatar ?? data.avatar_url });
      }).catch(() => {});

      toast(isLogin ? '登录成功' : '注册成功，欢迎！', 'success');
      renderNavbar();               /* 顶栏立刻变成“已登录”样式 */

      /* 跳转：登录前如果被守卫拦截过，就回到原来想去的页面；否则回首页 */
      const redirect = sessionStorage.getItem('redirect_after_login');
      sessionStorage.removeItem('redirect_after_login');
      location.hash = redirect || '#/';
    } catch (err) {
      /* api.js 已经 toast 过错误信息（如“邮箱或密码错误”），
         这里不用再提示，只把按钮恢复可用即可 */
    } finally {
      btn.disabled = false;
      btn.textContent = isLogin ? '登 录' : '注 册';
    }
  });
}

/* ============================================================================
 * views/publish.js —— 发布书籍 / 编辑书籍（一个页面两种模式）
 * ============================================================================
 * 对应后端接口：
 *   发布：POST /api/books
 *         body（全部下划线命名）：
 *           title, author, isbn, original_price, selling_price,
 *           condition, category, description, image_ids: [图片ID]
 *         ⚠ 两个后端硬性要求（不满足会直接 500 崩溃）：
 *            1. original_price 必填（service 里 originalPrice.toString() 会 NPE）
 *            2. isbn 必填，且“括号化长度”要够（service 里
 *               Arrays.toString(isbn.split("-")).substring(13)，太短会越界）
 *               —— 前端用和后端一样的算法预检，详见下面 isbnOk()
 *         成功后 data=null（不回显），我们跳回首页。
 *
 *   编辑：PUT /api/books/{bookId}
 *         ⚠ 后端 updateBook 会【先删光旧的图片关联，再按 image_ids 重新插入】，
 *           所以编辑时必须把现有图片 ID 一起传回去，否则图片会被清空！
 *         ⚠ 后端只更新非 null 字段（SQL 动态拼接），所以没改的字段可以不传。
 *
 *   上传图片：POST /api/upload/image（multipart/form-data，字段名 file）
 *         返回 { id, url, thumbnail_url } —— 我们只收集 id 和 url。
 * ========================================================================== */

import { api } from '../api.js';
import { CATEGORIES, CONDITIONS } from '../constants.js';
import { escapeHtml, toast, loadingBlock } from '../ui.js';

/**
 * 渲染发布/编辑页
 * @param {HTMLElement} app 页面容器
 * @param {{id?:string}} params 有 params.id 就是编辑模式（书籍 ID）
 */
export async function renderPublish(app, params) {
  const editId = params?.id;            /* 有 ID = 编辑模式 */
  let book = null;

  app.innerHTML = loadingBlock('加载中…');

  /* ---------------- 编辑模式：先拉书籍原信息用于回填 ---------------- */
  if (editId) {
    try {
      book = await api.get(`/api/books/${encodeURIComponent(editId)}`);
      if (book.status === 'sold') {
        toast('已售出的书籍不允许修改', 'warning');
        location.hash = `#/books/${editId}`;
        return;
      }
    } catch {
      return;   /* 书不存在等错误已提示 */
    }
  }

  /* 图片列表：[{ id, url }]。
     编辑模式必须用书籍现有图片初始化（原因见文件头注释的 ⚠） */
  let images = (book?.images ?? []).map(img => ({ id: img.id, url: img.url }));

  /* ---------------- 1. 页面骨架 ---------------- */
  app.innerHTML = `
    <div class="page-title">${editId ? '编辑书籍' : '发布书籍'}</div>

    <div class="card" style="max-width:720px">
      <form id="book-form" novalidate>
        <div class="form-row">
          <div class="form-item">
            <label class="form-label">书名<span class="required">*</span></label>
            <input class="form-input" id="f-title" maxlength="200"
                   value="${escapeHtml(book?.title ?? '')}" placeholder="例如：高等数学（第七版）上册" />
          </div>
          <div class="form-item">
            <label class="form-label">作者<span class="required">*</span></label>
            <input class="form-input" id="f-author" maxlength="100"
                   value="${escapeHtml(book?.author ?? '')}" placeholder="例如：同济大学数学系" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">ISBN<span class="required">*</span></label>
            <input class="form-input" id="f-isbn"
                   value="${escapeHtml(book?.isbn ?? '')}" placeholder="10 或 13 位，可带连字符" />
            <div class="form-help">⚠ 后端限制：必填，去掉连字符后至少 11 位</div>
          </div>
          <div class="form-item">
            <label class="form-label">分类<span class="required">*</span></label>
            <select class="form-select" id="f-category">
              ${CATEGORIES.map(c => `<option value="${c.value}" ${book?.category === c.value ? 'selected' : ''}>${c.label}</option>`).join('')}
            </select>
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">成色<span class="required">*</span></label>
            <select class="form-select" id="f-condition">
              ${CONDITIONS.map(c => `<option value="${c.value}" ${book?.condition === c.value ? 'selected' : ''}>${c.label}</option>`).join('')}
            </select>
          </div>
          <div class="form-item">
            <label class="form-label">原价（元）<span class="required">*</span></label>
            <input class="form-input" id="f-original" type="number" min="0" step="0.01"
                   value="${book?.original_price ?? ''}" placeholder="用于展示划线价" />
            <div class="form-help">⚠ 后端限制：原价必填</div>
          </div>
        </div>

        <div class="form-item" style="max-width:240px">
          <label class="form-label">售价（元）<span class="required">*</span></label>
          <input class="form-input" id="f-price" type="number" min="0.01" step="0.01"
                 value="${book?.selling_price ?? ''}" placeholder="必须大于 0" />
        </div>

        <div class="form-item">
          <label class="form-label">描述</label>
          <textarea class="form-textarea" id="f-desc" maxlength="2000"
                    placeholder="书籍的使用情况、笔记多少、为什么转让……（最多 2000 字）">${escapeHtml(book?.description ?? '')}</textarea>
        </div>

        <!-- 图片上传区 -->
        <div class="form-item">
          <label class="form-label">实拍图片（最多 9 张）</label>
          <input type="file" id="f-files" accept="image/jpeg,image/png,image/webp" multiple style="display:none" />
          <button type="button" class="btn btn-outline" id="btn-pick">＋ 上传图片</button>
          <div class="form-help">支持 jpg / png / webp，单张最大 10MB，先上传后发布</div>
          <div id="img-preview" style="display:flex;gap:10px;flex-wrap:wrap;margin-top:10px"></div>
        </div>

        <div class="actions">
          <button type="submit" class="btn btn-primary btn-lg" id="btn-submit">
            ${editId ? '保存修改' : '发 布'}
          </button>
          <button type="button" class="btn btn-outline btn-lg" id="btn-cancel">取消</button>
        </div>
      </form>
    </div>
  `;

  /* ---------------- 2. 图片上传交互 ---------------- */
  renderPreview();

  /* 点“上传图片”按钮 → 触发隐藏的文件选择框 */
  document.getElementById('btn-pick').onclick = () => document.getElementById('f-files').click();

  /* 选中文件后：逐张上传到后端（阿里云 OSS），拿到 {id,url} 存进 images */
  document.getElementById('f-files').addEventListener('change', async e => {
    const files = [...e.target.files];
    if (files.length === 0) return;
    if (images.length + files.length > 9) {
      toast('最多上传 9 张图片', 'warning');
      return;
    }
    for (const file of files) {
      if (file.size > 10 * 1024 * 1024) {          /* 后端限制单张 10MB */
        toast(`「${file.name}」超过 10MB，已跳过`, 'warning');
        continue;
      }
      try {
        toast(`正在上传 ${file.name}…`, 'info');
        const img = await api.uploadImage(file);   /* { id, url, thumbnail_url } */
        images.push({ id: img.id, url: img.url });
        renderPreview();
      } catch { /* 失败提示已由 api 层弹出 */ }
    }
    e.target.value = '';   /* 清空 file input，方便再次选择同一文件 */
  });

  /** 画图片预览（每张右上角有个 × 删除按钮） */
  function renderPreview() {
    const box = document.getElementById('img-preview');
    box.innerHTML = images.map((img, idx) => `
      <div style="position:relative">
        <img src="${escapeHtml(img.url)}" style="width:72px;height:96px;object-fit:cover;border-radius:6px;background:var(--bg)" alt="" />
        <button type="button" data-idx="${idx}" class="btn btn-danger btn-sm"
                style="position:absolute;top:-6px;right:-6px;padding:0 6px">×</button>
      </div>
    `).join('');
    box.querySelectorAll('button[data-idx]').forEach(btn => {
      btn.onclick = () => {
        images.splice(Number(btn.dataset.idx), 1);   /* 只是从列表移除，后端 24h 后自动清理未引用图片 */
        renderPreview();
      };
    });
  }

  /* ---------------- 3. 提交 ---------------- */
  document.getElementById('btn-cancel').onclick = () => {
    location.hash = editId ? `#/books/${editId}` : '#/';
  };

  document.getElementById('book-form').addEventListener('submit', async e => {
    e.preventDefault();

    /* -------- 收集 + 校验 -------- */
    const title = document.getElementById('f-title').value.trim();
    const author = document.getElementById('f-author').value.trim();
    const isbn = document.getElementById('f-isbn').value.trim();
    const original = document.getElementById('f-original').value;
    const price = document.getElementById('f-price').value;
    const condition = document.getElementById('f-condition').value;
    const category = document.getElementById('f-category').value;
    const description = document.getElementById('f-desc').value.trim();

    if (!title)  return toast('请填写书名', 'warning');
    if (!author) return toast('请填写作者', 'warning');
    if (!isbnOk(isbn)) return toast('ISBN 必填，去掉连字符后至少 11 位', 'warning');
    if (original === '' || Number(original) < 0) return toast('请填写原价（后端必填）', 'warning');
    if (price === '' || Number(price) <= 0) return toast('售价必须大于 0', 'warning');

    /* -------- 组装请求体（下划线字段名！） -------- */
    const body = {
      title,
      author,
      isbn,
      original_price: Number(original),
      selling_price: Number(price),
      condition,
      category,
      description: description || null,
      /* 图片 ID 列表。编辑时必须全量回传（后端先删后插）！ */
      image_ids: images.map(i => i.id),
    };

    const btn = document.getElementById('btn-submit');
    btn.disabled = true;
    btn.textContent = '提交中…';

    try {
      if (editId) {
        await api.put(`/api/books/${encodeURIComponent(editId)}`, body);
        toast('保存成功', 'success');
        location.hash = `#/books/${editId}`;
      } else {
        /* 后端发布成功不返回书籍 ID（data=null），所以只能跳回首页 */
        await api.post('/api/books', body);
        toast('发布成功！', 'success');
        location.hash = '#/';
      }
    } catch { /* 错误已统一提示 */ }

    btn.disabled = false;
    btn.textContent = editId ? '保存修改' : '发 布';
  });

  /* ------------------------------------------------------------------------
   * ISBN 预检：用和后端完全一样的逻辑算“括号化长度”。
   * 后端代码：Arrays.toString(isbn.split("-")).substring(13)
   *   Arrays.toString(["978","7","111"]) 会得到 "[978, 7, 111]"（元素用 ", " 连接）
   *   只要这个字符串长度 < 13，substring(13) 就会抛异常 → 前端提前拦下。
   * ------------------------------------------------------------------------- */
  function isbnOk(isbn) {
    if (!isbn) return false;
    const bracketed = '[' + isbn.split('-').join(', ') + ']';
    return bracketed.length >= 13 && /^[0-9Xx\-]+$/.test(isbn);
  }
}

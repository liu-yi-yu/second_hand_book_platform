# 二手书交易平台 — 后端需求文档

> 本文档按每周渐进式推进，描述每个阶段需实现的接口、请求/响应数据格式及核心业务规则。文档中不含代码，仅定义接口契约。

---

## 通用约定

以下约定适用于所有接口。

### 基础路径

```
开发环境: http://localhost:8080/api
```

### 认证方式

使用 JWT（JSON Web Token），登录成功后返回 `access_token` 和 `refresh_token`。

- `access_token`：有效期 2 小时，所有需认证接口在请求头携带：
  ```
  Authorization: Bearer <access_token>
  ```
- `refresh_token`：有效期 7 天，用于在 access_token 过期后无感续期。

### 统一响应信封

所有接口均返回以下 JSON 结构：

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | integer | 业务状态码，成功时为 `0` |
| message | string | 状态描述，成功时为 `"ok"` |
| data | object / array / null | 实际返回数据 |

**分页响应（data 内部额外字段）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| list | array | 当前页数据 |
| total | integer | 总记录数 |
| page | integer | 当前页码（从 1 开始） |
| page_size | integer | 每页条数 |
| total_pages | integer | 总页数 |

**错误响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | integer | 业务错误码（非 0） |
| message | string | 人类可读的错误描述 |
| data | null | 错误时恒为 `null` |

**通用错误码：**

| code | 含义 |
|------|------|
| 40001 | 参数校验失败 |
| 40002 | 资源不存在 |
| 40003 | 无权限操作 |
| 40004 | 资源状态不允许此操作 |
| 40101 | 未登录（access_token 缺失） |
| 40102 | access_token 过期 |
| 40103 | refresh_token 无效或过期 |
| 42901 | 请求频率超限 |
| 50001 | 服务器内部错误 |

---

# 第1周：用户系统 + 书籍发布 + 图片上传

> **目标：** 用户能注册、登录、维护个人信息；卖家能发布书籍并上传实拍照片。

---

## 1.1 用户注册

```
POST /api/auth/register
```

**请求头：** 无特殊要求。

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，3-20 字符，只能包含字母、数字、下划线 |
| email | string | 是 | 邮箱地址，需符合邮箱格式 |
| password | string | 是 | 密码，8-64 字符 |

> **请求体对象：** `UserDTO`

**成功响应（code=0）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.id | string | 用户唯一标识（UUID） |
| data.username | string | 用户名 |
| data.email | string | 邮箱 |
| data.created_at | string | 注册时间（ISO 8601） |

> **响应对象：** `UserVO` — 外层 `Result<UserVO>`

**可能的业务错误：**

| code | 含义 |
|------|------|
| 40001 | 用户名已存在 / 邮箱已被注册 / 参数格式不合法 |

**业务规则：**
- 密码使用 bcrypt 哈希后存储，不可明文落库。
- 注册成功后自动登录（同时返回 token），或要求用户跳转到登录页——这里选择前者，直接返回 token 对。

---

## 1.2 用户登录

```
POST /api/auth/login
```

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 登录邮箱 |
| password | string | 是 | 密码 |

> **请求体对象：** `UserDTO`

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.access_token | string | 访问令牌，有效期 2 小时 |
| data.refresh_token | string | 刷新令牌，有效期 7 天 |
| data.expires_in | integer | access_token 剩余有效秒数 |
| data.user.id | string | 用户 UUID |
| data.user.username | string | 用户名 |
| data.user.email | string | 邮箱 |
| data.user.avatar_url | string | 头像 URL，未设置时为 `null` |

> **响应对象：** `UserVO` — 外层 `Result<UserVO>`

**可能的业务错误：**

| code | 含义 |
|------|------|
| 40001 | 邮箱或密码错误 |
| 40004 | 账号已被禁用 |

---

## 1.3 刷新 Token

```
POST /api/auth/refresh
```

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refresh_token | string | 是 | 之前获取的 refresh_token |

> **请求体对象：** 无对应DTO — 仅 `refresh_token` 字符串

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.access_token | string | 新的访问令牌 |
| data.refresh_token | string | 新的刷新令牌（旧 token 立即失效，实现 token 轮转） |
| data.expires_in | integer | 新 access_token 剩余有效秒数 |

> **响应对象：** 无对应VO — data 直接返回 token 字段

**可能的业务错误：**

| code | 含义 |
|------|------|
| 40103 | refresh_token 无效或已过期或已被轮转 |

---

## 1.4 获取当前用户信息

```
GET /api/users/me
```

**请求头：** 需要 `Authorization: Bearer <access_token>`

**请求体：** 无。

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.id | string | 用户 UUID |
| data.username | string | 用户名 |
| data.email | string | 邮箱 |
| data.avatar_url | string \| null | 头像 URL |
| data.bio | string \| null | 个人简介 |
| data.credit_score | integer | 信誉分（默认 100） |
| data.selling_count | integer | 在售书籍数量 |
| data.sold_count | integer | 已售出数量 |
| data.created_at | string | 注册时间 |

> **响应对象：** `UserGetVO` — 外层 `Result<UserGetVO>`

---

## 1.5 修改当前用户信息

```
PUT /api/users/me
```

**请求头：** 需要认证。

**请求体（JSON，所有字段均可选）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 否 | 新用户名，3-20 字符 |
| bio | string | 否 | 个人简介，最长 200 字符 |

> **请求体对象：** `UserUpdateDTO`

**成功响应：** 返回更新后的用户完整信息（结构与 1.4 相同）。

> **响应对象：** `UserGetVO` — 外层 `Result<UserGetVO>`

**业务规则：**
- 用户名修改后需检查是否与已有用户重复。
- 头像修改使用单独的图片上传接口（见 1.9）。

---

## 1.6 查看用户主页

```
GET /api/users/:user_id
```

**请求头：** 无需认证（公开接口）。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| user_id | string | 目标用户 UUID |

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.id | string | 用户 UUID |
| data.username | string | 用户名 |
| data.avatar_url | string \| null | 头像 URL |
| data.bio | string \| null | 个人简介 |
| data.credit_score | integer | 信誉分 |
| data.selling_count | integer | 在售书籍数量 |
| data.sold_count | integer | 已售出数量 |
| data.created_at | string | 注册时间 |

> **响应对象：** `UserProfileVO` — 外层 `Result<UserProfileVO>`

---

## 1.7 发布书籍

```
POST /api/books
```

**请求头：** 需要认证。

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 书名，最长 200 字符 |
| author | string | 是 | 作者，最长 100 字符 |
| isbn | string | 否 | ISBN 号，10 位或 13 位 |
| original_price | number | 否 | 原价（元），精确到两位小数 |
| selling_price | number | 是 | 售价（元），必须 > 0，精确到两位小数 |
| condition | string | 是 | 成色，枚举值见下方 |
| category | string | 是 | 分类，枚举值见下方 |
| description | string | 否 | 书籍描述/卖点，最长 2000 字符 |
| image_ids | array of string | 否 | 已上传图片的 ID 列表（先调 1.9 上传图片，再传入此字段） |

> **请求体对象：** `BookCreateDTO`

**condition 枚举：**

| 值 | 含义 |
|------|------|
| like_new | 几乎全新，无任何使用痕迹 |
| minor_notes | 有少量笔记/划线 |
| folded | 有折痕或轻微破损 |
| worn | 较旧，有明显使用痕迹 |

**category 枚举：**

| 值 |
|------|
| literature | （文学小说） |
| social_science | （社会科学） |
| science_tech | （科学技术） |
| textbook | （教材教辅） |
| language | （语言学习） |
| art_design | （艺术设计） |
| life_style | （生活休闲） |
| other | （其他） |

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.id | string | 书籍 UUID |
| data.title | string | 书名 |
| data.author | string | 作者 |
| data.selling_price | number | 售价 |
| data.condition | string | 成色 |
| data.category | string | 分类 |
| data.status | string | 固定为 `selling`（在售） |
| data.images | array | 图片 URL 列表 |
| data.seller_id | string | 卖家用户 ID |
| data.created_at | string | 发布时间 |

> **响应对象：** `BookVO` — 外层 `Result<BookVO>`（简化版）

**业务规则：**
- 书籍发布后状态默认为 `selling`（在售）。
- 如果传了 `image_ids`，需校验这些图片是否属于当前用户且未被其他书籍使用。
- `selling_price` 最大不超过 99999.99 元。

---

## 1.8 修改 / 下架 / 查看 书籍

### 1.8.1 查看书籍详情

```
GET /api/books/:book_id
```

**请求头：** 无需认证。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| book_id | string | 书籍 UUID |

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.id | string | 书籍 UUID |
| data.title | string | 书名 |
| data.author | string | 作者 |
| data.isbn | string \| null | ISBN |
| data.original_price | number \| null | 原价 |
| data.selling_price | number | 售价 |
| data.condition | string | 成色 |
| data.category | string | 分类 |
| data.description | string \| null | 描述 |
| data.status | string | 状态：`selling` / `sold` / `removed` |
| data.images | array of {id, url} | 图片列表 |
| data.view_count | integer | 浏览次数 |
| data.seller.id | string | 卖家用户 ID |
| data.seller.username | string | 卖家用户名 |
| data.seller.avatar_url | string \| null | 卖家头像 |
| data.seller.credit_score | integer | 卖家信誉分 |
| data.created_at | string | 发布时间 |
| data.updated_at | string | 最后更新时间 |

> **响应对象：** `BookVO` — 外层 `Result<BookVO>`

**业务规则：**
- 每次访问详情，`view_count` +1（可用 Redis 异步累加，定时批量写回数据库，避免高频写库）。

---

### 1.8.2 修改书籍信息

```
PUT /api/books/:book_id
```

**请求头：** 需要认证。

**请求体（JSON，所有字段可选）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 书名 |
| author | string | 否 | 作者 |
| isbn | string | 否 | ISBN |
| original_price | number | 否 | 原价 |
| selling_price | number | 否 | 售价 |
| condition | string | 否 | 成色 |
| category | string | 否 | 分类 |
| description | string | 否 | 描述 |
| image_ids | array of string | 否 | 更新图片列表（全量替换） |

> **请求体对象：** `BookUpdateDTO`

**成功响应：** 返回更新后的书籍详情（同 1.8.1）。

> **响应对象：** `BookVO` — 外层 `Result<BookVO>`

**业务规则：**
- 只有卖家本人可以修改。
- 状态为 `sold`（已售出）的书籍不允许修改。
- 状态为 `removed`（已下架）的书籍可以修改后重新上架，此时状态需自动变为 `selling`。

---

### 1.8.3 下架书籍

```
DELETE /api/books/:book_id
```

**请求头：** 需要认证。

**成功响应：** `data: null`，状态码变为逻辑下架（软删除，`status` 变为 `removed`）。

> **响应对象：** `null`（data 为空）

**业务规则：**
- 只有卖家本人可以下架。
- 已有进行中订单的书籍（存在未完成的订单）不允许下架，需先处理完订单。

---

## 1.9 图片上传

```
POST /api/upload/image
```

**请求头：** 需要认证。

**请求体：** `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 图片文件 |

**文件限制：**
- 格式：jpg、jpeg、png、webp
- 单张最大 10 MB
- 每个用户每天最多上传 50 张

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.id | string | 图片唯一 ID（供发布/修改书籍时使用） |
| data.url | string | 图片访问 URL |
| data.width | integer | 图片宽度（像素） |
| data.height | integer | 图片高度（像素） |
| data.size | integer | 文件大小（字节） |

> **响应对象：** `ImageVO` — 外层 `Result<ImageVO>`（注意：文档含 width/height/size，ImageVO 暂无此字段，需补充）

**业务规则：**
- 上传后服务端生成缩略图（宽 400px），详情页使用缩略图，点击查看原图。
- 如果图片在 24 小时内没有被任何书籍引用，由定时任务自动清理。

---

### 第1周小结

第1周完成后，以下流程应可跑通：

```
用户注册 → 登录 → 发布书籍（含上传图片）→ 查看书籍列表 → 查看书籍详情
                                                      → 修改书籍 → 下架书籍
```

---

# 第2周：浏览发现 + 搜索 + 购物车

> **目标：** 用户可以按条件浏览/搜索书籍，并将心仪的书籍加入购物车。

---

## 2.1 书籍列表（首页）

```
GET /api/books
```

**请求头：** 无需认证。

**Query 参数：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数，最大 50 |
| category | string | 否 | — | 按分类筛选，值为 1.7 中 category 枚举 |
| condition | string | 否 | — | 按成色筛选，值为 1.7 中 condition 枚举 |
| min_price | number | 否 | — | 最低价（含） |
| max_price | number | 否 | — | 最高价（含） |
| sort_by | string | 否 | newest | 排序方式，枚举：`newest`（最新发布）、`price_asc`（价格升序）、`price_desc`（价格降序）、`popular`（浏览量最多） |
| keyword | string | 否 | — | 搜索关键词（与 2.2 共用同一个逻辑） |

**成功响应（分页结构）：**

data.list 中每项的字段同 1.8.1 的 data 结构（不含 `seller` 的信用分和 bio 等详细信息，仅含用户名和头像）。

> **响应对象：** `PageVO<BookListVO>` — 外层 `Result<PageVO<BookListVO>>`

**业务规则：**
- 只返回状态为 `selling` 的书籍。
- `sort_by=popular` 时按 `view_count` 降序排列。
- `min_price` 和 `max_price` 可单独或组合使用。

---

## 2.2 搜索

```
GET /api/books/search
```

**请求头：** 无需认证。

**Query 参数：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|------|
| q | string | 是 | — | 搜索关键词，最少 1 字符，最长 100 字符 |
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| category | string | 否 | — | 分类筛选 |
| sort_by | string | 否 | newest | 同 2.1 |

**成功响应：** 分页结构，list 中每项同 2.1。

> **响应对象：** `PageVO<BookListVO>` — 外层 `Result<PageVO<BookListVO>>`

**业务规则：**
- 搜索字段范围：`title`（书名）、`author`（作者）、`description`（描述）。
- 使用 PostgreSQL 的 `ILIKE` 或全文索引（`tsvector`）实现。初期可先用 `ILIKE '%keyword%'`，数据量大后迁移到 `tsvector` 或 Elasticsearch。
- 多个关键词用空格分隔，默认按 AND 逻辑匹配。

**搜索建议（可选，可后续迭代）：**

```
GET /api/books/search/suggestions?q=xxx
```

返回匹配的热门书名列表（最多 10 条），供前端搜索框自动补全。

---

## 2.3 购物车 — 查看

```
GET /api/cart
```

**请求头：** 需要认证。

**Query 参数：** 无。

**成功响应：**

data 为数组，每项结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| book_id | string | 书籍 UUID |
| book.title | string | 书名 |
| book.author | string | 作者 |
| book.selling_price | number | 售价 |
| book.condition | string | 成色 |
| book.cover_image | string \| null | 封面图（第一张图缩略图） |
| book.seller_name | string | 卖家用户名 |
| book.status | string | 书籍当前状态 |
| added_at | string | 加入购物车时间 |

> **响应对象：** 数组结构 — 类似 `CartItemVO`（文档嵌套结构与 CartItemVO 有差异）

**业务规则：**
- 购物车数据存储：可以是 Redis（以用户 ID 为 key 的 Hash）+ 定期同步到数据库，也可以直接存库。小规模用数据库即可。
- 如果购物车中某本书已被他人购买（status 变为 `sold`），应在返回时标记但不自动移除，由前端提示用户"该书籍已售出"。

---

## 2.4 购物车 — 添加

```
POST /api/cart
```

**请求头：** 需要认证。

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| book_id | string | 是 | 要加入的书籍 UUID |

> **请求体对象：** `CartAddDTO`

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.cart_count | integer | 购物车当前总数量 |

> **响应对象：** 无对应VO — data 仅返回 `cart_count`

**可能的业务错误：**

| code | 含义 |
|------|------|
| 40001 | 书籍不存在 |
| 40004 | 书籍已售出或已下架 |
| 40001 | 已在购物车中（重复添加） |
| 40001 | 不能将自己的书加入购物车 |

---

## 2.5 购物车 — 移除

```
DELETE /api/cart/:book_id
```

**请求头：** 需要认证。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| book_id | string | 要移除的书籍 UUID |

**成功响应：** `data: null`

> **响应对象：** `null`（data 为空）

**业务规则：**
- 移除不存在的购物车项不报错（幂等），直接返回成功。

---

## 2.6 购物车 — 批量获取选中项（结算用）

```
POST /api/cart/checkout-preview
```

**请求头：** 需要认证。

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| book_ids | array of string | 是 | 要结算的书籍 ID 列表，最少 1 个 |

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.items | array | 待结算书籍列表（每项含书名、单价、卖家信息） |
| data.total_amount | number | 总金额（元） |
| data.item_count | integer | 商品件数 |

> **响应对象：** 无对应VO — data 返回 `{ items, total_amount, item_count }`

**业务规则：**
- 需实时校验每本书的状态是否为 `selling`，如有已售出的需告知前端。
- 不同卖家的书可以一起结算，后端自动按卖家拆分成独立订单（见第3周）。

---

### 第2周小结

第2周完成后，以下流程应可跑通：

```
首页浏览（分页+筛选+排序）→ 搜索 → 查看详情 → 加入购物车 → 购物车管理 → 结算预览
```

---

# 第3周：订单系统（核心）

> **目标：** 实现完整的订单状态机，从下单到交易完成的全流程。

---

## 3.1 订单状态机

整个订单模块围绕以下状态流转设计：

```
                     ┌──────────┐
        ┌────────────│   取消    │◄─────────┐
        │            └──────────┘           │
        │  (买家取消)              (买家取消/卖家拒绝)│
        │                                  │
        ▼                                  │
   ┌──────────┐   卖家确认   ┌──────────┐   │
   │  待确认   │──────────►│  已确认   │   │
   └──────────┘            └──────────┘   │
                                  │        │
                             卖家发货     │
                                  │        │
                                  ▼        │
                           ┌──────────┐   │
                           │  已发货   │   │
                           └──────────┘   │
                                  │        │
                             买家收货     │
                                  │        │
                                  ▼        │
                           ┌──────────┐   │
                           │  已收货   │──┘ (超时自动完成)
                           └──────────┘
                                  │
                          买家确认/超时
                                  │
                                  ▼
                           ┌──────────┐
                           │  已完成   │
                           └──────────┘
```

**各状态说明：**

| 状态 | 含义 | 可执行操作的角色 |
|------|------|:--:|
| pending | 待卖家确认 | 买家：取消 / 卖家：确认、取消 |
| confirmed | 卖家已确认，等待发货 | 卖家：发货 |
| shipped | 卖家已发货，等待收货 | 买家：确认收货 |
| received | 买家已收货，等待自动完成 | 系统：24h 后自动完成 |
| completed | 交易完成 | 双方：互相评价 |
| cancelled | 已取消 | 不可操作 |

---

## 3.2 创建订单

```
POST /api/orders
```

**请求头：** 需要认证。

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| book_ids | array of string | 是 | 要购买的书籍 ID 列表 |

> **请求体对象：** `OrderCreateDTO`

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.orders | array | 创建的订单列表（按卖家拆分的每笔订单） |

每个 order 结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 订单 UUID |
| seller_id | string | 卖家 ID |
| seller_name | string | 卖家用户名 |
| book_id | string | 书籍 ID |
| book_title | string | 书名 |
| amount | number | 订单金额 |
| status | string | 固定为 `pending` |
| created_at | string | 下单时间 |

> **响应对象：** `OrderVO` 数组 — 外层 `Result`，data.orders 为列表

**业务规则：**
- 下单时必须校验每本书的当前状态为 `selling`。
- 一本书同时被多人下单：使用**乐观锁**策略——下单时检查书籍状态和版本号，防止超卖。
- 如果一批书来自多个卖家，后端自动拆分成多笔独立订单（每笔订单只含一本书、一个卖家）。
- 下单成功后，书籍状态立即变为 `sold`（已售出）。
- 如果同一本书在多个人的购物车中，一人下单后其他人在结算预览时会被告知"该书籍已售出"。
- 下单后自动从购物车中移除已下单的书籍。

---

## 3.3 卖家确认订单

```
PUT /api/orders/:order_id/confirm
```

**请求头：** 需要认证。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| order_id | string | 订单 UUID |

**请求体：** 无。

**成功响应：** 返回更新后的订单详情。

> **响应对象：** `OrderVO` — 外层 `Result<OrderVO>`

**可能的业务错误：**

| code | 含义 |
|------|------|
| 40003 | 不是该订单的卖家 |
| 40004 | 订单状态不是 `pending`，不能确认 |

**业务规则：**
- 卖家确认后状态变为 `confirmed`，`confirmed_at` 记录当前时间。
- 卖家如果在 48 小时内未确认，订单自动取消，书籍恢复 `selling` 状态。

---

## 3.4 卖家发货

```
PUT /api/orders/:order_id/ship
```

**请求头：** 需要认证（卖家）。

**请求体：** 无（初版无需物流单号，后续迭代可加入）。

**成功响应：** 返回更新后的订单详情。

> **响应对象：** `OrderVO` — 外层 `Result<OrderVO>`

**业务规则：**
- 发货后状态变为 `shipped`，`shipped_at` 记录当前时间。
- 卖家如果在确认后 72 小时内未发货，订单自动取消。

---

## 3.5 买家确认收货

```
PUT /api/orders/:order_id/receive
```

**请求头：** 需要认证。

**请求体：** 无。

**成功响应：** 返回更新后的订单详情。

> **响应对象：** `OrderVO` — 外层 `Result<OrderVO>`

**业务规则：**
- 收货后状态变为 `received`，`received_at` 记录当前时间。
- 收货后 24 小时自动变为 `completed`（给买家留检查书籍的时间）。
- 收货后不可再取消。

---

## 3.6 取消订单

```
PUT /api/orders/:order_id/cancel
```

**请求头：** 需要认证。

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | string | 否 | 取消原因，最长 500 字符 |

> **请求体对象：** `OrderCancelDTO`

> **响应对象：** `OrderVO` — 外层 `Result<OrderVO>`

**业务规则：**
- `pending` 状态：买家或卖家均可取消。
- `confirmed` 状态：仅买家可取消（卖家已承诺交易）。
- `shipped` 及之后状态：不可取消。
- 取消后书籍状态恢复为 `selling`。
- 频繁取消的用户将扣除信誉分（见 3.10）。

---

## 3.7 查询订单

### 3.7.1 我买的

```
GET /api/orders?role=buyer
```

**Query 参数：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|------|
| role | string | 是 | — | 固定为 `buyer` |
| status | string | 否 | — | 按状态筛选，多个用逗号分隔，如 `pending,confirmed` |
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |

### 3.7.2 我卖的

```
GET /api/orders?role=seller
```

**Query 参数：** 同上，`role` 固定为 `seller`。

**成功响应（分页结构）：**

data.list 中每项订单详情：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 订单 UUID |
| book_id | string | 书籍 ID |
| book_title | string | 书名 |
| book_cover | string \| null | 封面图 |
| amount | number | 金额 |
| status | string | 当前状态 |
| buyer_id / seller_id | string | 对方用户 ID |
| buyer_name / seller_name | string | 对方用户名 |
| buyer_avatar / seller_avatar | string \| null | 对方头像 |
| created_at | string | 下单时间 |
| confirmed_at | string \| null | 卖家确认时间 |
| shipped_at | string \| null | 发货时间 |
| received_at | string \| null | 收货时间 |
| completed_at | string \| null | 完成时间 |

> **响应对象：** `PageVO<OrderListVO>` — 外层 `Result<PageVO<OrderListVO>>`

---

## 3.8 订单详情

```
GET /api/orders/:order_id
```

**请求头：** 需要认证（买家或卖家）。

**成功响应：** 返回单条订单完整信息（同 3.7 列表项结构，外加 `cancelled_at` 和 `cancel_reason` 字段）。

> **响应对象：** `OrderVO` — 外层 `Result<OrderVO>`

---

## 3.9 超时自动处理

以下场景由**后台定时任务**处理（每分钟或每 5 分钟扫描一次）：

| 条件 | 动作 |
|------|------|
| 下单后 48h 卖家未确认 | 自动取消，书籍恢复 `selling` |
| 卖家确认后 72h 未发货 | 自动取消，书籍恢复 `selling` |
| 买家收货后 24h 未确认 | 自动变为 `completed` |

---

## 3.10 信誉分机制

| 行为 | 分值变化 |
|------|------|
| 卖家 48h 内未确认（超时取消） | -5 |
| 卖家确认后 72h 未发货（超时取消） | -10 |
| 买家频繁取消（一周内超 3 次） | 第 4 次起每次 -3 |
| 完成一笔交易（双方） | +2 |
| 收到差评 | 根据评价等级扣分 |

**业务规则：**
- 信誉分最低为 0。
- 信誉分低于 60 的用户，下单时前端展示警告提示（后端不做硬性拦截，作为软约束）。

---

### 第3周小结

第3周完成后，完整的交易闭环应可跑通：

```
选择书籍 → 创建订单 → 待卖家确认 → 卖家发货 → 买家收货 → 交易完成
                           ↘ 超时/取消 → 书籍恢复在售
```

---

# 第4周：即时通讯 + 评价系统

> **目标：** 买家卖家可在订单内实时聊天；交易完成后互相评价。

---

## 4.1 WebSocket 聊天连接

### 连接信息

| 项目 | 值 |
|------|------|
| 协议 | WebSocket（`ws://` 开发 / `wss://` 生产） |
| 端点 | `/ws/chat` |
| 认证 | 连接时在 URL 参数中携带 `?token=<access_token>` |
| 心跳 | 客户端每 30 秒发送 `ping`，服务端回复 `pong`；超过 60 秒无心跳则断开 |

### 连接建立流程

```
客户端                           服务端
  │                                │
  │── ws://host/ws/chat?token=xxx ──►
  │                                │ 验证 token
  │◄──── { "type": "connected" } ──│ 连接成功，加入该用户所有订单的聊天房间
  │                                │
  │── { "type": "ping" } ─────────►│ 心跳
  │◄──── { "type": "pong" } ──────│
```

### 房间隔离

- 聊天以**订单**为房间单位。
- 用户连接后自动加入其所有未完成订单的房间。
- 只有订单的买卖双方可以进入对应房间。
- 订单完成后房间变为只读（只能拉取历史，不能发送新消息）。

---

## 4.2 发送消息

**客户端 → 服务端：**

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | 固定为 `send_message` |
| order_id | string | 所属订单 UUID |
| content | string | 消息内容，最长 2000 字符 |
| client_id | string | 客户端生成的消息临时 ID，用于去重和前端乐观更新 |

> **请求体对象：** `MessageCreateDTO`（WebSocket 帧）

**服务端 → 发送方（确认）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | `message_ack` |
| client_id | string | 回传客户端临时 ID |
| message_id | string | 服务端生成的消息 UUID |
| created_at | string | 服务端接收时间（ISO 8601） |

**服务端 → 接收方（推送）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | `new_message` |
| message.id | string | 消息 UUID |
| message.order_id | string | 所属订单 UUID |
| message.sender_id | string | 发送者用户 ID |
| message.sender_name | string | 发送者用户名 |
| message.sender_avatar | string \| null | 发送者头像 |
| message.content | string | 消息内容 |
| message.created_at | string | 发送时间 |

> **响应对象：** WebSocket 帧格式 — `send_message`/`message_ack`/`new_message`，非HTTP Response，无对应VO类

**业务规则：**
- 消息服务端落库（数据库 `messages` 表）。
- 如果接收方不在线，消息依然落库，下次连接后可拉取。
- 发送方通过 `client_id` 实现去重——同一 `client_id` 的消息不重复落库。

---

## 4.3 拉取消息历史

```
GET /api/orders/:order_id/messages
```

**请求头：** 需要认证（买家或卖家）。

**Query 参数：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|------|
| before_id | string | 否 | — | 拉取此消息 ID 之前的历史（向上翻页） |
| limit | integer | 否 | 50 | 每次拉取条数，最大 100 |

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.messages | array | 消息列表（按时间降序，最新的在前） |
| data.has_more | boolean | 是否还有更早的消息 |

> **响应对象：** `MessageVO` 列表 — 外层包装 `{ messages: MessageVO[], has_more: boolean }`

每条消息结构同 4.2 中的 `message`。

---

## 4.4 未读消息计数

```
GET /api/messages/unread-count
```

**请求头：** 需要认证。

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.total | integer | 总未读消息数 |
| data.by_order | array | 按订单分组的未读计数 |

`by_order` 中每项：

| 字段 | 类型 | 说明 |
|------|------|------|
| order_id | string | 订单 UUID |
| book_title | string | 书名 |
| other_user_name | string | 对方用户名 |
| unread_count | integer | 该订单的未读消息数 |
| latest_message_preview | string | 最新消息的前 50 字符 |

> **响应对象：** 无对应VO — 返回聚合统计 `{ total, by_order }`

**业务规则：**
- 当用户在某个订单聊天页面（或 WebSocket 连接中收到该订单的消息）时，自动标记该订单消息为已读。
- 提供单独的标记已读接口供前端调用：

  ```
  PUT /api/orders/:order_id/messages/read
  ```

---

## 4.5 评价系统

### 4.5.1 提交评价

```
POST /api/reviews
```

**请求头：** 需要认证。

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| order_id | string | 是 | 订单 UUID |
| rating | integer | 是 | 评分 1-5（1=非常差，5=非常好） |
| comment | string | 否 | 评价文字，最长 500 字符 |

> **请求体对象：** `ReviewCreateDTO`

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.id | string | 评价 UUID |
| data.order_id | string | 订单 UUID |
| data.reviewer_id | string | 评价者 ID |
| data.target_user_id | string | 被评价者 ID |
| data.rating | integer | 评分 |
| data.comment | string \| null | 评价内容 |
| data.created_at | string | 评价时间 |

> **响应对象：** `ReviewVO` — 外层 `Result<ReviewVO>`

**业务规则：**
- 只有订单状态为 `completed` 时可以评价。
- 买家只能评价卖家，卖家只能评价买家——由后端根据当前登录用户和订单中的角色自动判断。
- 同一人对同一订单只能评价一次（唯一约束：`order_id + reviewer_id`）。

---

### 4.5.2 查看用户收到的评价

```
GET /api/users/:user_id/reviews
```

**请求头：** 无需认证。

**Query 参数：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |

**成功响应（分页结构）：**

data.list 中每项：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 评价 UUID |
| rating | integer | 评分 |
| comment | string \| null | 评价内容 |
| reviewer_name | string | 评价者用户名 |
| reviewer_avatar | string \| null | 评价者头像 |
| order_book_title | string | 订单对应的书名 |
| created_at | string | 评价时间 |

额外返回聚合统计：

| 字段 | 类型 | 说明 |
|------|------|------|
| data.avg_rating | number | 平均评分 |
| data.review_count | integer | 评价总数 |
| data.rating_distribution | object | 各星级评价数量，如 `{"5": 10, "4": 3, "3": 1, "2": 0, "1": 1}` |

> **响应对象：** `PageVO<ReviewVO>` — data 额外含 `avg_rating` / `review_count` / `rating_distribution`

---

### 第4周小结

第4周完成后，以下流程应可跑通：

```
订单中打开聊天 → 实时发送/接收消息 → 拉取历史消息 → 交易完成 → 双方互评 → 查看评价记录和信誉分
```

---

# 第5周：通知系统 + 管理后台

> **目标：** 用户能收到关键事件通知；管理员可管理用户、书籍和订单。

---

## 5.1 站内通知

### 5.1.1 通知列表

```
GET /api/notifications
```

**请求头：** 需要认证。

**Query 参数：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|------|------|
| page | integer | 否 | 1 | 页码 |
| page_size | integer | 否 | 20 | 每页条数 |
| unread_only | boolean | 否 | false | 仅显示未读 |

**成功响应（分页结构）：**

data.list 中每项：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 通知 UUID |
| type | string | 通知类型（见下方） |
| title | string | 通知标题 |
| content | string | 通知内容 |
| is_read | boolean | 是否已读 |
| related_order_id | string \| null | 关联订单 ID（可跳转） |
| related_book_id | string \| null | 关联书籍 ID |
| created_at | string | 通知时间 |

额外字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| data.unread_count | integer | 未读通知总数 |

> **响应对象：** `PageVO<NotificationVO>` — 外层 `Result`，data 额外含 `unread_count`

**通知类型枚举：**

| type | 触发时机 |
|------|----------|
| order_created | 有人购买你的书 |
| order_confirmed | 卖家已确认订单 |
| order_shipped | 卖家已发货 |
| order_received | 买家已收货 |
| order_completed | 交易完成 |
| order_cancelled | 订单被取消 |
| new_message | 收到新消息（聚合：5 分钟内同一订单只发一条） |
| review_received | 收到新评价 |
| book_sold_out | 你收藏的书被买走了 |

---

### 5.1.2 标记通知为已读

```
PUT /api/notifications/read
```

**请求体（JSON）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ids | array of string | 否 | 指定要标记的通知 ID 列表。不传则全部标记为已读 |

**成功响应：** `data.affected_count` 为被标记的数量。

> **响应对象：** 无对应VO — data 返回 `{ affected_count: int }`

---

## 5.2 管理后台

> 管理后台接口均需管理员角色。管理员由数据库 `users` 表的 `role` 字段控制（`admin` / `user`）。

### 5.2.1 用户管理

**用户列表：**

```
GET /api/admin/users
```

| Query 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | integer | 否 | 页码 |
| page_size | integer | 否 | 每页条数 |
| keyword | string | 否 | 按用户名或邮箱搜索 |
| role | string | 否 | 按角色筛选 |
| status | string | 否 | 按状态筛选：`active` / `disabled` |

**成功响应（分页）：** 每个用户含 id、username、email、role、status、credit_score、created_at。

> **响应对象：** `PageVO<UserProfileVO>` — admin 视图含 role/status 等额外字段

**禁用/启用用户：**

```
PUT /api/admin/users/:user_id/status
```

| 请求体字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 是 | `active` 或 `disabled` |

**业务规则：**
- 管理员不能禁用自己。
- 被禁用的用户无法登录，其所有在售书籍自动下架。

> **响应对象：** `null`（data 为空）

---

### 5.2.2 书籍审核

**书籍列表（管理视图）：**

```
GET /api/admin/books
```

比普通列表多了：`status`（所有状态都可见）、`report_count`（被举报次数）、`seller_name`。

> **响应对象：** `PageVO<BookListVO>` — admin 视图需额外字段

**强制下架：**

```
PUT /api/admin/books/:book_id/remove
```

- 管理员可强制下架违规书籍，会记录操作日志。
- 假设后续前端有举报功能，管理员经审核后操作。

> **响应对象：** `null`（data 为空）

---

### 5.2.3 订单仲裁

**订单列表（管理视图）：**

```
GET /api/admin/orders
```

> **响应对象：** `PageVO<OrderListVO>`

**强制取消订单：**

```
PUT /api/admin/orders/:order_id/cancel
```

| 请求体字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | string | 是 | 取消原因（必填，供日志追溯） |

**业务规则：**
- 管理员取消订单后，书籍恢复 `selling` 状态。
- 取消原因记录到操作日志和订单的 `cancel_reason` 字段。
- 管理员操作需写入操作日志表。

> **响应对象：** `null`（data 为空）

---

### 5.2.4 简易数据看板

```
GET /api/admin/dashboard
```

**请求头：** 需要认证（admin）。

**成功响应：**

| 字段 | 类型 | 说明 |
|------|------|------|
| data.total_users | integer | 注册用户总数 |
| data.active_users_7d | integer | 近 7 天活跃用户数（有登录行为） |
| data.total_books | integer | 在售书籍总数 |
| data.total_orders | integer | 总订单数 |
| data.completed_orders | integer | 已完成订单数 |
| data.total_trade_amount | number | 总交易金额 |
| data.orders_by_status | object | 各状态订单数量，如 `{"pending": 5, "confirmed": 3, ...}` |
| data.daily_new_orders | array | 近 7 天每天新增订单数 |

> **响应对象：** 无对应VO — data 为 8 个统计字段的 dashboard 对象

---

### 第5周小结

第5周完成后，平台具备基本运营能力：

```
用户收到通知 → 查看通知 → 标记已读
管理员 → 管理用户 → 审核书籍 → 仲裁订单 → 查看数据看板
```

---

# 附录A：建议后续补充的文档

以下是推荐在项目推进过程中逐步完善的配套文档：

## A.1 MySQL 数据库设计

> 使用 MySQL 8.0+，字符集 `utf8mb4`，排序规则 `utf8mb4_unicode_ci`。主键统一使用 `CHAR(36)` 存 UUID，时间字段使用 `DATETIME(3)`（毫秒精度），以 UTC 存储。

---

### A.1.0 建库

```sql
CREATE DATABASE book_trade
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE book_trade;
```

---

### A.1.1 用户表

```sql
CREATE TABLE users (
    id             CHAR(36)     NOT NULL,
    username       VARCHAR(20)  NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    avatar_url     VARCHAR(500) NULL,
    bio            VARCHAR(200) NULL,
    role           ENUM('user','admin')   NOT NULL DEFAULT 'user',
    status         ENUM('active','disabled') NOT NULL DEFAULT 'active',
    credit_score   INT          NOT NULL DEFAULT 100,
    last_login_at  DATETIME(3)  NULL,
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE INDEX idx_users_email (email),
    UNIQUE INDEX idx_users_username (username),
    INDEX idx_users_role_status (role, status),
    INDEX idx_users_credit_score (credit_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.2 图片表

```sql
CREATE TABLE images (
    id            CHAR(36)     NOT NULL,
    user_id       CHAR(36)     NOT NULL,
    url           VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500) NOT NULL,
    width         INT          NOT NULL,
    height        INT          NOT NULL,
    file_size     INT UNSIGNED NOT NULL,
    is_used       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_images_user_id (user_id),
    INDEX idx_images_created_at (created_at),
    INDEX idx_images_is_used (is_used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.3 书籍表

```sql
CREATE TABLE books (
    id             CHAR(36)     NOT NULL,
    seller_id      CHAR(36)     NOT NULL,
    title          VARCHAR(200) NOT NULL,
    author         VARCHAR(100) NOT NULL,
    isbn           VARCHAR(13)  NULL,
    original_price DECIMAL(10,2) NULL,
    selling_price  DECIMAL(10,2) NOT NULL,
    `condition`    ENUM('like_new','minor_notes','folded','worn') NOT NULL,
    category       ENUM('literature','social_science','science_tech',
                        'textbook','language','art_design',
                        'life_style','other') NOT NULL,
    description    VARCHAR(2000) NULL,
    status         ENUM('selling','sold','removed') NOT NULL DEFAULT 'selling',
    view_count     INT UNSIGNED NOT NULL DEFAULT 0,
    version        INT UNSIGNED NOT NULL DEFAULT 0,
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_books_seller_id (seller_id),
    INDEX idx_books_status_category (status, category),
    INDEX idx_books_status_created (status, created_at),
    INDEX idx_books_status_price (status, selling_price),
    INDEX idx_books_status_view (status, view_count),
    FULLTEXT INDEX idx_books_search (title, author, description) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.4 书籍-图片关联表

```sql
CREATE TABLE book_image_relations (
    id         CHAR(36)    NOT NULL,
    book_id    CHAR(36)    NOT NULL,
    image_id   CHAR(36)    NOT NULL,
    sort_order TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX  idx_bir_book_id (book_id),
    UNIQUE INDEX idx_bir_image_id (image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.5 购物车表

```sql
CREATE TABLE cart_items (
    id         CHAR(36)    NOT NULL,
    user_id    CHAR(36)    NOT NULL,
    book_id    CHAR(36)    NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_cart_user_id (user_id),
    UNIQUE INDEX idx_cart_user_book (user_id, book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.6 订单表

```sql
CREATE TABLE orders (
    id            CHAR(36)     NOT NULL,
    buyer_id      CHAR(36)     NOT NULL,
    seller_id     CHAR(36)     NOT NULL,
    book_id       CHAR(36)     NOT NULL,
    amount        DECIMAL(10,2) NOT NULL,
    status        ENUM('pending','confirmed','shipped',
                       'received','completed','cancelled') NOT NULL DEFAULT 'pending',
    cancel_reason VARCHAR(500) NULL,
    cancelled_by  ENUM('buyer','seller','admin','system') NULL,
    version       INT UNSIGNED NOT NULL DEFAULT 0,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    confirmed_at  DATETIME(3)  NULL,
    shipped_at    DATETIME(3)  NULL,
    received_at   DATETIME(3)  NULL,
    completed_at  DATETIME(3)  NULL,
    cancelled_at  DATETIME(3)  NULL,
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_orders_buyer_id (buyer_id, status),
    INDEX idx_orders_seller_id (seller_id, status),
    INDEX idx_orders_status_created (status, created_at),
    INDEX idx_orders_book_id (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.7 消息表

```sql
CREATE TABLE messages (
    id          CHAR(36)     NOT NULL,
    order_id    CHAR(36)     NOT NULL,
    sender_id   CHAR(36)     NOT NULL,
    receiver_id CHAR(36)     NOT NULL,
    content     VARCHAR(2000) NOT NULL,
    client_id   VARCHAR(64)  NOT NULL,
    is_read     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX  idx_messages_order_id (order_id, created_at),
    UNIQUE INDEX idx_messages_client_id (client_id),
    INDEX  idx_messages_receiver_unread (receiver_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.8 评价表

```sql
CREATE TABLE reviews (
    id             CHAR(36)    NOT NULL,
    order_id       CHAR(36)    NOT NULL,
    reviewer_id    CHAR(36)    NOT NULL,
    target_user_id CHAR(36)    NOT NULL,
    rating         TINYINT UNSIGNED NOT NULL,
    comment        VARCHAR(500) NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE INDEX idx_reviews_order_reviewer (order_id, reviewer_id),
    INDEX  idx_reviews_target_user (target_user_id, created_at),
    INDEX  idx_reviews_rating (target_user_id, rating),
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.9 通知表

```sql
CREATE TABLE notifications (
    id               CHAR(36)     NOT NULL,
    user_id          CHAR(36)     NOT NULL,
    type             VARCHAR(50)  NOT NULL,
    title            VARCHAR(200) NOT NULL,
    content          VARCHAR(500) NOT NULL,
    is_read          TINYINT(1)   NOT NULL DEFAULT 0,
    related_order_id CHAR(36)     NULL,
    related_book_id  CHAR(36)     NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_notif_user_read (user_id, is_read, created_at),
    INDEX idx_notif_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.10 收藏表

```sql
CREATE TABLE favorites (
    id         CHAR(36)    NOT NULL,
    user_id    CHAR(36)    NOT NULL,
    book_id    CHAR(36)    NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE INDEX idx_fav_user_book (user_id, book_id),
    INDEX idx_fav_book_id (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.11 管理员操作日志表

```sql
CREATE TABLE admin_operation_logs (
    id          CHAR(36)     NOT NULL,
    admin_id    CHAR(36)     NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    target_type VARCHAR(50)  NOT NULL,
    target_id   CHAR(36)     NOT NULL,
    detail      VARCHAR(1000) NULL,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_logs_admin_id (admin_id, created_at),
    INDEX idx_logs_target (target_type, target_id),
    INDEX idx_logs_action_created (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.12 Refresh Token 表

```sql
CREATE TABLE refresh_tokens (
    id         CHAR(36)     NOT NULL,
    user_id    CHAR(36)     NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(3)  NOT NULL,
    revoked    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE INDEX idx_rt_token_hash (token_hash),
    INDEX idx_rt_user_revoked (user_id, revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### A.1.13 外键约束

> 以下外键在实际编码时可选择是否创建物理外键。物理外键能保证数据完整性，但会略微影响写入性能。**建议开发阶段加上，生产环境视情况保留或改用应用层约束。**

```sql
ALTER TABLE images
    ADD CONSTRAINT fk_images_user FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE books
    ADD CONSTRAINT fk_books_seller FOREIGN KEY (seller_id) REFERENCES users(id);

ALTER TABLE book_image_relations
    ADD CONSTRAINT fk_bir_book  FOREIGN KEY (book_id)  REFERENCES books(id),
    ADD CONSTRAINT fk_bir_image FOREIGN KEY (image_id) REFERENCES images(id);

ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_cart_book FOREIGN KEY (book_id) REFERENCES books(id);

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_buyer  FOREIGN KEY (buyer_id)  REFERENCES users(id),
    ADD CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    ADD CONSTRAINT fk_orders_book   FOREIGN KEY (book_id)   REFERENCES books(id);

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
    ADD CONSTRAINT fk_messages_sender  FOREIGN KEY (sender_id)  REFERENCES users(id),
    ADD CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users(id);

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_order    FOREIGN KEY (order_id)       REFERENCES orders(id),
    ADD CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id)    REFERENCES users(id),
    ADD CONSTRAINT fk_reviews_target   FOREIGN KEY (target_user_id) REFERENCES users(id);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE favorites
    ADD CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_fav_book FOREIGN KEY (book_id) REFERENCES books(id);

ALTER TABLE admin_operation_logs
    ADD CONSTRAINT fk_logs_admin FOREIGN KEY (admin_id) REFERENCES users(id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id);
```

---

### A.1.14 表关系总览

```
users (1) ──────< books (N)               一个用户可发布多本书
users (1) ──────< cart_items (N)          一个用户可有多个购物车项
users (1) ──────< orders (N)              一个用户可有多个订单（买/卖）
users (1) ──────< messages (N)            一个用户可发送/接收多条消息
users (1) ──────< reviews (N)             一个用户可写/收多条评价
users (1) ──────< notifications (N)       一个用户可收到多条通知
users (1) ──────< images (N)              一个用户可上传多张图片
users (1) ──────< refresh_tokens (N)      一个用户可有多个 refresh token
users (1) ──────< favorites (N)           一个用户可收藏多本书
users (1) ──────< admin_operation_logs(N) 一个管理员可有多条操作日志

books (1) ──────< book_image_relations >── images
    一本书有多张图片（通过关联表）

books (1) ──────< cart_items (N)          一本书可被多人加入购物车
books (1) ──────< orders (N)              一本书可产生多个订单
books (1) ──────< favorites (N)           一本书可被多人收藏

orders (1) ──────< messages (N)           一个订单下有多条聊天消息
orders (1) ──────< reviews (N)            一个订单可产生双向评价（最多 2 条）
```

---

### A.1.15 关键业务查询示例

> 以下列出文档中各接口对应的核心 SQL 思路，供编码时参考。

**下订单（乐观锁防超卖）：**
```sql
-- 第 1 步：查询当前版本号
SELECT id, status, version FROM books WHERE id = :book_id;

-- 第 2 步：更新时带版本号条件
UPDATE books
SET status = 'sold', version = version + 1
WHERE id = :book_id AND status = 'selling' AND version = :old_version;
-- 如果 affected_rows = 0，说明并发冲突，回滚并重试或报错
```

**首页列表（分页+分类筛选+价格排序）：**
```sql
SELECT b.*, u.username, u.avatar_url
FROM books b
JOIN users u ON u.id = b.seller_id
WHERE b.status = 'selling'
  AND b.category = :category
ORDER BY b.selling_price ASC
LIMIT :limit OFFSET :offset;
```

**全文搜索：**
```sql
SELECT b.*, u.username, u.avatar_url
FROM books b
JOIN users u ON u.id = b.seller_id
WHERE b.status = 'selling'
  AND MATCH(b.title, b.author, b.description) AGAINST(:keyword IN BOOLEAN MODE)
ORDER BY b.created_at DESC
LIMIT :limit OFFSET :offset;
```

**未读消息计数（按订单分组）：**
```sql
SELECT
    m.order_id,
    b.title AS book_title,
    COUNT(*) AS unread_count
FROM messages m
JOIN orders o ON o.id = m.order_id
JOIN books  b ON b.id = o.book_id
WHERE m.receiver_id = :user_id AND m.is_read = 0
GROUP BY m.order_id, b.title;
```

**扫描超时订单（定时任务）：**
```sql
-- 48h 未确认 → 自动取消
SELECT id FROM orders
WHERE status = 'pending'
  AND created_at < DATE_SUB(NOW(), INTERVAL 48 HOUR);

-- 72h 未发货 → 自动取消
SELECT id FROM orders
WHERE status = 'confirmed'
  AND confirmed_at < DATE_SUB(NOW(), INTERVAL 72 HOUR);

-- 收货后 24h → 自动完成
SELECT id FROM orders
WHERE status = 'received'
  AND received_at < DATE_SUB(NOW(), INTERVAL 24 HOUR);
```

**清理 24h 未引用的图片：**
```sql
DELETE FROM images
WHERE is_used = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR);
```

---

### A.1.16 设计决策说明

| 决策 | 理由 |
|------|------|
| 主键使用 CHAR(36) 而非 INT 自增 | UUID 在分布式环境下无冲突；前端可直接生成，减少一次数据库往返；避免自增 ID 暴露业务规模 |
| 金额使用 DECIMAL(10,2) | 避免浮点精度问题，10 位总长 + 2 位小数 |
| 状态字段使用 ENUM | 约束合法值，防止脏数据；比 VARCHAR 更省空间 |
| 时间用 DATETIME(3) | 毫秒精度满足排序去重需求；UTC 存储避免时区混乱 |
| 乐观锁 version 字段 | books 和 orders 存在并发竞争，用版本号而非悲观锁，性能更好 |
| 图片独立表 + 关联表 | 图片可复用于不同书籍；is_used + 定时清理防止垃圾堆积 |
| 购物车直接存库 | 小规模完全可承载；高并发时迁移到 Redis Hash |
| FULLTEXT + ngram parser | 初期避免引入 Elasticsearch 运维成本；数据量上去后再迁移 |
| refresh_token 存 SHA-256 哈希 | 即使数据库泄露，攻击者也无法直接使用 token |
| 外键约束在 ALTER 中单独声明 | 方便按需开关（开发环境强制，生产环境可选） |

## A.2 前端页面路由规划

梳理完整的页面树（如 `/login`、`/books/:id`、`/orders/:id/chat`），标注每个页面的数据依赖接口、路由守卫策略（哪些页面需登录）、页面间跳转关系。

## A.3 部署运维

作为下一步改进方向，可考虑：Docker 容器化编排、环境变量管理、CI/CD 流水线、日志收集、监控告警等。本文档不做展开。

---

*文档版本: v1.0 | 最后更新: 2026-08-08*

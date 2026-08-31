/* ============================================================================
 * constants.js —— 全站共用的“枚举值 ↔ 中文”对照表
 * ============================================================================
 * 小白理解：
 * 后端数据库里存的分类、成色、订单状态等都是英文小写字符串（例如 "literature"、
 * "pending"），直接显示给用户很难看懂。这个文件就是一本“翻译词典”：
 *   - 页面上要显示中文时：拿英文值来查表
 *   - 下拉框要给后端传英文值时：也从这里取
 *
 * ⚠ 注意：这里的英文值必须和后端代码保持一致（以 yu-test1 后端代码为准，
 *   与需求文档里的枚举并不相同！比如后端成色是 brand_new/like_new/used/old，
 *   而文档里写的是 like_new/minor_notes/folded/worn）。
 * ========================================================================== */

/* ---------------------------------------------------------------------------
 * 1. 书籍分类（后端枚举：enumeration/BookCategory.java，存库为小写）
 * ------------------------------------------------------------------------- */
export const CATEGORIES = [
  { value: 'literature',   label: '文学小说' },
  { value: 'textbook',     label: '教材教辅' },
  { value: 'professional', label: '专业书籍' },
  { value: 'comic',        label: '漫画' },
  { value: 'children',     label: '儿童读物' },
  { value: 'novel',        label: '小说' },
  { value: 'magazine',     label: '杂志' },
  { value: 'other',        label: '其他' },
];

/* ---------------------------------------------------------------------------
 * 2. 书籍成色（后端枚举：enumeration/BookCondition.java）
 * ------------------------------------------------------------------------- */
export const CONDITIONS = [
  { value: 'brand_new', label: '全新' },
  { value: 'like_new',  label: '几乎全新' },
  { value: 'used',      label: '有使用痕迹' },
  { value: 'old',       label: '较旧' },
];

/* ---------------------------------------------------------------------------
 * 3. 书籍状态（后端常量：constant/BookStatu.java）
 * ------------------------------------------------------------------------- */
export const BOOK_STATUS_MAP = {
  selling: '在售',
  sold:    '已售出',
  removed: '已下架',
};

/* 书籍状态对应徽章颜色（CSS 里的 badge-xxx 类名） */
export const BOOK_STATUS_COLOR = {
  selling: 'green',
  sold:    'gray',
  removed: 'red',
};

/* ---------------------------------------------------------------------------
 * 4. 订单状态（后端常量：constant/OrderStatu.java）
 *    状态机：pending → confirmed → shipped → received → completed
 * ------------------------------------------------------------------------- */
export const ORDER_STATUS_MAP = {
  pending:    '待卖家确认',
  confirmed:  '待发货',
  shipped:    '已发货',
  received:   '买家已收货',
  completed:  '交易完成',
  cancelled:  '已取消',
};

/* 订单状态对应徽章颜色 */
export const ORDER_STATUS_COLOR = {
  pending:   'orange',
  confirmed: 'blue',
  shipped:   'purple',
  received:  'blue',
  completed: 'green',
  cancelled: 'red',
};

/* 正常流程的步骤顺序（订单详情页画“步骤条”用；cancelled 是分支不列入） */
export const ORDER_FLOW = ['pending', 'confirmed', 'shipped', 'received', 'completed'];

/* ---------------------------------------------------------------------------
 * 5. 通知类型（后端常量：constant/NotificationsType.java）
 * ------------------------------------------------------------------------- */
export const NOTIFICATION_TYPE_MAP = {
  order_created:    '有人购买了你的书',
  order_confirmed:  '卖家已确认订单',
  order_shipped:    '卖家已发货',
  order_received:   '买家已收货',
  order_completed:  '交易完成',
  order_cancelled:  '订单已取消',
  new_message:      '收到新消息',
  review_received:  '收到新评价',
  book_sold_out:    '收藏的书籍被买走',
  system:           '系统通知',
};

/* 每种通知配一个小图标（emoji，纯装饰） */
export const NOTIFICATION_ICON = {
  order_created:    '🛒',
  order_confirmed:  '✅',
  order_shipped:    '📦',
  order_received:   '📬',
  order_completed:  '🎉',
  order_cancelled:  '❌',
  new_message:      '💬',
  review_received:  '⭐',
  book_sold_out:    '💸',
  system:           '📢',
};

/* ---------------------------------------------------------------------------
 * 6. 排序选项 —— ⚠ 非常特殊的后端设计
 * ---------------------------------------------------------------------------
 * 后端 BookServiceImpl 里是这样用的：
 *     PageHelper.startPage(pageNum, pageSize, bookSearchDTO.getSortBy());
 * 也就是说 sortBy 参数会被直接拼进 SQL 的 ORDER BY 子句！
 * 因此前端不能传 "newest" 这种单词，必须传后端 SortBy 常量里的 SQL 片段
 * （constant/SortBy.java）：
 *     NEWEST       = "updated_at desc"
 *     PRICE_ASC    = "selling_price asc"
 *     PRICE_DESC   = "selling_price desc"
 *     POPULAR      = "view_count desc"
 *     CREATE_TIME_DESC = "created_at desc"
 * （URL 里的空格浏览器会自动编码成 %20，后端能正常收到，不用特殊处理。）
 * ------------------------------------------------------------------------- */
export const SORT_OPTIONS = [
  { value: 'updated_at desc',  label: '最新发布' },
  { value: 'selling_price asc',  label: '价格从低到高' },
  { value: 'selling_price desc', label: '价格从高到低' },
  { value: 'view_count desc',    label: '浏览最多' },
];

/* ---------------------------------------------------------------------------
 * 7. 通用查表函数
 * ---------------------------------------------------------------------------
 * 用法示例：categoryLabel('literature') → '文学小说'
 * 找不到时原样返回英文值，避免页面上出现 undefined。
 * ------------------------------------------------------------------------- */
export function getOptionLabel(list, value) {
  const hit = list.find(item => item.value === value);
  return hit ? hit.label : (value ?? '—');
}

/* 下面是几个快捷函数，视图代码里直接调用更省事 */
export const categoryLabel        = v => getOptionLabel(CATEGORIES, v);
export const conditionLabel       = v => getOptionLabel(CONDITIONS, v);
export const bookStatusLabel      = v => BOOK_STATUS_MAP[v] ?? v ?? '—';
export const orderStatusLabel     = v => ORDER_STATUS_MAP[v] ?? v ?? '—';
export const notificationLabel    = v => NOTIFICATION_TYPE_MAP[v] ?? (v ?? '通知');

/* 信誉分对应的颜色描述：≥90 优秀（绿）、≥60 良好（蓝）、<60 风险（红） */
export function creditInfo(score) {
  if (score == null) return { text: '未知', color: 'gray' };
  if (score >= 90) return { text: '信用优秀', color: 'green' };
  if (score >= 60) return { text: '信用良好', color: 'blue' };
  return { text: '信用风险', color: 'red' };
}

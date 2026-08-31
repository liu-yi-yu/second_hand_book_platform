/* ============================================================================
 * ws.js —— WebSocket 聊天客户端（第 4 周的实时聊天功能）
 * ============================================================================
 * 小白理解：
 * 1. HTTP 请求是“一问一答”：前端问一次，后端答一次，答完就断了。
 *    而 WebSocket 是一条“电话线”：双方一直连着，谁都能随时说话——
 *    这才做得到“对方一发消息，我这边立刻弹出来”。
 * 2. 后端聊天服务的入口是  ws://localhost:8081/ws/chat?token=<token>
 *    （token 直接放在地址里，后端在连接建立的那一刻校验身份）。
 * 3. 约定好的“暗号”（消息格式都是 JSON）：
 *    前端 → 后端：
 *      {type:"ping"}                              心跳（告诉后端我还活着）
 *      {type:"send_message", order_id, content, client_id}   发聊天消息
 *    后端 → 前端：
 *      {"type":"connected"}                       连接成功
 *      {"type":"pong"}                            心跳回应
 *      {"type":"message_ack", client_id, message_id, created_at}
 *                                                 “你发的那条我存好了”回执
 *      {"type":"new_message", message:{...}}      有人给你发了新消息
 * 4. “乐观上屏”：点发送后先立刻把消息画在屏幕上（半透明=发送中），
 *    收到 message_ack 再把那条消息变成正常样式。这样体验更流畅。
 *    client_id 是前端随机生成的一串字符，用来对上“哪条乐观消息对应哪条回执”，
 *    同时后端也用它做去重（网络重试不会存两条一样的消息）。
 * ========================================================================== */

/* 模块内部状态（不导出，外面碰不到） */
let ws = null;                  /* 当前 WebSocket 连接对象 */
let heartbeatTimer = null;      /* 心跳定时器 */
let reconnectTimer = null;      /* 重连定时器 */
let reconnectDelay = 2000;      /* 重连间隔：第一次 2 秒，失败后翻倍，最多 30 秒 */
let currentToken = null;        /* 连接用的 token（重连时要再次放地址里） */
let manuallyClosed = false;     /* 是否主动断开（退出登录时置 true，不再自动重连） */

/* 回执登记表：client_id → { resolve, timer }
   发消息时登记一条，收到 message_ack 时按 client_id 找到并“兑现”Promise */
const pendingAcks = new Map();

/* “收到新消息”的监听器列表：聊天页面、顶栏徽标都会注册自己的处理函数 */
const newMessageListeners = new Set();

/* “连接状态变化”的监听器（聊天页显示“已断线，重连中…”用） */
const statusListeners = new Set();

/**
 * 建立 WebSocket 连接（已登录时调用；重复调用不会建第二条）。
 * @param {string} token 登录拿到的 token（裸 token，不加 Bearer 前缀）
 */
export function connectChat(token) {
  if (!token) return;
  /* 已经连着或正在连：直接复用，不重复建 */
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }

  currentToken = token;
  manuallyClosed = false;

  /* location.host = "localhost:8081"，自动跟随当前访问的地址，不用写死 */
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
  ws = new WebSocket(`${proto}//${location.host}/ws/chat?token=${encodeURIComponent(token)}`);

  /* ---------------- 连接建立成功 ----------------
     注意：后端会主动发一条 {"type":"connected"}，真正的“就绪”以收到它为准 */
  ws.onopen = () => {
    reconnectDelay = 2000;                       /* 连上了就把重连间隔复位 */
    startHeartbeat();                            /* 启动心跳 */
    notifyStatus('connected');
  };

  /* ---------------- 收到后端消息 ---------------- */
  ws.onmessage = event => {
    let data;
    try { data = JSON.parse(event.data); } catch { return; }

    switch (data.type) {
      case 'connected':                          /* 连接成功通知 */
        notifyStatus('connected');
        break;

      case 'pong':                               /* 心跳回应，无需处理 */
        break;

      case 'message_ack': {                      /* 我发的消息被后端确认落库了 */
        const waiter = pendingAcks.get(data.client_id);
        if (waiter) {
          clearTimeout(waiter.timer);            /* 取消超时判定 */
          pendingAcks.delete(data.client_id);
          waiter.resolve(data);                  /* 把回执交给“发送消息”的 Promise */
        }
        break;
      }

      case 'new_message':                        /* 对方发来了新消息 */
        newMessageListeners.forEach(fn => {
          try { fn(data.message); } catch (e) { console.error('new_message 处理出错', e); }
        });
        break;
    }
  };

  /* ---------------- 连接断开 ---------------- */
  ws.onclose = () => {
    stopHeartbeat();
    notifyStatus('disconnected');
    /* 不是主动断开（说明是网络问题/后端重启），安排自动重连 */
    if (!manuallyClosed) scheduleReconnect();
  };

  ws.onerror = () => { /* onclose 也会跟着触发，重连逻辑放在 onclose 里统一处理 */ };
}

/** 断开连接（退出登录时调用），并且不再自动重连 */
export function disconnectChat() {
  manuallyClosed = true;
  clearTimeout(reconnectTimer);
  stopHeartbeat();
  if (ws) { try { ws.close(); } catch { /* 忽略 */ } }
  ws = null;
}

/* ---------------- 心跳：每 30 秒发一个 ping ----------------
   后端约定：超过 60 秒没心跳就把连接踢掉。所以我们必须定时“报平安”。 */
function startHeartbeat() {
  stopHeartbeat();
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'ping' }));
    }
  }, 30000);
}
function stopHeartbeat() {
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null; }
}

/* ---------------- 自动重连（间隔指数退避：2s → 4s → 8s … 上限 30s） ---------------- */
function scheduleReconnect() {
  clearTimeout(reconnectTimer);
  reconnectTimer = setTimeout(() => {
    if (!manuallyClosed && currentToken) {
      reconnectDelay = Math.min(reconnectDelay * 2, 30000);
      connectChat(currentToken);
    }
  }, reconnectDelay);
}

/* ==========================================================================
 * 对外功能 1：发送一条聊天消息
 * ========================================================================== */

/**
 * 发送消息，返回 Promise：
 *   - 成功：resolve(后端回执 { type, client_id, message_id, created_at })
 *   - 失败：reject（连接没建立 / 6 秒内没收到回执）
 *
 * @param {number} orderId 订单 ID（后端订单 ID 是整数！）
 * @param {string} content 消息文字
 */
export function sendChatMessage(orderId, content) {
  return new Promise((resolve, reject) => {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      reject(new Error('聊天连接未建立，请稍候重试'));
      return;
    }
    /* 生成全局唯一的 client_id：优先用浏览器自带的 UUID，没有就用时间戳拼随机数 */
    const clientId = (crypto.randomUUID)
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}`;

    /* 登记回执等待者：6 秒没等到 message_ack 就当失败 */
    const timer = setTimeout(() => {
      pendingAcks.delete(clientId);
      reject(new Error('发送超时，请检查网络'));
    }, 6000);
    pendingAcks.set(clientId, { resolve, timer });

    ws.send(JSON.stringify({
      type: 'send_message',
      order_id: orderId,       /* ⚠ 后端按 order_id 取值（下划线） */
      content,
      client_id: clientId,
    }));
  });
}

/* ==========================================================================
 * 对外功能 2：注册/注销“收到新消息”与“连接状态”监听
 * ========================================================================== */

/** 注册新消息监听，返回“取消监听”的函数（页面离开时调用，防止重复处理） */
export function onNewMessage(fn) {
  newMessageListeners.add(fn);
  return () => newMessageListeners.delete(fn);
}

/** 注册连接状态监听（'connected' | 'disconnected'），同样返回取消函数 */
export function onStatusChange(fn) {
  statusListeners.add(fn);
  return () => statusListeners.delete(fn);
}

function notifyStatus(status) {
  statusListeners.forEach(fn => {
    try { fn(status); } catch (e) { console.error(e); }
  });
}

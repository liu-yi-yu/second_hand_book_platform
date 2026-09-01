package org.tlais.yutest1.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tlais.yutest1.context.SpringContextHolder;
import org.tlais.yutest1.domain.dto.MessageCreateDTO;
import org.tlais.yutest1.domain.entity.Message;
import org.tlais.yutest1.domain.entity.User;
import org.tlais.yutest1.mapper.UserMapper;
import org.tlais.yutest1.properties.JwtProperties;
import org.tlais.yutest1.service.MessagesService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 服务端（第4周 4.1 + 4.2）
 *
 * 小白理解整条链路：
 * 1. 前端通过 ws://localhost:8080/ws/chat?token=xxx 建立连接
 * 2. onOpen：校验 token，把「用户ID -> 连接集合（多标签页共存）」存进 sessionMap
 * 3. 前端发 {type:"send_message", order_id, content, client_id}
 * 4. onMessage：交给 MessagesService 落库，然后给发送方回 message_ack、给接收方所有在线连接推 new_message
 */
@Component
@ServerEndpoint("/ws/chat")
@Slf4j
public class ChatWebSocketServer {

    // 保存「用户ID -> 该用户的所有 WebSocket 连接」的映射
    // 用 Set 而不是单 Session：同一用户可能开多个标签页/多端登录，
    // 每个连接都要能收到推送（否则后连的会把先连的顶掉）。
    // 用 ConcurrentHashMap 是因为可能有多个用户同时连接/断开（线程安全）
    private static final Map<String, Set<Session>> sessionMap = new ConcurrentHashMap<>();

    // 服务端空闲超时：不显式设置时内嵌 Tomcat 默认 60s 就会把没活动的连接踢掉。
    // 前端每 30s 发一次心跳，这里放宽到 120s，避免后台标签页定时器被节流时误踢线
    private static final long MAX_IDLE_TIMEOUT_MS = 120_000;

    // Jackson 工具，用来把 JSON 字符串 <-> Java 对象 互相转换
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立时触发
     */
    @OnOpen
    public void onOpen(Session session) {
        try {
            // 1. 从连接地址里取出 token，例如 ws://localhost:8080/ws/chat?token=xxx
            String token = getQueryParam(session, "token");

            // 2. 校验 token 并拿到用户ID
            JwtProperties jwtProperties = SpringContextHolder.getBean(JwtProperties.class);
            if (token == null || !jwtProperties.validateToken(token)) {
                // token 不合法，直接断开连接
                session.close();
                return;
            }
            String userId = jwtProperties.getUserIdByToken(token);

            // 3. 把「用户ID -> 连接集合」存起来：同一用户多个标签页共存，互不顶替
            sessionMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
            // 把用户ID也存到 session 自己的属性里，这样 onMessage 时能知道这条消息是谁发的
            session.getUserProperties().put("userId", userId);

            // 4. 放宽空闲超时（默认 60s 会误踢），前端 30s 心跳，120s 留足余量
            session.setMaxIdleTimeout(MAX_IDLE_TIMEOUT_MS);

            // 5. 告诉前端「连接成功」
            sendText(session, "{\"type\":\"connected\"}");
            log.info("用户 {} 已连接，当前在线连接数：{}", userId, sessionMap.size());
        } catch (Exception e) {
            log.error("WebSocket 连接建立失败", e);
        }
    }

    /**
     * 收到前端消息时触发
     */
    @OnMessage
    public void onMessage(String messageText, Session session) {
        try {
            // 把前端发来的 JSON 字符串解析成 JsonNode（类似 Map，可以按字段取值）
            JsonNode node = objectMapper.readTree(messageText);
            String type = node.path("type").asText();

            // 心跳：前端发 ping，服务端回 pong，用来保持连接不断开
            if ("ping".equals(type)) {
                sendText(session, "{\"type\":\"pong\"}");
                return;
            }

            // 发送消息
            if ("send_message".equals(type)) {
                handleSendMessage(node, session);
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败", e);
        }
    }

    /**
     * 处理「发送消息」这个动作
     */
    private void handleSendMessage(JsonNode node, Session session) throws Exception {
        // 1. 从前端发来的数据里取出各个字段
        Integer orderId = node.path("order_id").asInt();
        String content = node.path("content").asText();
        String clientId = node.path("client_id").asText();
        // 从 session 里取出当前登录用户ID（onOpen 时存进去的）
        String senderId = (String) session.getUserProperties().get("userId");

        // 2. 交给 service 落库（service 内部会做权限校验、去重）
        MessagesService messagesService = SpringContextHolder.getBean(MessagesService.class);
        MessageCreateDTO dto = new MessageCreateDTO(orderId, content, clientId);
        Message message = messagesService.sendMessage(senderId, dto);

        // 如果返回 null，说明校验没通过，直接忽略
        if (message == null) {
            return;
        }

        // 3. 给发送方回一个确认（message_ack），告诉它消息已保存，并返回服务端生成的 id
        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("type", "message_ack");
        ack.put("client_id", message.getClientId());
        ack.put("message_id", message.getId());
        ack.put("created_at", message.getCreatedAt().toString());
        sendText(session, objectMapper.writeValueAsString(ack));

        // 4. 如果接收方在线，把新消息推送给对方的所有连接（new_message）
        //    多标签页时每个连接都要收到，这样任意一个标签页都能实时弹出
        Set<Session> receiverSessions = sessionMap.get(message.getReceiverId());
        if (receiverSessions != null) {
            Map<String, Object> msgBody = new LinkedHashMap<>();
            msgBody.put("id", message.getId());
            msgBody.put("order_id", message.getOrderId());
            msgBody.put("sender_id", message.getSenderId());
            msgBody.put("content", message.getContent());
            msgBody.put("created_at", message.getCreatedAt().toString());

            // 查出发送者的用户名和头像，一起推给接收方（前端要用）
            UserMapper userMapper = SpringContextHolder.getBean(UserMapper.class);
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                msgBody.put("sender_name", sender.getUsername());
                msgBody.put("sender_avatar", sender.getAvatarUrl());
            }

            Map<String, Object> newMsg = new LinkedHashMap<>();
            newMsg.put("type", "new_message");
            newMsg.put("message", msgBody);
            String payload = objectMapper.writeValueAsString(newMsg);

            for (Session receiverSession : receiverSessions) {
                if (receiverSession.isOpen()) {
                    sendText(receiverSession, payload);
                }
            }
        }
    }

    /**
     * 连接断开时触发：只移除当前这个连接，不影响该用户的其他标签页
     */
    @OnClose
    public void onClose(Session session) {
        String userId = (String) session.getUserProperties().get("userId");
        if (userId != null) {
            Set<Session> sessions = sessionMap.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                // 该用户所有连接都断了，才把整个键删掉
                if (sessions.isEmpty()) {
                    sessionMap.remove(userId);
                }
                log.info("用户 {} 有连接断开，剩余连接数：{}", userId, sessions.size());
            }
        }
    }

    /**
     * 出错时触发
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 发生错误", error);
    }

    /**
     * 工具方法：从连接 URL 里取指定参数
     * 例如 ws://localhost:8080/ws/chat?token=abc123，key 传 "token" 就返回 "abc123"
     */
    private String getQueryParam(Session session, String key) {
        String query = session.getQueryString();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                // URL 里可能包含特殊字符（如 %2B），需要解码
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * 工具方法：向指定连接发送一段文本
     */
    private void sendText(Session session, String text) {
        try {
            session.getBasicRemote().sendText(text);
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }
}

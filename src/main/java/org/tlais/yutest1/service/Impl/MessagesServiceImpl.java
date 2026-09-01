package org.tlais.yutest1.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tlais.yutest1.constant.*;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.dto.MessageCreateDTO;
import org.tlais.yutest1.domain.entity.*;
import org.tlais.yutest1.domain.vo.MessageVO;
import org.tlais.yutest1.domain.vo.UnreadMessagesVO;
import org.tlais.yutest1.mapper.*;
import org.tlais.yutest1.service.MessagesService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class MessagesServiceImpl implements MessagesService {
    @Autowired
    private MessagesMapper messagesMapper;
    @Autowired
    private OrdersMapper  ordersMapper;
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private NotificationsMapper notificationsMapper;

    @Override
    public MessageVO getMessages(Integer orderId, Integer page, Integer limit) {
        Order order = ordersMapper.selectById(orderId);
        if(order == null){
            throw new IllegalArgumentException(OrderException.ORDER_NOT_EXIST);
        }
        if(!order.getSellerId().equals(BaseContext.getCurrentId())&&!order.getBuyerId().equals(BaseContext.getCurrentId())) {
            throw new IllegalArgumentException(OrderException.USER_NOT_SLLER_OR_BUYER);
        }

        PageHelper.startPage(page, limit, SortBy.CREATE_TIME_DESC);
        List<Message> messages = messagesMapper.selectByOrderId(orderId);
        PageInfo<Message> pageInfo = new PageInfo<>(messages);

        return MessageVO.builder()
                .message(messages)
                .hasMore(pageInfo.getPageNum() < pageInfo.getPages())
                .build();
    }

    @Override
    public UnreadMessagesVO getUnreadMessagesCount() {
        List<ByOrder> byOrders = new ArrayList<>();
        AtomicReference<Integer> count= new AtomicReference<>(0);
        String currentId = BaseContext.getCurrentId();
        List<Message> messages = messagesMapper.selectUnread(currentId);
        if(messages.isEmpty()){
            return new UnreadMessagesVO(0,new ArrayList<>());
        }
        HashMap<Integer,Long> map = new HashMap<>();
        HashMap<Integer,String> mapMessage = new HashMap<>();
        HashSet<Integer> orderIds = new HashSet<>();
        messages.forEach(message -> {
            if(map.containsKey(message.getOrderId())){
                map.put(message.getOrderId(),map.get(message.getOrderId())+1L);
            }
            else{
                map.put(message.getOrderId(),1L);
                String content = message.getContent();
                if(content.length()>50) {
                    content = content.substring(0,50);
                }
                mapMessage.put(message.getOrderId(), content);
            }
            count.getAndSet(count.get() + 1);
            orderIds.add(message.getOrderId());
        });
        List<Order> orders = ordersMapper.selectByIdList(orderIds);
        orders.forEach(order -> {
            ByOrder byOrder = new ByOrder();
            String userId;
            byOrder.setOrderId(order.getId());
            if (!order.getBuyerId().equals(currentId)) {
                userId = order.getBuyerId();
            }
            else {
                userId = order.getSellerId();
            }

            Book book = bookMapper.selectById(order.getBookId());
            byOrder.setTitle(book.getTitle());

            User user = userMapper.selectById(userId);
            byOrder.setUserName(user.getUsername());

            int mapCount = map.get(order.getId()).intValue();
            byOrder.setCount(mapCount);
            byOrder.setContent(mapMessage.get(order.getId()));
            byOrders.add(byOrder);
        });
        return new UnreadMessagesVO(count.get(),byOrders);
    }

    @Override
    public void readMessages(Integer orderId) {
        String currentId = BaseContext.getCurrentId();
        Order order = ordersMapper.selectById(orderId);
        if(order==null){
            throw new IllegalArgumentException(OrderException.ORDER_NOT_EXIST);
        }
        if(!order.getBuyerId().equals(currentId)&&!order.getSellerId().equals(currentId)){
            throw new IllegalArgumentException(OrderException.USER_NOT_SLLER_OR_BUYER);
        }

        messagesMapper.updateRead(orderId, currentId);
    }

    /**
     * 发送一条聊天消息（由 WebSocket 端点调用，不走 HTTP 接口）
     *
     * 整体流程：校验权限 → 去重 → 落库 → 重新查询拿到自增 id
     *
     * @param senderId 发送者用户ID（WebSocket 连接时从 token 解析得到）
     * @param dto      消息内容（orderId + content + clientId）
     * @return 落库后的完整消息对象（含数据库自增的 id）
     */
    @Override
    @Transactional
    public Message sendMessage(String senderId, MessageCreateDTO dto) {
        // 1. 查出订单，判断订单是否存在
        Order order = ordersMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException(OrderException.ORDER_NOT_EXIST);
        }

        // 2. 校验发送者必须是这个订单的买家或卖家（防止无关的人乱发消息）
        String buyerId = order.getBuyerId();
        String sellerId = order.getSellerId();
        if (!senderId.equals(buyerId) && !senderId.equals(sellerId)) {
            throw new IllegalArgumentException(OrderException.USER_NOT_SLLER_OR_BUYER);
        }

        // 3. 订单已完成或已取消后，不能再发消息（房间只读）
        if (OrderStatu.COMPLETED.equals(order.getStatus()) || OrderStatu.CANCELLED.equals(order.getStatus())) {
            throw new IllegalArgumentException("订单已完成或已取消，不能发送消息");
        }

        // 4. 去重：同一个 clientId 只允许落库一次（网络重试会产生重复消息）
        if (messagesMapper.countByClientId(dto.getClientId()) > 0) {
            log.warn("消息重复，直接返回已存在的消息。clientId={}", dto.getClientId());
            return messagesMapper.selectByClientId(dto.getClientId());
        }

        // 5. 确定接收者：我是买家就发给卖家，反之亦然
        String receiverId = senderId.equals(buyerId) ? sellerId : buyerId;

        LocalDateTime now = LocalDateTime.now();
        //去重非原子：countByClientId 和 insert 之间并发重复会抛 DuplicateKeyException，
        // 被 onMessage 的 catch 吞掉 → 发送方拿不到 message_ack
        // 。落库不重复（有唯一索引兜底），但客户端收不到确认。
        // 5. 组装消息并落库 —— 真正并发时靠唯一索引兜底，捕获重复
        Message message = Message.builder()
                .orderId(dto.getOrderId())
                .senderId(senderId)
                .receiverId(receiverId)
                .content(dto.getContent())
                .clientId(dto.getClientId())
                .createdAt(now)
                .build();

        try {
            messagesMapper.insert(message);
        } catch (DuplicateKeyException e) {
            // 并发下同一 clientId 已被插入，直接返回已存在的那条（不重复通知）
            log.warn("消息重复（并发去重），clientId={}", dto.getClientId());
            return messagesMapper.selectByClientId(dto.getClientId());
        }

// 6. 只有「真正新落库」的消息才发通知（重复消息不会走到这里）
        LocalDateTime maxTime = notificationsMapper.getMaxTime(order.getId(), receiverId);
        if (maxTime == null || maxTime.isBefore(now.minusMinutes(5))) {
            Notification notification = Notification.builder()
                    .relatedOrderId(order.getId())
                    .userId(receiverId)
                    .type(NotificationsType.NEW_MESSAGE)
                    .title(NotificationsContent.NEW_MESSAGE)
                    .createdAt(now)
                    .build();
            notificationsMapper.insert(notification);
        }


        // 7. 重新查询一次，拿到数据库自增生成的 id（Message.id 是 String，直接自增回填可能类型不匹配，所以用查询拿）
        Message saved = messagesMapper.selectByClientId(dto.getClientId());
        log.info("消息发送成功，messageId={}, orderId={}", saved != null ? saved.getId() : null, dto.getOrderId());
        return saved;
    }
}

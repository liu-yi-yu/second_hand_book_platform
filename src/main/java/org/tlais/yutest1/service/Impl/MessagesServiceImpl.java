package org.tlais.yutest1.service.Impl;

import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.tlais.yutest1.constant.SortBy;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.entity.*;
import org.tlais.yutest1.domain.vo.MessageVO;
import org.tlais.yutest1.mapper.BookMapper;
import org.tlais.yutest1.mapper.MessagesMapper;
import org.tlais.yutest1.mapper.OrdersMapper;
import org.tlais.yutest1.mapper.UserMapper;
import org.tlais.yutest1.service.MessagesService;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Service
public class MessagesServiceImpl implements MessagesService {
    @Autowired
    private MessagesMapper messagesMapper;
    @Autowired
    private OrdersMapper  ordersMapper;
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public MessageVO getMessages(Integer orderId, Integer limit) {
        int max = messagesMapper.fillMax();
        PageHelper.startPage(max, limit, SortBy.CREATE_TIME_DESC);
        List<Message> messages = messagesMapper.selectByOrderId(orderId);

        return MessageVO.builder()
                .message(messages)
                .hasMore(messages.size() >= limit)
                .build();
    }

    @Override
    public List<ByOrder> getUnreadMessagesCount() {
        List<ByOrder> byOrders = new ArrayList<>();
        String currentId = BaseContext.getCurrentId();
        List<Message> messages = messagesMapper.selectUnread(currentId);
        HashMap<Integer,Long> map = new HashMap<>();
        HashMap<Integer,String> mapMessage = new HashMap<>();
        HashSet<Integer> orderIds = new HashSet<>();
        messages.forEach(message -> {
            if(map.containsKey(message.getOrderId())){
                map.put(message.getOrderId(),map.get(message.getOrderId())+1L);
            }
            else{
                map.put(message.getOrderId(),1L);
                mapMessage.put(message.getOrderId(),message.getContent().substring(0,50));
            }
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

            byOrder.setCount(map.get(order.getId()).intValue());
            byOrder.setContent(mapMessage.get(order.getId()));
            byOrders.add(byOrder);
        });
        return byOrders;
    }
}

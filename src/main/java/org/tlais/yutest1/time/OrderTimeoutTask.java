package org.tlais.yutest1.time;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.tlais.yutest1.constant.BookStatu;
import org.tlais.yutest1.constant.NotificationsContent;
import org.tlais.yutest1.constant.NotificationsType;
import org.tlais.yutest1.constant.OrderStatu;
import org.tlais.yutest1.context.BookViewCounter;
import org.tlais.yutest1.domain.entity.*;
import org.tlais.yutest1.mapper.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OrderTimeoutTask {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private BookMapper  bookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private NotificationsMapper notificationsMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private BookViewCounter bookViewCounter;


    //每过6小时检查一次超时订单
    @Scheduled(cron = "0 0 0/6 * * ?")
    @Transactional
    public void checkOrderTimeout() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> cancelOrders = new ArrayList<>();
        List<Notification> notifications = new ArrayList<>();

        List<Order> p1 = ordersMapper.checkOrderPendingTimeout(OrderStatu.PENDING, now.minusHours(48));
        List<Order> p2 = ordersMapper.checkOrderConfirmedTimeout(OrderStatu.CONFIRMED, now.minusHours(72));
        List<Order> p3 = ordersMapper.checkOrderReceivedTimeout(OrderStatu.RECEIVED, now.minusHours(24));
        cancelOrders.addAll(p1);
        cancelOrders.addAll(p2);

        if(!p3.isEmpty()){
            ordersMapper.cancelOrder(p3,OrderStatu.COMPLETED,now);
            userMapper.addCredit(p3,+2,now);
            for(Order order:p3){
                Notification notification = new Notification();
                notification.setUserId(order.getSellerId());

                createNotification(notification,NotificationsType.ORDER_COMPLETED,NotificationsContent.ORDER_COMPLETED
                        ,NotificationsContent.ORDER_COMPLETED,order,now,notifications);
            }
        }
        if(cancelOrders.isEmpty()){
            return;
        }
        //TODO:取消之后需要退款

        // 取消超时订单
        ordersMapper.cancelOrder(cancelOrders,OrderStatu.CANCELLED,now);
        // 处理
        if(!p1.isEmpty()){
            userMapper.minusCredit(p1,-5,now);
            bookMapper.updateStatusByBookIds(p1, BookStatu.SELLING,now);
        }
        if(!p2.isEmpty()){
            userMapper.minusCredit(p2,-10,now);
            bookMapper.updateStatusByBookIds(p2, BookStatu.SELLING,now);
        }

        for(Order order:cancelOrders){
            Notification notification = new Notification();
            notification.setUserId(order.getSellerId());

            createNotification(notification,NotificationsType.ORDER_CANCELLED,NotificationsContent.ORDER_CANCELLED
                    ,NotificationsContent.ORDER_CANCELLED,order,now,notifications);
        }
        notificationsMapper.insertBatch(notifications);
    }

    private static void createNotification(Notification notification,String type,String title,String content,Order order,LocalDateTime now,List<Notification> notifications) {
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedOrderId(order.getId());
        notification.setCreatedAt(now);
        //卖家通知
        notifications.add(notification);
        //买家通知
        Notification buyer = new Notification();
        BeanUtils.copyProperties(notification,buyer);
        buyer.setUserId(order.getBuyerId());
        notifications.add(buyer);
    }

    //每周一凌晨0点检查一次取消的订单
    @Scheduled(cron = "0 0 0 ? * MON")
    @Transactional
    public void checkCancelOrder() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = new ArrayList<>();
        List<OrderCountByUserId> cancelOrders = ordersMapper.getOrderCountByUserId(now.minusDays(7));
        List<UserCredit> userCredits = new ArrayList<>();
        if(cancelOrders.isEmpty()){
            return;
        }
        for(OrderCountByUserId orderCountByUserId : cancelOrders){
            Integer orderCount = orderCountByUserId.getOrderCount();
            if(orderCount <= 3){
                Order order = new Order();
                //实际应为买家ID，但为了方便（与minusCredit方法参数一致），这里用卖家ID代替买家ID
                order.setSellerId(orderCountByUserId.getUserId());
                orders.add(order);
            }
            else{
                UserCredit userCredit = new UserCredit();
                userCredit.setUserId(orderCountByUserId.getUserId());
                userCredit.setCredit(-orderCount * 3 - 1);
                userCredits.add(userCredit);
            }
        }
        userMapper.minusCredit(orders,-10,now);
        userMapper.decCredit(userCredits,now);
    }

    //每天
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkOrderConfirmed() {
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateByCredit(0,now);

        Map<String, Long> drain = bookViewCounter.drain();
        if(!drain.isEmpty()){
            for(Map.Entry<String, Long> entry : drain.entrySet()){
                bookMapper.addViewCount(entry.getKey(), entry.getValue(), now);
            }
        }
    }
}

package org.tlais.yutest1.service.Impl;

import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tlais.yutest1.constant.*;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.dto.OrderCreatesDTO;
import org.tlais.yutest1.domain.dto.OrderSearchDTO;
import org.tlais.yutest1.domain.entity.*;
import org.tlais.yutest1.domain.vo.*;
import org.tlais.yutest1.mapper.*;
import org.tlais.yutest1.service.OrdersService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
public class OrdersServiceImpl implements OrdersService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private BookImageMapper bookImageMapper;
    @Autowired
    private ImageMapper imageMapper;
    @Autowired
    private NotificationsMapper notificationsMapper;


    @Override
    @Transactional
    public List<OrderVO> createOrder(OrderCreatesDTO orderCreatesDTO) {
        // 一本书同时被多人下单：使用**乐观锁**策略——下单时检查书籍状态和版本号，防止超卖。
        ArrayList<String> bookIds = orderCreatesDTO.getBookIds();
        List<Book> books = bookMapper.selectByIds(bookIds);
        if (books == null || books.isEmpty()) {
            log.error(OrderException.BOOK_NOT_EXIST);
            return null;
        }

        List<OrderVO> orders = new ArrayList<>();
        List<Order> orderList = new ArrayList<>();
        List<Notification> notifications = new ArrayList<>();
        UserSimpleVO buyer = new UserSimpleVO();
        String currentId = BaseContext.getCurrentId();
        user(currentId, buyer);
        LocalDateTime now = LocalDateTime.now();

        Iterator<Book> it = books.iterator();
        while (it.hasNext()) {
            Book book = it.next();
            // 禁止自买：买家不能购买自己发布的书籍。
            // 否则订单 buyerId == sellerId，聊天时 receiver_id == sender_id（自己发给自己），
            // 未读数会把"自己发的"算进去。此校验与 upStatus 里的 SELLER_AND_BUYER_SAME 保持一致
            if (book.getSellerId().equals(currentId)) {
                throw new IllegalArgumentException(OrderException.SELLER_AND_BUYER_SAME);
            }
            if (!book.getStatus().equals(BookStatu.SELLING)) {
                bookIds.remove(book.getId());
                it.remove();          // 安全删除
                continue;
            }
            book.setUpdatedAt(now);
            OrderVO orderVO = new OrderVO();
            Order order = new Order();
            BookSimpleVO bookSimpleVO = new BookSimpleVO();
            UserSimpleVO seller = new UserSimpleVO();

            user(book.getSellerId(), seller);
            BeanUtils.copyProperties(book, bookSimpleVO);

            orderVO.setBook(bookSimpleVO);
            orderVO.setSeller(seller);
            orderVO.setBuyer(buyer);
            orderVO.setAmount(book.getSellingPrice());
            orderVO.setStatus(OrderStatu.PENDING);
            orderVO.setCreatedAt(now.toString());

            BeanUtils.copyProperties(orderVO, order);
            order.setBookId(book.getId());
            order.setSellerId(book.getSellerId());
            order.setBuyerId(currentId);
            order.setCreatedAt(now);
            order.setUpdatedAt(now);

            log.info("创建订单成功，订单ID：{}", order.toString());

            orderList.add(order);
            orders.add(orderVO);

            // 插入通知,给卖家
            Notification notification = new Notification();
            createNotification(order.getId(), notification, book.getSellerId(),NotificationsType.ORDER_CREATED,NotificationsContent.ORDER_CREATED,NotificationsContent.ORDER_CREATED);
            notifications.add(notification);
        }
        // 更新书籍状态为已售
        Integer rows = bookMapper.updateByIds(books, now);
        if(rows!=books.size()){
            log.error(OrderException.BOOK_VERSION_ERROR);
            return null;
        }
        // 插入订单
        ordersMapper.insertBatch(orderList);
        // 插入通知,给卖家
        notificationsMapper.insertBatch(notifications);
        // 清空购物车
        cartMapper.deleteBatch(bookIds);
        return orders;
    }

    @Override
    @Transactional
    public OrderVO upStatus(Integer orderId,String orderStatus,String reason) {
        Order order = ordersMapper.selectById(orderId);
        if (order == null ) {
            log.error(OrderException.ORDER_NOT_EXIST);
            return null;
        }
        String sellerId = order.getSellerId();
        String buyerId = order.getBuyerId();
        String currentId = BaseContext.getCurrentId();
        String status = order.getStatus();

        if(!buyerId.equals(currentId)&&!sellerId.equals(currentId)){
            log.error(OrderException.USER_NOT_SLLER_OR_BUYER);
            return null;
        }

        if(sellerId.equals(buyerId)){
            log.error(OrderException.SELLER_AND_BUYER_SAME);
            return null;
        }

        Notification notification = new Notification();
        switch (orderStatus) {
            // 确认订单
            case OrderStatu.CONFIRMED ->{
                //  卖家如果在 48 小时内未确认，订单自动取消，书籍恢复 `selling` 状态
                if(!sellerId.equals(currentId)){
                    log.error(OrderException.SELLER_DIFFERENT);
                    return null;
                }
                if(!status.equals(OrderStatu.PENDING)){
                    log.error(OrderException.ORDER_STATUS_EXCEPTION);
                    return null;
                }
                order.setConfirmedAt(LocalDateTime.now());
                // 插入通知,给买家
                createNotification(orderId, notification, buyerId,NotificationsType.ORDER_CONFIRMED,NotificationsContent.ORDER_CONFIRMED,NotificationsContent.ORDER_CONFIRMED);
            }
            // 发货
            case OrderStatu.SHIPPED ->{
                // 卖家如果在确认后 72 小时内未发货，订单自动取消
                if(!sellerId.equals(currentId)){
                    log.error(OrderException.SELLER_DIFFERENT);
                    return null;
                }
                if(!status.equals(OrderStatu.CONFIRMED)){
                    log.error(OrderException.ORDER_STATUS_EXCEPTION);
                    return null;
                }
                order.setShippedAt(LocalDateTime.now());
                // 插入通知,给买家
                createNotification(orderId, notification, buyerId,NotificationsType.ORDER_SHIPPED,NotificationsContent.ORDER_SHIPPED,NotificationsContent.ORDER_SHIPPED);
            }
            // 收货
            case OrderStatu.RECEIVED ->{
                //  收货后 24 小时自动变为 `completed`（给买家留检查书籍的时间）
                if(!buyerId.equals(currentId)){
                    log.error(OrderException.BUYER_DIFFERENT);
                    return null;
                }
                if(!status.equals(OrderStatu.SHIPPED)){
                    log.error(OrderException.ORDER_STATUS_EXCEPTION);
                    return null;
                }
                order.setReceivedAt(LocalDateTime.now());
                // 插入通知,给买家
                createNotification(orderId, notification, buyerId,NotificationsType.ORDER_RECEIVED,NotificationsContent.ORDER_RECEIVED,NotificationsContent.ORDER_RECEIVED);
                // 插入通知,给卖家
                createNotification(orderId, notification, sellerId,NotificationsType.ORDER_RECEIVED,NotificationsContent.ORDER_RECEIVED,NotificationsContent.ORDER_RECEIVED);
            }
            // 取消订单
            case OrderStatu.CANCELLED ->{
                //  频繁取消的用户将扣除信誉分（见 3.10）
                //TODO 取消后需要退钱
                if(!status.equals(OrderStatu.PENDING) &&!(status.equals(OrderStatu.COMPLETED)&&buyerId.equals(currentId))){
                    log.error(OrderException.ORDER_STATUS_EXCEPTION);
                    return null;
                }
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelReason(reason);
                Book book = bookMapper.selectById(order.getBookId());
                book.setStatus(BookStatu.SELLING);
                bookMapper.updateById(book);
                // 插入通知,给买家
                createNotification(orderId, notification, buyerId,NotificationsType.ORDER_CANCELLED,NotificationsContent.ORDER_CANCELLED,NotificationsContent.ORDER_CANCELLED);
                // 插入通知,给卖家
                createNotification(orderId, notification, sellerId,NotificationsType.ORDER_CANCELLED,NotificationsContent.ORDER_CANCELLED,NotificationsContent.ORDER_CANCELLED);
            }
        }

        order.setStatus(orderStatus);
        ordersMapper.updateById(order);

        OrderVO orderVO = new OrderVO();
        if (orderVOGet(order, orderVO)) return null;

        return orderVO;

    }

    private void createNotification(Integer orderId, Notification notification, String buyerId,String type,String title,String content) {
        notification.setUserId(buyerId);
        notification.setRelatedOrderId(orderId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCreatedAt(LocalDateTime.now());
        notificationsMapper.insert(notification);
    }

    private boolean orderVOGet(Order order, OrderVO orderVO) {
        BeanUtils.copyProperties(order, orderVO);
        UserSimpleVO buyer = new UserSimpleVO();
        UserSimpleVO seller = new UserSimpleVO();
        BookSimpleVO bookSimpleVO = new BookSimpleVO();

        User user = userMapper.selectById(order.getBuyerId());
        if(user==null){
            log.error(OrderException.USER_NOT_EXIST);
            return true;
        }
        BeanUtils.copyProperties(user, buyer);
        user = userMapper.selectById(order.getSellerId());
        if(user==null){
            log.error(OrderException.USER_NOT_EXIST);
            return true;
        }
        BeanUtils.copyProperties(user, seller);
        Book book = bookMapper.selectById(order.getBookId());
        if(book==null){
            log.error(OrderException.BOOK_NOT_EXIST);
            return true;
        }
        BeanUtils.copyProperties(book, bookSimpleVO);

        orderVO.setBuyer(buyer);
        orderVO.setSeller(seller);
        orderVO.setBook(bookSimpleVO);
        return false;
    }

    @Override
    public PageVO<OrderListVO> getAllOrders(OrderSearchDTO orderSearchDTO) {
        log.info("查询订单参数，角色：{}，状态：{}", orderSearchDTO.getRole(), orderSearchDTO.getStatus());
        String role = orderSearchDTO.getRole();
        List<OrderListVO> orderListVOs = new ArrayList<>();
        OrdersRole orderRole = new OrdersRole();
        boolean equals = role.equals(OrderRole.BUYER);
        if(equals){
            orderRole.setBuyerId(BaseContext.getCurrentId());
        }else{
            orderRole.setSellerId(BaseContext.getCurrentId());
        }
        log.info("查询订单参数，角色：{}，状态：{}", role, orderRole.getBuyerId());
        if(orderSearchDTO.getStatus()!=null&&!orderSearchDTO.getStatus().isEmpty()) {
            orderRole.setStatus(orderSearchDTO.getStatus().split(","));
        }

        PageHelper.startPage(orderSearchDTO.getPageNum(), orderSearchDTO.getPageSize(), SortBy.CREATE_TIME_DESC);
        List<Order> orders = ordersMapper.selectList(orderRole);

        log.info("查询订单成功，订单数量：{}", orders.size());
        log.info("查询订单成功：{}", orders.toString());

        if(orders.isEmpty()){
            log.error(OrderException.ORDER_NOT_EXIST);
            return null;
        }

        for (Order order : orders) {
            OrderListVO orderListVO = new OrderListVO();
            User counterparty ;
            BeanUtils.copyProperties(order, orderListVO);

            log.info("查询订单成功，订单：{}", order.toString());
            String bookId = order.getBookId();
            log.info("查询订单成功，订单ID：{}，书ID：{}", order.getId(), bookId);
            Book book = bookMapper.selectById(bookId);
            List<String> imageIds = bookImageMapper.selectList(bookId);
            if(!imageIds.isEmpty()){
                List<ImageVO> imageVOOS = imageMapper.getByIds(imageIds);
                if(!imageVOOS.isEmpty()){
                    orderListVO.setBookCoverImage(imageVOOS.get(0).getUrl());
                }
            }
            orderListVO.setBookId(bookId);
            orderListVO.setBookTitle(book.getTitle());


            if(equals){
                counterparty = userMapper.selectById(order.getSellerId());
            }
            else {
                counterparty = userMapper.selectById(order.getBuyerId());
            }
            log.info("查询订单成功，订单ID：{}，对方ID：{}", order.getId(), counterparty.getId());
            orderListVO.setCounterpartyName(counterparty.getUsername());
            orderListVO.setCounterpartyAvatar(counterparty.getAvatarUrl());

            orderListVOs.add(orderListVO);
        }

        return new PageVO<OrderListVO>(orderListVOs, (long) orders.size());
    }

    @Override
    public OrderVO getOrderDetail(Integer orderId) {
        Order order = ordersMapper.selectById(orderId);
        if(order==null){
            log.error(OrderException.ORDER_NOT_EXIST);
            return null;
        }
        String currentId = BaseContext.getCurrentId();
        if(order.getSellerId().equals(currentId)||order.getBuyerId().equals(currentId)){
            OrderVO orderVO = new OrderVO();
            if(orderVOGet(order, orderVO)){
                return null;
            }
            return orderVO;
        }

        log.error(OrderException.USER_NOT_SLLER_OR_BUYER);
        return null;

    }


    private void user(String currentId, UserSimpleVO buyer) {
        User user = userMapper.selectById(currentId);
        BeanUtils.copyProperties(user, buyer);
    }
}

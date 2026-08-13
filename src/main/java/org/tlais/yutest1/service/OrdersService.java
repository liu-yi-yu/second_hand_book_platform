package org.tlais.yutest1.service;


import org.tlais.yutest1.domain.dto.OrderCreatesDTO;
import org.tlais.yutest1.domain.dto.OrderSearchDTO;
import org.tlais.yutest1.domain.vo.OrderListVO;
import org.tlais.yutest1.domain.vo.OrderVO;
import org.tlais.yutest1.domain.vo.PageVO;

import java.util.ArrayList;
import java.util.List;

public interface OrdersService {
    List<OrderVO> createOrder(OrderCreatesDTO orderCreatesDTO);

    OrderVO upStatus(Integer orderId,String orderStatus,String reason);

    PageVO<OrderListVO> getAllOrders(OrderSearchDTO orderSearchDTO);

    OrderVO getOrderDetail(Integer orderId);
}

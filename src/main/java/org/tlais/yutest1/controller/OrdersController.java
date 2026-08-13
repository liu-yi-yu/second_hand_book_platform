package org.tlais.yutest1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.tlais.yutest1.constant.OrderStatu;
import org.tlais.yutest1.domain.dto.OrderCreateDTO;
import org.tlais.yutest1.domain.dto.OrderCreatesDTO;
import org.tlais.yutest1.domain.dto.OrderSearchDTO;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.service.OrdersService;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrdersController {
    @Autowired
    private OrdersService ordersService;

    @PostMapping()
    @Operation(summary = "买家创建订单")
    public Result createOrder(@RequestBody OrderCreatesDTO orderCreatesDTO) {
        return Result.success(ordersService.createOrder(orderCreatesDTO));
    }

    @PutMapping("/{order_id}/confirm")
    @Operation(summary = "卖家确认订单")
    public Result confirmOrder(@PathVariable("order_id") Integer order_id) {
        return Result.success(ordersService.upStatus(order_id, OrderStatu.CONFIRMED,null));
    }

    @PutMapping("/{order_id}/ship")
    @Operation(summary = "卖家发货")
    public Result shipOrder(@PathVariable("order_id") Integer order_id) {
        return Result.success(ordersService.upStatus(order_id,OrderStatu.SHIPPED,null));
    }

    @PutMapping("/{order_id}/receive")
    @Operation(summary = "买家收货")
    public Result receiveOrder(@PathVariable("order_id") Integer order_id) {
        return Result.success(ordersService.upStatus(order_id, OrderStatu.RECEIVED,null));
    }

    @PutMapping("/{order_id}/cancel")
    @Operation(summary = "买家取消订单")
    public Result cancelOrder(@PathVariable("order_id") Integer order_id,@RequestBody String reason) {
        return Result.success(ordersService.upStatus(order_id, OrderStatu.CANCELLED, reason));
    }

    @GetMapping()
    @Operation(summary = "查询用户订单列表")
    public Result getAllOrders(OrderSearchDTO orderSearchDTO) {
        log.info("查询订单参数，角色：{}，状态：{}，页码：{}，每页数量：{}", orderSearchDTO.getRole(), orderSearchDTO.getStatus(), orderSearchDTO.getPageNum(), orderSearchDTO.getPageSize());
        return Result.success(ordersService.getAllOrders(orderSearchDTO));
    }

    @GetMapping("/{order_id}")
    @Operation(summary = "查询订单详情")
    public Result getOrderDetail(@PathVariable("order_id") Integer order_id) {
        return Result.success(ordersService.getOrderDetail(order_id));
    }

}

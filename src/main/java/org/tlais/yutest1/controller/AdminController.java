package org.tlais.yutest1.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.ibatis.annotations.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.tlais.yutest1.domain.dto.AdminDTO;
import org.tlais.yutest1.domain.dto.PageDTO;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    //TODO: 设置一个管理员判断接口，在登录时判断是否为管理员
    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    public Result getUsers(PageDTO pageDTO, AdminDTO admin) {
        return Result.success(adminService.getPage(pageDTO,admin));
    }

    @PutMapping("/users/{user_id}/status")
    @Operation(summary = "更新用户状态")
    public Result updateStatus(@PathVariable("user_id") String userId, @RequestBody AdminDTO adminDTO) {
        adminService.updateStatus(userId,adminDTO);
        return Result.success();
    }

    @GetMapping("/books")
    @Operation(summary = "获取所有图书列表")
    public Result getBooks() {
        return Result.success(adminService.getBooks());
    }

    @PutMapping("/books/{book_id}/remove")
    @Operation(summary = "删除图书")
    public Result removeBook(@PathVariable("book_id") String id) {
        //TODO: 假设后续前端有举报功能，管理员经审核后操作。
        adminService.removeBook(id);
        return Result.success();
    }

    @GetMapping("/orders")
    @Operation(summary = "获取所有订单列表")
    public Result getOrders() {
        return Result.success(adminService.getOrders());
    }

    @PutMapping("/orders/{order_id}/cancel")
    @Operation(summary = "强制取消订单")
    public Result cancelOrder(@PathVariable("order_id") String orderId,@RequestParam String reason) {
        adminService.cancelOrder(orderId,reason);
        return Result.success();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "简易数据看板")
    public Result getDashboard() {
        return Result.success(adminService.getDashboard());
    }

}

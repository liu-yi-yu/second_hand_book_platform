package org.tlais.yutest1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.service.MessagesService;

@RestController
@RequestMapping("/api")
public class MessagesController {
    @Autowired
    private MessagesService messagesService;

    @GetMapping("/orders/{order_id}/messages")
    public Result getMessages(@PathVariable("order_id") Integer order_id,@RequestParam(defaultValue = "1") Integer page,Integer limit) {
        if(limit == null||limit<=0||limit>100) limit = 50;
        if (page == null || page < 1) page = 1;
        return Result.success(messagesService.getMessages(order_id,page,limit));
    }

    @GetMapping("/messages/unread-count")
    public Result getUnreadMessagesCount(){
        //  当用户在某个订单聊天页面（或 WebSocket 连接中收到该订单的消息）时，自动标记该订单消息为已读。
        //- 提供单独的标记已读接口供前端调用：
        //
        //  ```
        //  PUT /api/orders/:order_id/messages/read
        return Result.success(messagesService.getUnreadMessagesCount());
    }

    @PutMapping("/orders/{order_id}/messages/read")
    public Result readMessages(@PathVariable("order_id") Integer order_id){
        messagesService.readMessages(order_id);
        return Result.success();
    }
}

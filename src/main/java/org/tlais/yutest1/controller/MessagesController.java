package org.tlais.yutest1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.service.MessagesService;

@RestController
@RequestMapping("/api")
public class MessagesController {
    @Autowired
    private MessagesService messagesService;

    @GetMapping("/orders/{order_id}/messages")
    public Result getMessages(@PathVariable("order_id") Integer order_id,Integer limit) {
        if(limit == null||limit<=0){
            limit = 50;
        }
        return Result.success(messagesService.getMessages(order_id,limit));
    }

    @GetMapping("/messages/unread-count")
    public Result getUnreadMessagesCount(){
        //TODO  当用户在某个订单聊天页面（或 WebSocket 连接中收到该订单的消息）时，自动标记该订单消息为已读。
        //- 提供单独的标记已读接口供前端调用：
        //
        //  ```
        //  PUT /api/orders/:order_id/messages/read
        return Result.success(messagesService.getUnreadMessagesCount());
    }
}

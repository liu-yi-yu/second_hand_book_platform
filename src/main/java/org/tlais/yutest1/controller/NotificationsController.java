package org.tlais.yutest1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.tlais.yutest1.domain.dto.NotificationsDTO;
import org.tlais.yutest1.domain.dto.NotificationsIdsDTO;
import org.tlais.yutest1.domain.dto.PageDTO;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.service.NotificationsService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationsController {
    @Autowired
    private NotificationsService notificationsService;

    @GetMapping()
    public Result getNotifications(NotificationsDTO notificationsDTO) {
        return Result.success(notificationsService.getNotifications(notificationsDTO));
    }

    @PutMapping("/read")
    public Result updateNotifications(@RequestBody NotificationsIdsDTO notificationsIdsDTO) {
        return Result.success(notificationsService.updateNotifications(notificationsIdsDTO.getIds()));
    }
}

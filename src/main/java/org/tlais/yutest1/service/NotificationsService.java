package org.tlais.yutest1.service;

import org.tlais.yutest1.domain.dto.NotificationsDTO;
import org.tlais.yutest1.domain.vo.NotificationVO;
import org.tlais.yutest1.domain.vo.PageNotificationVO;

public interface NotificationsService {
    PageNotificationVO<NotificationVO> getNotifications(NotificationsDTO notificationsDTO);

    Integer updateNotifications(Integer[] ids);

}

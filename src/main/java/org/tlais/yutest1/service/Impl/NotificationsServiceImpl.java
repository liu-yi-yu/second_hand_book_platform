package org.tlais.yutest1.service.Impl;

import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.dto.NotificationsDTO;
import org.tlais.yutest1.domain.vo.NotificationVO;
import org.tlais.yutest1.domain.vo.PageNotificationVO;
import org.tlais.yutest1.domain.vo.PageVO;
import org.tlais.yutest1.mapper.NotificationsMapper;
import org.tlais.yutest1.service.NotificationsService;

import java.util.List;

@Service
public class NotificationsServiceImpl implements NotificationsService {
    @Autowired
    private NotificationsMapper notificationsMapper;

    @Override
    public PageNotificationVO<NotificationVO> getNotifications(NotificationsDTO notificationsDTO) {
        Integer isRead;
        if(notificationsDTO.isUnreadOnly()){
            isRead=1;
        }else{
            isRead=0;
        }

        PageHelper.startPage(notificationsDTO.getPage(), notificationsDTO.getPageSize());
        List<NotificationVO> notificationVOS = notificationsMapper.fillNotifications(isRead, BaseContext.getCurrentId());

        PageVO<NotificationVO> notificationVOPageVO = new PageVO<>(notificationVOS, (long) notificationVOS.size());

        return new PageNotificationVO<NotificationVO>(notificationVOPageVO, notificationVOS.size());

    }

    @Override
    public Integer updateNotifications(Integer[] ids) {
        return notificationsMapper.updateNotifications(ids);
    }
}

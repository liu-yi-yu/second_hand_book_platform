package org.tlais.yutest1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tlais.yutest1.domain.vo.NotificationVO;

import java.util.List;

@Mapper
public interface NotificationsMapper {
    @Select("select * from notifications where is_read=#{isRead} and user_id=#{currentId}")
    List<NotificationVO> fillNotifications(Integer isRead, String currentId);

    Integer updateNotifications(Integer[] ids);
}

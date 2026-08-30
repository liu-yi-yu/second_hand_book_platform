package org.tlais.yutest1.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tlais.yutest1.domain.entity.Notification;
import org.tlais.yutest1.domain.vo.NotificationVO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationsMapper {
    @Select("select * from notifications where is_read=#{isRead} and user_id=#{currentId}")
    List<NotificationVO> fillNotifications(Integer isRead, String currentId);

    Integer updateNotifications(Integer[] ids);

    void insertBatch(List<Notification> notifications);

    @Insert("insert into notifications (user_id, type, related_order_id, content, created_at, is_read,title) values (#{userId}, #{type}, #{relatedOrderId}, #{content}, #{createdAt}, 0,#{title})")
    void insert(Notification notification);

    @Select("select max(created_at) from notifications where related_order_id=#{orderId} and user_id=#{userId} and type = 'new_message'")
    LocalDateTime getMaxTime(Integer orderId, String userId);
}

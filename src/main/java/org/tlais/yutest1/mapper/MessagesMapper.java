package org.tlais.yutest1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.tlais.yutest1.domain.entity.Message;
import org.tlais.yutest1.domain.entity.Order;

import java.util.List;

@Mapper
public interface MessagesMapper {
    @Select("select count(*) from messages where order_id = #{orderId}")
    int fillMax(Integer orderId);

    List<Message> selectByOrderId(Integer orderId);

    List<Message> selectUnread(String currentId);

    // 标记已读：只把「我是接收方」的消息标为已读，不能把对方该读的也一起标了
    @Update("update messages set is_read = 1 where order_id = #{orderId} and receiver_id = #{receiverId}")
    void updateRead(Integer orderId, String receiverId);

    // 新增：保存一条消息（对应 resources/mapper/MessagesMapper.xml 里的 insert）
    int insert(Message message);

    // 新增：按客户端ID查重，防止同一条消息重复落库
    @Select("select count(*) from messages where client_id = #{clientId}")
    int countByClientId(String clientId);

    // 新增：按客户端ID查询完整消息（插入后用它重新查一次，拿到数据库自增的 id）
    @Select("select * from messages where client_id = #{clientId}")
    Message selectByClientId(String clientId);
}

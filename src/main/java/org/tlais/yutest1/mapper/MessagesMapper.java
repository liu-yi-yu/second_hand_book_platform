package org.tlais.yutest1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.tlais.yutest1.domain.entity.Message;
import org.tlais.yutest1.domain.entity.Order;

import java.util.List;

@Mapper
public interface MessagesMapper {
    @Select("select count(*) from messages")
    int fillMax();

    List<Message> selectByOrderId(Integer orderId);

    List<Message> selectUnread(String currentId);

    @Update("update messages set is_read = 1 where order_id = #{orderId}")
    void updateRead(Integer orderId);
}

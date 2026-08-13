package org.tlais.yutest1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.tlais.yutest1.annotation.AutoFill;
import org.tlais.yutest1.domain.entity.Order;
import org.tlais.yutest1.domain.entity.OrdersRole;
import org.tlais.yutest1.domain.vo.OrderVO;
import org.tlais.yutest1.enumeration.OperationType;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface OrdersMapper {
    void insertBatch(@Param("orderList") List<Order> orderList);

    @AutoFill(OperationType.UPDATE)
    void updateById(Order order);

    @Select("select * from orders where id = #{orderId}")
    Order selectById(Integer orderId);

    List<Order> selectList(OrdersRole orderRole);
}

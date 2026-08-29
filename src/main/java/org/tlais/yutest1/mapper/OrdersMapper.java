package org.tlais.yutest1.mapper;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.tlais.yutest1.annotation.AutoFill;
import org.tlais.yutest1.domain.entity.Order;
import org.tlais.yutest1.domain.entity.OrderCountByStatus;
import org.tlais.yutest1.domain.entity.OrdersRole;
import org.tlais.yutest1.domain.vo.OrderListVO;
import org.tlais.yutest1.domain.vo.OrderVO;
import org.tlais.yutest1.enumeration.OperationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Mapper
public interface OrdersMapper {
    void insertBatch(@Param("orderList") List<Order> orderList);

    @AutoFill(OperationType.UPDATE)
    void updateById(Order order);

    @Select("select * from orders where id = #{orderId}")
    Order selectById(Integer orderId);

    List<Order> selectList(OrdersRole orderRole);

    List<Order> selectByIdList(HashSet<Integer> orderIds);

    @Select("select * from orders")
    Page<OrderListVO> getOrderList();

    @Select("select count(*) from orders")
    Integer getCount();

//    @Select("select count(*) from orders where status = #{status}")
    Integer getCountByStatus(String status, LocalDateTime now);

    @Select("select sum(amount) from orders where status = 'completed'")
    BigDecimal getTotalAmount();

    @Select("select count(*) as count, status from orders group by status")
    ArrayList<OrderCountByStatus> getCountTotalStatus();

    ArrayList<Integer> getCountByStatus7d(String status, LocalDateTime now);
}

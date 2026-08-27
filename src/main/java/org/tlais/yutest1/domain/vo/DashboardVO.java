package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tlais.yutest1.domain.entity.OrderCountByStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardVO implements Serializable {
    /** 总用户数 */
    private Integer totalUsers;
    /** 近 7 天活跃用户数（有登录行为） */
    private Integer activeUsers7Days;
    /** 总在售书籍数 */
    private Integer totalBooksSelling;
    /** 总订单数 */
    private Integer totalOrders;
    /** 已完成订单数 */
    private Integer completedOrders;
    /** 总销售额 */
    private BigDecimal totalSales;
    /** 各状态订单数量 */
    private ArrayList<OrderCountByStatus> orderStatusCount;
    /** 近 7 天每天新增订单数 */
    private ArrayList<Integer> newOrders7Days;
}

package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 订单详情视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderVO implements Serializable {

    /** 订单ID */
    private String id;

    /** 买家信息 */
    private UserSimpleVO buyer;

    /** 卖家信息 */
    private UserSimpleVO seller;

    /** 书籍信息 */
    private BookSimpleVO book;

    /** 成交金额 */
    private String amount;

    /** 订单状态 */
    private String status;

    /** 取消原因 */
    private String cancelReason;

    /** 乐观锁版本号 */
    private Integer version;

    /** 确认时间 */
    private String confirmedAt;

    /** 发货时间 */
    private String shippedAt;

    /** 收货时间 */
    private String receivedAt;

    /** 完成时间 */
    private String completedAt;

    /** 取消时间 */
    private String cancelledAt;

    /** 创建时间 */
    private String createdAt;

}

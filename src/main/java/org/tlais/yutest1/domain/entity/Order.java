package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order implements Serializable {

    /** 订单ID（UUID） */
    private String id;

    /** 买家ID */
    private String buyerId;

    /** 卖家ID */
    private String sellerId;

    /** 书籍ID */
    private String bookId;

    /** 成交金额 */
    private BigDecimal amount;

    /** 订单状态（pending/confirmed/shipped/received/completed/cancelled） */
    private String status;

    /** 取消原因 */
    private String cancelReason;

    /** 乐观锁版本号 */
    private Integer version;

    /** 确认时间 */
    private LocalDateTime confirmedAt;

    /** 发货时间 */
    private LocalDateTime shippedAt;

    /** 收货时间 */
    private LocalDateTime receivedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 取消时间 */
    private LocalDateTime cancelledAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}

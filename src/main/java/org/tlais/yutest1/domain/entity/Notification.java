package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification implements Serializable {

    /** 通知ID（UUID） */
    private Integer id;

    /** 目标用户ID */
    private String userId;

    /** 通知类型（order_created/order_confirmed/.../system） */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 是否已读 */
    private Boolean isRead = false;

    /** 关联订单ID */
    private Integer relatedOrderId;

    /** 关联图书ID */
    private String relatedBookId;

    /** 创建时间 */
    private LocalDateTime createdAt;

}

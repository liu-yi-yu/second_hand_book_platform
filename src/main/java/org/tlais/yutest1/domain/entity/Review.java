package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评价实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Review implements Serializable {

    /** 评价ID（UUID） */
    private String id;

    /** 关联订单ID */
    private Integer orderId;

    /** 评价者ID */
    private String reviewerId;

    /** 被评价用户ID */
    private String targetUserId;

    /** 评分（1-5） */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createdAt;

}

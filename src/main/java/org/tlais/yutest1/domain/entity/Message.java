package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message implements Serializable {

    /** 消息ID（UUID） */
    private String id;

    /** 关联订单ID */
    private Integer orderId;

    /** 发送者ID */
    private String senderId;

    private String receiverId;

    /** 消息内容 */
    private String content;

    /** 客户端唯一ID（用于去重） */
    private String clientId;

    /** 发送时间 */
    private LocalDateTime createdAt;

}

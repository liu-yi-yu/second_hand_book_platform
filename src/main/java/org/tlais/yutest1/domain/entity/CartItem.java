package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 购物车实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItem implements Serializable {

    /** 购物车项ID（UUID） */
    private String id;

    /** 用户ID */
    private String userId;

    /** 书籍ID */
    private String bookId;

    /** 添加时间 */
    private LocalDateTime createdAt;

}

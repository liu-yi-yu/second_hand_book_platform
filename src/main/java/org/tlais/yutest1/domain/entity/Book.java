package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.time.LocalDateTime;

/**
 * 书籍实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Book implements Serializable {

    /** 书籍ID（UUID） */
    private String id;

    /** 卖家ID */
    private String sellerId;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** ISBN编号 */
    private String isbn;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 售价 */
    private BigDecimal sellingPrice;

    /** 品相（brand_new/like_new/used/old） */
    private String condition;

    /** 分类（literature/textbook/professional/comic/children/other/novel/magazine） */
    private String category;

    /** 描述 */
    private String description;

    /** 状态（selling/sold/removed） */
    private String status;

    /** 浏览次数 */
    private Integer viewCount;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}

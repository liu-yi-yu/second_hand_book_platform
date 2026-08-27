package org.tlais.yutest1.domain.vo;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

/**
 * 书籍列表项视图对象（用于搜索/列表结果）
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BookListVO implements Serializable {

    /** 书籍ID */
    private String id;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 原价 */
    private String originalPrice;

    /** 售价 */
    private String sellingPrice;

    /** 品相 */
    private String condition;

    /** 分类 */
    private String category;

    /** 状态 */
    private String status;

    /** 浏览次数 */
    private Integer viewCount;

    /** 封面图URL（第一张图片缩略图） */
    private String coverImage;

    /** 卖家用户名 */
    private String sellerName;

    /** 卖家ID */
    private String sellerId;

    /** 发布时间 */
    private String createdAt;

}

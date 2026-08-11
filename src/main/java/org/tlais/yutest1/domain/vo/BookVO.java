package org.tlais.yutest1.domain.vo;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;
import org.tlais.yutest1.constant.BookStatu;

import java.util.List;

/**
 * 书籍详情视图对象
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BookVO implements Serializable {

    /** 书籍ID */
    private String id;

    /** 卖家信息 */
    private UserSimpleVO seller;

    /** 卖家ID */
    private String sellerId;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** ISBN编号 */
    private String isbn;

    /** 原价 */
    private String originalPrice;

    /** 售价 */
    private String sellingPrice;

    /** 品相 */
    private String condition;

    /** 分类 */
    private String category;

    /** 描述 */
    private String description;

    /** 状态 */
    private String status = BookStatu.SELLING;

    /** 浏览次数 */
    private Integer viewCount=0;

    /** 版本号（乐观锁，前端下单时回传） */
    private Integer version=0;

    /** 图片列表 */
    private List<ImageVO> images;

    /** 当前用户是否已收藏 */
    private Boolean isFavorited;

    /** 发布时间 */
    private String createdAt;

    /** 更新时间 */
    private String updatedAt;

}

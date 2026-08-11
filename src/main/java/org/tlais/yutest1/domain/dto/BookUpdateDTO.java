package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.util.List;

/**
 * 书籍信息更新请求（所有字段可选，按需部分更新）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookUpdateDTO implements Serializable {

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

    /** 品相 */
    private String condition;

    /** 分类 */
    private String category;

    /** 描述 */
    private String description;

    /** 关联图片ID列表（替换整个列表） */
    private List<String> imageIds;

    /** 状态（可用于下架操作：selling → removed） */
    private String status;

}

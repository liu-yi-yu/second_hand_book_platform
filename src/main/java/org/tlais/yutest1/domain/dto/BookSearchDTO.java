package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

import org.tlais.yutest1.constant.SortBy;

import java.math.BigDecimal;

/**
 * 书籍搜索请求参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookSearchDTO implements Serializable {

    /** 搜索关键词（匹配书名/作者/ISBN） */
    private String keyword;

    /** 分类筛选 */
    private String category;

    /** 品相筛选 */
    private String condition;

    /** 最低价格 */
    private BigDecimal minPrice;

    /** 最高价格 */
    private BigDecimal maxPrice;

    /** 排序字段（默认按创建时间降序） */
    private String sortBy = SortBy.NEWEST;

    /** 页码（默认1） */
    private Integer pageNum = 1;

    /** 每页数量（默认20） */
    private Integer pageSize = 20;

}

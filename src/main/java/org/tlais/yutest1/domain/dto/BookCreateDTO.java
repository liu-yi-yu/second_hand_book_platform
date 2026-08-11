package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.util.List;

/**
 * 书籍发布请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookCreateDTO implements Serializable {

    /** 书名 */
    @NotBlank(message = "书名不能为空")
    private String title;

    /** 作者 */
    @NotBlank(message = "作者不能为空")
    private String author;

    /** ISBN编号（可选） */
    private String isbn;

    /** 原价 */
    @NotNull(message = "原价不能为空")
//    @JsonProperty("original_price")
    private BigDecimal originalPrice;

    /** 售价 */
    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于0")
//    @JsonProperty("selling_price")
    private BigDecimal sellingPrice;

    /** 品相（brand_new/like_new/used/old） */
    @NotBlank(message = "品相不能为空")
    private String condition;

    /** 分类（literature/textbook/professional/comic/children/other/novel/magazine） */
    @NotBlank(message = "分类不能为空")
    private String category;

    /** 描述 */
    private String description=null;

    /** 关联图片ID列表 */
    private List<String> imageIds=null;

}

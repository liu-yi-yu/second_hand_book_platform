package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

/**
 * 创建评价请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewCreateDTO implements Serializable {

    /** 评价的订单ID */
    @NotNull(message = "订单ID不能为空")
    private Integer orderId;

    /** 评分（1-5） */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1")
    @Max(value = 5, message = "评分最高为5")
    private Integer rating;

    /** 评价内容（可选） */
    @Size( max = 500)
    private String content;

}

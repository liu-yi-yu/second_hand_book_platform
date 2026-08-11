package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;

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
    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    /** 评分（1-5） */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1")
    @Max(value = 5, message = "评分最高为5")
    private Integer rating;

    /** 评价内容（可选） */
    private String content;

}

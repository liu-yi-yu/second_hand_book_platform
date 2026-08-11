package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

/**
 * 创建订单请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateDTO implements Serializable {

    /** 购买的书籍ID */
    @NotBlank(message = "书籍ID不能为空")
    private String bookId;

    /** 初始留言（可选） */
    private String message;

}

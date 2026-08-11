package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

/**
 * 添加购物车请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartAddDTO implements Serializable {

    /** 要添加的书籍ID */
    @NotBlank(message = "书籍ID不能为空")
    private String bookId;

}

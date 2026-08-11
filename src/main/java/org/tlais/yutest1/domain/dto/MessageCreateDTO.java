package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

/**
 * 发送消息请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageCreateDTO implements Serializable {

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 客户端唯一ID（用于去重） */
    @NotBlank(message = "客户端ID不能为空")
    private String clientId;

}

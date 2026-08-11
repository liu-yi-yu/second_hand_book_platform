package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO implements Serializable {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3,max = 20, message = "用户名长度必须在3到20位之间")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 8,max = 64, message = "密码长度必须在8到64位之间")
    private String password;
    @NotBlank(message = "邮箱不能为空")
    private String email;

}

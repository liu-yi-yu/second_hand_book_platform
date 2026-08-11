package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

/**
 * 用户信息更新请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO implements Serializable {

    /** 头像URL */
    private String avatarUrl;

    /** 个人简介 */
    private String bio;

}

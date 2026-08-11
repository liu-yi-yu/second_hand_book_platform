package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 用户主页视图对象（公开查看）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileVO implements Serializable {

    /** 用户ID */
    private String id;

    /** 用户名 */
    private String username;

    /** 头像URL */
    private String avatarUrl;

    /** 个人简介 */
    private String bio;

    /** 信用分 */
    private Integer creditScore;

    /** 在售数量 */
    private Integer sellingCount;

    /** 已售数量 */
    private Integer soldCount;

    /** 注册时间 */
    private String createdAt;

}

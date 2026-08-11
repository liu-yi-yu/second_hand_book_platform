package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 用户简要信息视图对象（嵌入其他VO使用）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSimpleVO implements Serializable {

    /** 用户ID */
    private String id;

    /** 用户名 */
    private String username;

    /** 头像URL */
    private String avatarUrl;

}

package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 消息视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageVO implements Serializable {

    /** 消息ID */
    private String id;

    /** 订单ID */
    private String orderId;

    /** 发送者信息 */
    private UserSimpleVO sender;

    /** 消息内容 */
    private String content;

    /** 发送时间 */
    private String createdAt;

}

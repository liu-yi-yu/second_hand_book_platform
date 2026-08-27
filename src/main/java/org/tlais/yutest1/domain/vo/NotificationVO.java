package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 通知视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationVO implements Serializable {

    /** 通知ID */
    private Integer id;

    /** 通知类型 */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 是否已读 */
    private Boolean isRead;

    /** 关联业务ID */
    private String relatedBookId;

    private Integer relatedOrderId;

    /** 创建时间 */
    private String createdAt;

}

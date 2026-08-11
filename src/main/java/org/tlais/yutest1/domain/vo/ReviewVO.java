package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 评价视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewVO implements Serializable {

    /** 评价ID */
    private String id;

    /** 关联订单ID */
    private String orderId;

    /** 评价者信息 */
    private UserSimpleVO reviewer;

    /** 评分（1-5） */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 创建时间 */
    private String createdAt;

}

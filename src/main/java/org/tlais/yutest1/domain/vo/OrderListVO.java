package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单列表项视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderListVO implements Serializable {

    /** 订单ID */
    private Integer id;

    /** 书ID */
    private String bookId;

    /** 书名 */
    private String bookTitle;

    /** 封面图URL */
    private String bookCoverImage;

    /** 金额 */
    private BigDecimal amount;

    /** 状态 */
    private String status;

    /** 对方用户名（买家视角显示卖家，卖家视角显示买家） */
    private String counterpartyName;

    /** 对方头像 */
    private String counterpartyAvatar;

    /** 创建时间 */
    private String createdAt;

    //卖家确认时间
    private String confirmedAt;

    //发货时间
    private String shippedAt;

    //确认收货时间
    private String receivedAt;

    //完成时间
    private String completedAt;

}

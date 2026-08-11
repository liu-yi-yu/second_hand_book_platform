package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 购物车项视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItemVO implements Serializable {

    /** 购物车项ID */
    private String id;

    /** 书籍信息 */
    private BookSimpleVO book;

    /** 卖家信息 */
    private UserSimpleVO seller;

    /** 当前售价 */
    private String sellingPrice;

    /** 添加时间 */
    private String createdAt;

}

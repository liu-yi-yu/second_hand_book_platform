package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 收藏项视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteVO implements Serializable {

    /** 收藏ID */
    private String id;

    /** 书籍信息 */
    private BookSimpleVO book;

    /** 当前售价 */
    private String sellingPrice;

    /** 书籍状态（前端据此判断是否已售出） */
    private String bookStatus;

    /** 收藏时间 */
    private String createdAt;

}

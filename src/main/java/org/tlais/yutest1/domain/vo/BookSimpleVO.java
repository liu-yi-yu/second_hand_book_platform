package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 书籍简要信息视图对象（嵌入其他VO使用）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookSimpleVO implements Serializable {

    /** 书籍ID */
    private String id;

    /** 书名 */
    private String title;

    /** 封面图URL */
    private String coverImage;

}

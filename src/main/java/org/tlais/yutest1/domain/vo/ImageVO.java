package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import java.io.Serializable;

/**
 * 图片视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageVO implements Serializable {

    /** 图片ID */
    private String id;

    /** 原图URL */
    private String url;

    /** 缩略图URL */
    private String thumbnailUrl;

}

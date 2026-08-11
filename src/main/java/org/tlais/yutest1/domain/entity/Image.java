package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图片实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Image implements Serializable {

    /** 图片ID（UUID） */
    private String id;

    /** 上传用户ID */
    private String userId;

    /** 原图URL */
    private String url;

    /** 缩略图URL */
    private String thumbnailUrl;

    /** 是否已被引用 */
    private Boolean isUsed;

    /** 创建时间 */
    private LocalDateTime createdAt;

}

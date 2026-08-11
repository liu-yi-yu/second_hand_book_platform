package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

/**
 * 书籍图片关联实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookImageRelation implements Serializable {

    /** 关联ID（UUID） */
    private String id;

    /** 书籍ID */
    private String bookId;

    /** 图片ID */
    private String imageId;

    /** 排序序号 */
    private Integer sortOrder;

}

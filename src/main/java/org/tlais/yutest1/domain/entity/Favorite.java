package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Favorite implements Serializable {

    /** 收藏ID（UUID） */
    private String id;

    /** 用户ID */
    private String userId;

    /** 书籍ID */
    private String bookId;

    /** 收藏时间 */
    private LocalDateTime createdAt;

}

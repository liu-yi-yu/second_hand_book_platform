package org.tlais.yutest1.domain.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshToken implements Serializable {

    /** 令牌ID（UUID） */
    private String id;

    /** 用户ID */
    private String userId;

    /** 令牌哈希值（SHA-256） */
    private String tokenHash;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

}

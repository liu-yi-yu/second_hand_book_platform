package org.tlais.yutest1.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员操作日志实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminOperationLog implements Serializable {

    /** 日志ID（UUID） */
    private String id;

    /** 管理员ID */
    private String adminId;

    /** 操作动作 */
    private String action;

    /** 操作目标类型 */
    private String targetType;

    /** 操作目标ID */
    private String targetId;

    /** 操作详情 */
    private String detail;

    /** 操作时间 */
    private LocalDateTime createdAt;

}

package org.tlais.yutest1.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationsDTO implements Serializable {
    /**
     * 页码（默认1）
     */
    @NotBlank(message = "页码不能为空")
    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    /**
     * 每页数量（默认20）
     */
    @NotBlank(message = "每页数量不能为空")
    @Size(min = 1)
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 20;

    /**
     * 是否只显示未读通知（默认false）
     */
    private Boolean unreadOnly = false;

    // 工具方法获取，业务统一调用
    public Integer getPage() {
        return page == null ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null ? 20 : pageSize;
    }

    public Boolean isUnreadOnly() {
        return unreadOnly == null ? false : unreadOnly;
    }



}
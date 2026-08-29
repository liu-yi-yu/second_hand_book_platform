package org.tlais.yutest1.domain.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;

/**
 * 通用分页请求参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageDTO implements Serializable {

    /** 页码（默认1） */
    private Integer page = 1;

    /** 每页数量（默认20） */
    private Integer pageSize = 20;

    public Integer getPage() {
        return page == null ? 1 : page;
    }

    public Integer getPageSize() {
        return pageSize == null ? 20 : pageSize;
    }

}

package org.tlais.yutest1.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tlais.yutest1.constant.OrderRole;
import org.tlais.yutest1.constant.SortBy;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 搜索请求参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderSearchDTO implements Serializable {

    private String role = OrderRole.BUYER;

    private String status;

    /** 页码（默认1） */
    private Integer pageNum = 1;

    /** 每页数量（默认20） */
    private Integer pageSize = 20;

}

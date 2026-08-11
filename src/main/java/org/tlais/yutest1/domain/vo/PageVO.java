package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页响应
 *
 * @param <T> 数据列表元素类型
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PageVO<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

//    /** 当前页码 */
//    private Integer pageNum;
//
//    /** 每页数量 */
//    private Integer pageSize;


}

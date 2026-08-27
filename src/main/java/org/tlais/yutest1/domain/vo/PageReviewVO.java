package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PageReviewVO<T> {
    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    private Double avgRating;

    private Integer reviewCount;

    private HashMap<String,Integer> ratingMap;
}

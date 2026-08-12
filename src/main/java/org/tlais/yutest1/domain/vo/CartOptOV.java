package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartOptOV implements Serializable {
    private List<CartItemVO> cartItemVOList;
    private BigDecimal totalPrice;
    private Integer count;
}

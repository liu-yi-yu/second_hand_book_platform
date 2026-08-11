package org.tlais.yutest1.domain.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消订单请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCancelDTO implements Serializable {

    /** 取消原因 */
    private String cancelReason;

}

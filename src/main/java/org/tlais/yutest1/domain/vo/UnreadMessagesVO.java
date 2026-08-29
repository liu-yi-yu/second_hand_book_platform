package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tlais.yutest1.domain.entity.ByOrder;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnreadMessagesVO implements Serializable {
    private Integer count=0;
    private List<ByOrder> byOrders;
}

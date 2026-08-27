package org.tlais.yutest1.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCountByStatus {
    private Integer count;
    private String status;
}

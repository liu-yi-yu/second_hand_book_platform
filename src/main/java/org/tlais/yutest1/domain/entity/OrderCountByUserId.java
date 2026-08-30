package org.tlais.yutest1.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCountByUserId implements Serializable {
    private String userId;
    private Integer orderCount;
}

package org.tlais.yutest1.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersRole implements Serializable {
    private String sellerId;
    private String buyerId;
    private String[] status;
}

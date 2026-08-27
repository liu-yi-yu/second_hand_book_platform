package org.tlais.yutest1.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ByOrder implements Serializable {
    private Integer orderId;
    private String title;
    private String userName;
    private Integer count;
    private String content;

}

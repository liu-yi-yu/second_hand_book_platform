package org.tlais.yutest1.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * 创建订单请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatesDTO implements Serializable {

    private ArrayList<String> bookIds;

}

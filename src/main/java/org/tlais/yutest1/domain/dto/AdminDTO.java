package org.tlais.yutest1.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDTO implements Serializable {
    private String keyword;
    private String role;
    private String status;
}

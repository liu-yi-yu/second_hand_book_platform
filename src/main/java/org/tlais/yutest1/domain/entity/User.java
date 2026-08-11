package org.tlais.yutest1.domain.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User implements Serializable {
    private String id;
    private String username;
    private String passwordHash;
    private String email;
    private String avatarUrl = null;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String role="user";
    private String status="active";
    private Integer creditScore =100;

    private String bio;
    private Integer sellingCount;
    private Integer soldCount;
}

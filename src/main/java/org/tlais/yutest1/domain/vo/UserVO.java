package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserVO implements Serializable {

    private String id;
    private String username;
    private String passwordHash;
    private String email;
    private LocalDateTime createdAt;
    private String token;
    private String prefix;
    private String avatarUrl ;
}

package org.tlais.yutest1.domain.vo;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Builder
@Data
public class UserGetVO implements Serializable {
    private String id;
    private String username;
    private String email;
    private String avatar;
    private LocalDateTime createdAT;

    private String bio;
    private Integer score;
    private Integer sellingCount;
    private Integer soldCount;

    private String created;

}

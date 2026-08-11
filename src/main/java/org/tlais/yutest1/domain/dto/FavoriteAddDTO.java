package org.tlais.yutest1.domain.dto;

import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加收藏请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteAddDTO implements Serializable {

    /** 要收藏的书籍ID */
    @NotBlank(message = "书籍ID不能为空")
    private String bookId;

}

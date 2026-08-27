package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PageNotificationVO<T> {
    private PageVO<T> pageVO;
    private Integer unreadCount;
}

package org.tlais.yutest1.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import org.tlais.yutest1.domain.entity.Message;

import java.io.Serializable;
import java.util.List;

/**
 * 消息视图对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageVO implements Serializable {

    /** 消息列表 */
    private List<Message> message;

    /** 是否还有更多消息 */
    private boolean hasMore;

}

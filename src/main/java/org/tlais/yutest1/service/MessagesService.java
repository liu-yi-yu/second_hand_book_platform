package org.tlais.yutest1.service;

import org.tlais.yutest1.domain.dto.MessageCreateDTO;
import org.tlais.yutest1.domain.entity.ByOrder;
import org.tlais.yutest1.domain.entity.Message;
import org.tlais.yutest1.domain.vo.MessageVO;
import org.tlais.yutest1.domain.vo.UnreadMessagesVO;

import java.util.List;

public interface MessagesService {
    MessageVO getMessages(Integer orderId, Integer limit);

    UnreadMessagesVO getUnreadMessagesCount();

    void readMessages(Integer orderId);

    /**
     * 发送一条聊天消息（由 WebSocket 端点调用）
     *
     * @param senderId 发送者用户ID
     * @param dto      消息内容（orderId + content + clientId）
     * @return 落库后的完整消息对象
     */
    Message sendMessage(String senderId, MessageCreateDTO dto);
}

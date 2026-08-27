package org.tlais.yutest1.service;

import org.tlais.yutest1.domain.entity.ByOrder;
import org.tlais.yutest1.domain.vo.MessageVO;

import java.util.List;

public interface MessagesService {
    MessageVO getMessages(Integer orderId, Integer limit);

    List<ByOrder> getUnreadMessagesCount();
}

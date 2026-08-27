package org.tlais.yutest1.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationsDTO implements Serializable {
    private PageDTO pageDTO;
    private boolean unreadOnly=false;
}

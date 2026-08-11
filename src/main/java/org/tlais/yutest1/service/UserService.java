package org.tlais.yutest1.service;

import org.tlais.yutest1.domain.dto.UserDTO;
import org.tlais.yutest1.domain.dto.UserUpdateDTO;
import org.tlais.yutest1.domain.entity.User;
import org.tlais.yutest1.domain.vo.UserGetVO;
import org.tlais.yutest1.domain.vo.UserProfileVO;

public interface UserService {
    User register(UserDTO userDTO);

    User login(UserDTO userDTO);

    UserGetVO get();

    void update(UserUpdateDTO userUpdateDTO);

    UserProfileVO getById(String userId);
}

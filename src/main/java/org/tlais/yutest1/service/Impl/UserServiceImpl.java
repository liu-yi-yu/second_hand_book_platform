package org.tlais.yutest1.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.tlais.yutest1.constant.Status;
import org.tlais.yutest1.constant.UserException;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.dto.UserDTO;
import org.tlais.yutest1.domain.dto.UserUpdateDTO;
import org.tlais.yutest1.domain.vo.UserGetVO;
import org.tlais.yutest1.domain.vo.UserProfileVO;
import org.tlais.yutest1.exception.BusinessException;
import org.tlais.yutest1.mapper.UserMapper;
import org.tlais.yutest1.service.UserService;

import org.springframework.stereotype.Service;
import org.tlais.yutest1.domain.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper ;

    @Override
    public User register(UserDTO userDTO) {
        // 验证用户名是否存在
        User selectUser = userMapper.selectByUsername(userDTO.getUsername());
        if (selectUser != null) {
            throw new BusinessException(40001, UserException.USER_USERNAME_EXIST);
        }
        User selectEmail=userMapper.selectByEmail(userDTO.getEmail());
        if (selectEmail != null) {
            throw new BusinessException(40001, UserException.USER_EMAIL_EXIST);
        }

        /// 对密码进行加密
        String hashpw = BCrypt.hashpw(userDTO.getPassword(), BCrypt.gensalt());
        User user = new User();
        BeanUtils.copyProperties(userDTO,user);
        user.setPasswordHash(hashpw);
        // 生成UUID作为用户ID
        user.setId(UUID.randomUUID().toString().substring(0, 10));
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        int insert = userMapper.insert(user);
        if (insert == 0) {
            throw new BusinessException(40001, UserException.USER_REGISTER_FAILED);
        }
        return user;
    }

    @Override
    public User login(UserDTO userDTO) {
        User selectUser = userMapper.selectByUsername(userDTO.getUsername());
        // 验证密码是否匹配
        // 对用户输入的密码进行加密
        if (selectUser == null||!BCrypt.checkpw(userDTO.getPassword(), selectUser.getPasswordHash())) {
            throw new BusinessException(40001, UserException.USER_PASSWORD_ERROR);
        }
        if (selectUser.getStatus().equals(Status.USER_DISABLED)) {
            throw new BusinessException(40001, UserException.USER_STATUS_INVALID);
        }
        return selectUser;
    }

    @Override
    public UserGetVO get() {
        User user = userMapper.selectById(BaseContext.getCurrentId());
        log.info("当前用户ID:{}",BaseContext.getCurrentId());
        UserGetVO userGetVO = UserGetVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatarUrl())
                .createdAT(user.getCreatedAt())
                .bio(user.getBio())
                .score(user.getCreditScore())
                .sellingCount(user.getSellingCount())
                .soldCount(user.getSoldCount())
                .created(user.getCreatedAt().toString())
                .build();
        return userGetVO;
    }

    @Override
    public void update(UserUpdateDTO userUpdateDTO) {
        User user = new User();
        BeanUtils.copyProperties(userUpdateDTO,user);
        userMapper.update(user);
    }

    @Override
    public UserProfileVO getById(String userId) {
        User user = userMapper.selectById(userId);
        UserProfileVO userProfileVO = UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .creditScore(user.getCreditScore())
                .sellingCount(user.getSellingCount())
                .soldCount(user.getSoldCount())
                .createdAt(user.getCreatedAt().toString())
                .build();
        return userProfileVO;
    }
}

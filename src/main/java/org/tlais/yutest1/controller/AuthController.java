package org.tlais.yutest1.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.domain.dto.UserDTO;
import org.tlais.yutest1.properties.JwtProperties;
import org.tlais.yutest1.service.UserService;
import org.tlais.yutest1.domain.entity.User;
import org.tlais.yutest1.domain.vo.UserVO;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;


    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody UserDTO userDTO) {
        //TODO : 尽量在最后可以弄一个通过邮箱发送的验证码
        User register = userService.register(userDTO);

        //登录成功后，生成jwt令牌
        String token = jwtProperties.generateToken(register.getId());

//        Map<String, Object> map = new HashMap<>();
//        map.put("token", token);
//        prefix: Bearer：约定的令牌前缀，告诉前端拼接格式 Bearer + 空格 + token，用于请求头鉴权。
//        map.put("prefix", "Bearer");
        UserVO userVO = UserVO.builder()
                .id(register.getId())
                .username(register.getUsername())
                .passwordHash(register.getPasswordHash())
                .email(register.getEmail())
                .createdAt(register.getCreatedAt())
                .token(token)
//                .prefix("Bearer ")
                .build();

        return Result.success(userVO);
    }

    @PostMapping("/login")
    public Result login(@RequestBody UserDTO userDTO) {
        BaseContext.removeCurrentId();
        User loginUser = userService.login(userDTO);
        String token = jwtProperties.generateToken(loginUser.getId());
        BaseContext.setCurrentId(loginUser.getId());
        UserVO userVO = UserVO.builder()
                .id(loginUser.getId())
                .username(loginUser.getUsername())
                .email(loginUser.getEmail())
                .createdAt(loginUser.getCreatedAt())
                .token(token)
//                .prefix("Bearer")
                .avatarUrl(loginUser.getAvatarUrl())
                .build();
        log.info("Token:{}",userVO.getToken());

        return Result.success(userVO);
    }

}

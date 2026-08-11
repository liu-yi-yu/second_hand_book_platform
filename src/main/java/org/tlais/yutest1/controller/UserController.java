package org.tlais.yutest1.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tlais.yutest1.domain.dto.UserDTO;
import org.tlais.yutest1.domain.dto.UserUpdateDTO;
import org.tlais.yutest1.domain.entity.Result;
import org.tlais.yutest1.domain.vo.UserGetVO;
import org.tlais.yutest1.service.UserService;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result get() {
        log.info("获取当前用户信息");
        return Result.success(userService.get());
    }

    @PutMapping("/me")
    @Operation(summary = "更新当前用户信息")
    public Result update(UserUpdateDTO userUpdateDTO) {
        log.info("更新当前用户信息:{}",userUpdateDTO);
        userService.update(userUpdateDTO);
        return Result.success();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "根据用户ID获取用户信息")
    public Result getById(@PathVariable String userId) {
        return Result.success(userService.getById(userId));
    }
}

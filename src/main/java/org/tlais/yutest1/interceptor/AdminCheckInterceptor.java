package org.tlais.yutest1.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.tlais.yutest1.constant.UserException;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.domain.entity.User;
import org.tlais.yutest1.mapper.UserMapper;

@Component
public class AdminCheckInterceptor implements HandlerInterceptor {
    @Autowired
    private UserMapper userMapper;

    /**
     * 检查用户是否为管理员
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String currentId = BaseContext.getCurrentId();
        User user = userMapper.selectById(currentId);
        if (user == null || !user.getRole().equals("admin")) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":40003,\"msg\":\"" + UserException.USER_NOT_ADMIN + "\"}");
            return false;
        }
        return true;
    }
}

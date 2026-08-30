package org.tlais.yutest1.interceptor;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.web.servlet.HandlerInterceptor;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.properties.JwtProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取header
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":40101,\"msg\":\"未登录，请先登录\"}");
            return false;
        }
        // 兼容 "Bearer xxx" 格式，自动去掉前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // 2. 校验token合法性
        if (!jwtProperties.validateToken(token)) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":40102,\"msg\":\"令牌无效或已过期\"}");
            return false;
        }
        // 3. 把用户ID存入request，后续接口直接获取登录用户
        String userId = jwtProperties.getUserIdByToken(token);
        BaseContext.setCurrentId(userId);
        request.setAttribute("loginUserId", userId);
        log.info("当前用户ID:{}",BaseContext.getCurrentId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }

}

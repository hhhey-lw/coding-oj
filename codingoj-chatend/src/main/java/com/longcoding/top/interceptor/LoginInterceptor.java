package com.longcoding.top.interceptor;

import com.longcoding.top.utils.JwtTokenUtil;
import com.longcoding.top.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            // 预检请求直接放行，不走登录逻辑
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        // 1. 从请求头中获取 Authorization
        String token = request.getHeader("Authorization");
        System.out.println("Token ==============> " + token);
        // 2. 检查 Authorization 是否存在且格式正确（Bearer Token）
        if (token == null) {
            // 未携带 Token 或格式错误，返回 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"请先登录！\", data: \"\"}");
            return false; // 拦截请求
        }
        if (!JwtTokenUtil.validateToken(token)) {
            // Token 验证失败（例如，过期或签名无效），返回 403
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 403, \"msg\": \"Token无效或已过期，请重新登录！\", \"data\": null}");
            return false; // 拦截请求
        }
        // 3. 将 Token 存入 ThreadLocal（供后续业务逻辑使用）
        Long userId = JwtTokenUtil.getUserIdFromToken(token);
        log.debug("userId: " + userId);
        UserContext.setUserId(userId);

        // 4. Token 有效，放行请求
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清除 ThreadLocal 中的 Token（防止内存泄漏）
        UserContext.clearToken();
        log.debug("UserContext.clearToken()");
    }
}

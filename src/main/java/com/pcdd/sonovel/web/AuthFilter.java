package com.pcdd.sonovel.web;

import com.pcdd.sonovel.core.AppConfigLoader;
import com.pcdd.sonovel.model.AppConfig;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Web 认证过滤器
 * <p>
 * 当 auth.enabled=1 时，未登录用户访问受保护资源返回 401。
 * /login, /logout, /auth-status, 静态资源(css/js/favicon) 不受限制。
 */
public class AuthFilter implements Filter {

    // 不需要认证的路径前缀
    private static final String[] PUBLIC_PATHS = {
            "/login",
            "/logout",
            "/auth-status",
            "/favicon.ico",
            "/js/",
    };

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        AppConfig cfg = AppConfigLoader.APP_CONFIG;

        // 认证未启用，直接放行
        if (cfg.getAuthEnabled() == null || cfg.getAuthEnabled() != 1) {
            chain.doFilter(request, response);
            return;
        }

        String path = req.getServletPath();

        // 公开路径放行
        for (String pub : PUBLIC_PATHS) {
            if (path.startsWith(pub)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 检查 session 中的认证标记
        HttpSession session = req.getSession(false);
        boolean authenticated = session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));

        if (authenticated) {
            chain.doFilter(request, response);
            return;
        }

        // 未认证：API 返回 401 JSON，页面请求重定向到首页（前端弹登录框）
        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
        } else {
            // 对于页面请求（如直接访问 /），放行让前端 index.html 加载后自行检查认证状态
            // 只有 API 调用才拦截
            if (path.equals("/") || path.equals("/index.html")) {
                chain.doFilter(request, response);
            } else {
                resp.setStatus(401);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
            }
        }
    }

    @Override
    public void destroy() {
    }
}

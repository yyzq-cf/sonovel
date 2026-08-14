package com.pcdd.sonovel.web.servlet;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.pcdd.sonovel.core.AppConfigLoader;
import com.pcdd.sonovel.model.AppConfig;
import com.pcdd.sonovel.web.util.RespUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 登录 / 登出 / 认证状态检查
 * <p>
 * POST /login   { "username": "xxx", "password": "xxx" }
 * POST /logout
 * GET  /auth-status
 */
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();

        if ("/login".equals(path)) {
            handleLogin(req, resp);
        } else if ("/logout".equals(path)) {
            handleLogout(req, resp);
        } else {
            resp.setStatus(404);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // GET /auth-status
        AppConfig cfg = AppConfigLoader.APP_CONFIG;
        boolean authEnabled = cfg.getAuthEnabled() != null && cfg.getAuthEnabled() == 1;

        HttpSession session = req.getSession(false);
        boolean authenticated = session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));

        writeJson(resp, 200, "OK", new AuthStatus(authEnabled, authenticated));
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppConfig cfg = AppConfigLoader.APP_CONFIG;

        // 读取 JSON body
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        String username;
        String password;
        try {
            JSONObject body = JSONUtil.parseObj(sb.toString());
            username = body.getStr("username");
            password = body.getStr("password");
        } catch (Exception e) {
            writeJson(resp, 400, "请求格式错误", null);
            resp.setStatus(400);
            return;
        }

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            writeJson(resp, 400, "用户名和密码不能为空", null);
            resp.setStatus(400);
            return;
        }

        if (username.equals(cfg.getAuthUsername()) && password.equals(cfg.getAuthPassword())) {
            HttpSession session = req.getSession(true);
            session.setAttribute("authenticated", true);
            session.setMaxInactiveInterval(7 * 24 * 60 * 60); // 7 天
            writeJson(resp, 200, "登录成功", null);
        } else {
            writeJson(resp, 401, "用户名或密码错误", null);
            resp.setStatus(401);
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        writeJson(resp, 200, "已退出登录", null);
    }

    // 认证状态响应体
    public record AuthStatus(boolean authEnabled, boolean authenticated) {}

    /**
     * 直接写入 JSON 响应，避免 RespUtils 的双重包装
     */
    private void writeJson(HttpServletResponse resp, int code, String message, Object data) {
        resp.setStatus(code);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            JSONObject json = new JSONObject();
            json.set("code", code);
            json.set("message", message);
            json.set("data", data);
            writer.println(json.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

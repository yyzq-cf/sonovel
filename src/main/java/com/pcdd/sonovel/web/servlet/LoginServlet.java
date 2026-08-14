package com.pcdd.sonovel.web.servlet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.Setting;
import com.pcdd.sonovel.core.AppConfigLoader;
import com.pcdd.sonovel.model.AppConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * 登录 / 登出 / 认证状态检查 / 修改密码
 * <p>
 * POST /login         { "username": "xxx", "password": "xxx" }
 * POST /logout
 * POST /change-password { "oldPassword": "xxx", "newUsername": "xxx", "newPassword": "xxx" }
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
        } else if ("/change-password".equals(path)) {
            handleChangePassword(req, resp);
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

        JSONObject body = readJsonBody(req);
        if (body == null) {
            writeJson(resp, 400, "请求格式错误", null);
            resp.setStatus(400);
            return;
        }

        String username = body.getStr("username");
        String password = body.getStr("password");

        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
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

    private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 必须已登录
        HttpSession session = req.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute("authenticated"))) {
            writeJson(resp, 401, "请先登录", null);
            resp.setStatus(401);
            return;
        }

        AppConfig cfg = AppConfigLoader.APP_CONFIG;

        JSONObject body = readJsonBody(req);
        if (body == null) {
            writeJson(resp, 400, "请求格式错误", null);
            resp.setStatus(400);
            return;
        }

        String oldPassword = body.getStr("oldPassword");
        String newUsername = body.getStr("newUsername");
        String newPassword = body.getStr("newPassword");

        // 验证旧密码
        if (StrUtil.isBlank(oldPassword) || !oldPassword.equals(cfg.getAuthPassword())) {
            writeJson(resp, 403, "原密码错误", null);
            resp.setStatus(403);
            return;
        }

        if (StrUtil.isBlank(newUsername) || StrUtil.isBlank(newPassword)) {
            writeJson(resp, 400, "新用户名和密码不能为空", null);
            resp.setStatus(400);
            return;
        }

        // 定位 config.ini 文件路径
        String configFilePath = System.getProperty("config.file");
        if (StrUtil.isBlank(configFilePath) || !FileUtil.exist(configFilePath)) {
            // 默认路径：与 jar 同目录的 config.ini
            String jarDir = new File(LoginServlet.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParent();
            configFilePath = Paths.get(jarDir, "config.ini").toString();
        }

        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            writeJson(resp, 500, "配置文件不存在: " + configFilePath, null);
            resp.setStatus(500);
            return;
        }

        // 更新 config.ini 中的 [auth] 段
        Setting setting = new Setting(configFile.getAbsolutePath(), StandardCharsets.UTF_8, false);
        setting.set("username", "auth", newUsername);
        setting.set("password", "auth", newPassword);
        setting.store(configFile.getAbsolutePath());

        // 更新内存中的配置
        cfg.setAuthUsername(newUsername);
        cfg.setAuthPassword(newPassword);

        writeJson(resp, 200, "密码修改成功，请重新登录", null);
    }

    private JSONObject readJsonBody(HttpServletRequest req) throws IOException {
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        try {
            return JSONUtil.parseObj(sb.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // 认证状态响应体
    public record AuthStatus(boolean authEnabled, boolean authenticated) {}

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

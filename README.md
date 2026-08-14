# So Novel (Docker 认证版)

> 基于 [freeok/so-novel](https://github.com/freeok/so-novel) 修改，原项目作者 **pcdd / freeok**，遵循原项目 MIT 许可证。

## 📌 项目说明

本项目在原版 [so-novel](https://github.com/freeok/so-novel) 基础上做了以下修改：

### 新增功能

| 功能 | 说明 |
|---|---|
| 🔐 **Web 用户认证** | 新增用户名密码登录系统，默认开启，防止未授权访问 |
| 🐳 **多架构 Docker 镜像** | 支持 `amd64` + `arm64` 双平台，镜像内置编译无需本地 JDK |
| 📦 **零配置启动** | 不挂载任何配置文件，`docker run` 即用，默认 `admin/admin123` |
| 🔄 **环境变量改密码** | 通过 `AUTH_USERNAME` / `AUTH_PASSWORD` 环境变量自定义账号密码 |

### 改动的文件

| 文件 | 改动内容 |
|---|---|
| `AppConfig.java` | 新增 `authEnabled` / `authUsername` / `authPassword` 字段 |
| `AppConfigLoader.java` | 加载 `[auth]` 配置段，支持 `-Dauth.username` / `-Dauth.password` 系统属性覆盖 |
| `AuthFilter.java` | **新增** — 拦截未认证 API 请求返回 401 |
| `LoginServlet.java` | **新增** — 处理登录 / 登出 / 认证状态检查 |
| `WebServer.java` | 注册 AuthFilter 和 LoginServlet，启用 SessionHandler |
| `index.html` | 新增登录弹窗 + 认证检查逻辑 + 修改密码 UI |
| `api.js` | 新增认证 API + 统一 401 拦截处理 |
| `bundle/config.ini` | 新增 `[auth]` 配置段，默认 `enabled = 1` |
| `Dockerfile.multiarch` | **新增** — 多阶段多架构构建 |
| `docker-compose.yml` | **新增** — 开箱即用的 compose 文件 |

## 🚀 快速开始

### Docker 一键启动

```bash
docker run -d \
  --name sonovel \
  -p 7765:7765 \
  -e JAVA_OPTS="-Dmode=web" \
  -e AUTH_USERNAME="admin" \
  -e AUTH_PASSWORD="admin123" \
  -v ./downloads:/sonovel/downloads \
  ywsj/sonovel:latest
```

### Docker Compose

```yaml
services:
  sonovel:
    image: ywsj/sonovel:latest
    container_name: sonovel
    ports:
      - "7765:7765"
    environment:
      JAVA_OPTS: "-Dmode=web"
      AUTH_USERNAME: "admin"      # 修改用户名
      AUTH_PASSWORD: "admin123"   # 修改密码
    volumes:
      - ./downloads:/sonovel/downloads
    restart: unless-stopped
```

```bash
docker compose up -d
```

浏览器访问 `http://你的IP:7765`，输入用户名密码登录即可。

### 环境变量说明

| 变量 | 说明 | 默认值 |
|---|---|---|
| `JAVA_OPTS` | JVM 启动参数 | `-Dmode=web` |
| `AUTH_USERNAME` | 登录用户名 | `admin` |
| `AUTH_PASSWORD` | 登录密码 | `admin123` |

> 修改密码只需修改环境变量后重启容器，无需挂载配置文件。

## 📖 原项目功能

So Novel 是一款通用的网页内容处理与导出工具，支持从多个书源搜索、下载网络小说，导出为 EPUB / TXT / HTML / PDF 等格式。

更多功能说明请参考 [原项目 README](https://github.com/freeok/so-novel)。

## 📄 许可证

MIT License — 遵循原项目许可证。

## 🙏 致谢

- 原项目：[freeok/so-novel](https://github.com/freeok/so-novel)
- 原作者：pcdd / freeok

# 智衣 ERP 线上部署文档

本文档面向生产部署，覆盖后端服务、Web 管理端、微信小程序三端。后端服务采用 Docker 方式运行，Web 端由 Nginx 容器托管，小程序构建后通过微信开发者工具上传。

## 1. 部署架构

线上推荐部署形态：

```text
用户浏览器 / 微信小程序
        |
        | HTTPS，建议由 CDN、负载均衡或主机 Nginx 终止 TLS
        v
Nginx 容器
  - /                 Web 管理端静态资源
  - /api/             反向代理到后端
  - /swagger-ui/      反向代理到后端 API 文档
        |
        v
Spring Boot 后端容器
        |
        +-- MySQL 8 容器
        +-- Redis 7 容器
```

本项目按单个 Git 仓库部署，仓库根目录同时包含 `zhiyi-server`、`zhiyi-web`、`zhiyi-mini`、`sql`、`deploy` 和 `scripts`。仓库内新增的生产部署文件：

```text
deploy/
├── docker-compose.prod.yml       # 生产 Docker Compose
├── env.example                   # 生产环境变量模板
└── nginx/
    └── default.conf.template     # Nginx 容器模板配置

scripts/
└── deploy.sh                     # 一键构建与部署脚本
```

根目录现有 `docker-compose.yml` 可继续作为本地或旧部署参考；生产一键部署使用 `deploy/docker-compose.prod.yml`。

## 2. 服务器要求

推荐配置：

| 项目 | 建议 |
| --- | --- |
| 操作系统 | Ubuntu 22.04 LTS / Debian 12 / CentOS Stream 9 |
| CPU | 2 核起 |
| 内存 | 4 GB 起 |
| 磁盘 | 50 GB SSD 起 |
| Docker | Docker Engine 24+ |
| Docker Compose | Compose V2 |
| Node.js | 20 LTS，至少 18+ |
| npm | 9+ |
| JDK | 17，脚本通过 Maven Wrapper 构建后端 |

服务器需要开放的端口：

| 端口 | 用途 |
| --- | --- |
| 80 | Web/API HTTP 入口，或给外层 TLS 代理回源 |
| 443 | 如在主机层或外部网关启用 HTTPS |

生产 Compose 默认不暴露 MySQL `3306` 和 Redis `6379` 到公网，只在 Docker 内部网络访问。

## 3. 一键部署

### 3.1 首次部署

在服务器上拉取代码后执行：

```bash
cd /opt/zhiyi_erp
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

脚本会执行：

1. 如果 `deploy/.env` 不存在，自动生成一份带随机密码的环境文件。
2. 执行 `zhiyi-web` 的 `npm ci && npm run build`。
3. 执行 `zhiyi-server` 的 `./mvnw -q -DskipTests clean package`。
4. 如果 `BUILD_MINI=true`，执行小程序 Taro 构建。
5. 执行 `docker compose up -d --build`，启动 MySQL、Redis、后端、Nginx。

首次运行后请立即检查并按线上域名调整：

```bash
vim deploy/.env
./scripts/deploy.sh
```

关键变量：

```env
SERVER_NAME=erp.example.com
HTTP_PORT=80

MYSQL_ROOT_PASSWORD=请改成强密码
MYSQL_DATABASE=zhiyi_erp
MYSQL_USER=zhiyi
MYSQL_PASSWORD=请改成强密码

REDIS_PASSWORD=请改成强密码

JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

BUILD_MINI=true
MINI_API_BASE_URL=https://erp.example.com/api
```

### 3.2 常用脚本命令

```bash
# 完整构建并部署，默认命令
./scripts/deploy.sh deploy

# 只构建三端，不启动容器
./scripts/deploy.sh build

# 只构建微信小程序
./scripts/deploy.sh mini

# 启动或重建 Docker 服务
./scripts/deploy.sh up

# 停止 Docker 服务，不删除数据卷
./scripts/deploy.sh down

# 查看容器状态
./scripts/deploy.sh ps

# 查看后端日志
./scripts/deploy.sh logs backend

# 查看 Nginx 日志
./scripts/deploy.sh logs nginx

# 备份 MySQL 到 deploy/backups/
./scripts/deploy.sh backup
```

## 4. 后端服务部署

### 4.1 构建方式

后端位于 `zhiyi-server`，生产部署由脚本自动构建：

```bash
cd zhiyi-server
./mvnw -q -DskipTests clean package
```

产物：

```text
zhiyi-server/target/zhiyi-server-1.0.0.jar
```

### 4.2 Docker 运行方式

后端镜像使用 `zhiyi-server/Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/zhiyi-server-1.0.0.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
```

Compose 中的后端服务会注入生产环境变量：

| 变量 | 说明 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | 启用 `application-prod.yml` |
| `MYSQL_HOST=mysql` | Docker 内部 MySQL 服务名 |
| `MYSQL_PORT=3306` | MySQL 端口 |
| `MYSQL_DB=zhiyi_erp` | 数据库名 |
| `MYSQL_USER=zhiyi` | 数据库账号 |
| `MYSQL_PASSWORD` | 数据库密码 |
| `REDIS_HOST=redis` | Docker 内部 Redis 服务名 |
| `REDIS_PORT=6379` | Redis 端口 |
| `REDIS_PASSWORD` | Redis 密码 |
| `JAVA_OPTS` | JVM 参数 |

生产配置文件 `zhiyi-server/src/main/resources/application-prod.yml` 已支持以上变量。

### 4.3 数据库初始化

MySQL 容器首次创建数据卷时会自动执行：

```text
sql/init.sql
```

默认账号来自初始化 SQL。上线后需要立即修改默认管理员密码。

如果数据卷已经存在，MySQL 不会重复执行初始化脚本。需要重置数据库时必须先确认备份，然后删除 Compose 数据卷：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml down -v
```

这是破坏性操作，生产环境不要直接执行。

### 4.4 后端验证

查看容器：

```bash
./scripts/deploy.sh ps
```

查看日志：

```bash
./scripts/deploy.sh logs backend
```

通过 Nginx 验证 API 文档：

```bash
curl -I http://你的域名/v3/api-docs
```

如生产环境不希望暴露 Swagger，可在 Nginx 模板中移除 `/swagger-ui/` 和 `/v3/api-docs` 两个 location，或在后端生产配置中关闭 springdoc。

## 5. Web 管理端部署

### 5.1 构建方式

Web 端位于 `zhiyi-web`：

```bash
cd zhiyi-web
npm ci
npm run build
```

产物：

```text
zhiyi-web/dist/
```

一键脚本会自动构建，并通过 Nginx 容器挂载：

```yaml
../zhiyi-web/dist:/usr/share/nginx/html:ro
```

### 5.2 API 访问方式

Web 请求封装在 `zhiyi-web/src/utils/request.ts`，生产环境使用相对路径：

```ts
baseURL: '/api'
```

因此 Web 和后端必须部署在同一个域名下，由 Nginx 将 `/api/` 转发到后端容器。当前模板已经配置：

```nginx
location /api/ {
    proxy_pass http://backend:8080/api/;
}
```

### 5.3 Web 验证

```bash
curl -I http://你的域名/
curl -I http://你的域名/assets/
```

浏览器访问：

```text
http://你的域名/
```

如果页面能打开但接口失败，优先检查：

```bash
./scripts/deploy.sh logs nginx
./scripts/deploy.sh logs backend
```

## 6. 微信小程序部署

### 6.1 线上域名要求

微信小程序生产环境必须使用 HTTPS 域名。需要在微信公众平台配置：

| 类型 | 域名 |
| --- | --- |
| request 合法域名 | `https://erp.example.com` |
| uploadFile 合法域名 | 如后续需要上传文件，同域名配置 |
| downloadFile 合法域名 | 如后续需要下载文件，同域名配置 |

`deploy/.env` 中的小程序 API 地址必须是完整 `/api` 前缀：

```env
MINI_API_BASE_URL=https://erp.example.com/api
```

### 6.2 构建方式

小程序位于 `zhiyi-mini`：

```bash
cd zhiyi-mini
npm ci
TARO_APP_API_BASE_URL=https://erp.example.com/api ./node_modules/.bin/taro build --type weapp --no-check
```

产物：

```text
zhiyi-mini/dist/
```

说明：

- `TARO_APP_API_BASE_URL` 会在构建时写入小程序请求层。
- 当前本机环境直接运行 `npm run build:weapp` 可能触发 Taro 构建前检查卡住，脚本统一使用 `--no-check`。
- 如果不想在服务器上构建小程序，可以在 `deploy/.env` 设置 `BUILD_MINI=false`。

### 6.3 上传发布

1. 打开微信开发者工具。
2. 导入 `zhiyi-mini` 目录。
3. 确认 `project.config.json` 中 `miniprogramRoot` 为 `dist/`。
4. 确认 AppID 为线上小程序 AppID。
5. 点击上传，填写版本号和版本说明。
6. 在微信公众平台提交审核并发布。

小程序不是普通 Docker 服务，脚本只负责生成可上传的 `dist/`，审核发布仍通过微信官方流程完成。

## 7. HTTPS 部署建议

生产建议使用以下任一方式提供 HTTPS：

1. 云厂商负载均衡、CDN 或 API 网关终止 TLS，然后回源到服务器 `HTTP_PORT=80`。
2. 在宿主机安装 Nginx 和 Certbot，宿主机处理 443，再反代到 Docker Nginx 的 80。
3. 将 Docker Nginx 扩展为 443 配置并挂载证书。

当前仓库的一键脚本默认只启用容器内 HTTP，因为证书来源、续期方式和云厂商差异较大。微信小程序上线时，外部访问域名必须已经是 HTTPS。

宿主机 Nginx 反代示例：

```nginx
server {
    listen 443 ssl http2;
    server_name erp.example.com;

    ssl_certificate /etc/letsencrypt/live/erp.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/erp.example.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

## 8. 更新、回滚与备份

### 8.1 更新流程

```bash
cd /opt/zhiyi_erp
git pull
./scripts/deploy.sh
```

脚本会重新构建 Web、后端镜像、小程序产物，并滚动替换容器。数据库数据卷不会被删除。

### 8.2 回滚代码版本

```bash
cd /opt/zhiyi_erp
git log --oneline
git checkout <上一版提交>
./scripts/deploy.sh
```

如果涉及数据库结构变更，回滚前必须确认 SQL 是否兼容。

### 8.3 MySQL 备份

手动备份：

```bash
./scripts/deploy.sh backup
```

备份文件位置：

```text
deploy/backups/zhiyi_erp_YYYYMMDD_HHMMSS.sql.gz
```

恢复示例：

```bash
gunzip -c deploy/backups/zhiyi_erp_20260702_120000.sql.gz \
  | docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml exec -T mysql \
      sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
```

## 9. 常见问题

### 9.1 容器启动失败

```bash
./scripts/deploy.sh ps
./scripts/deploy.sh logs backend
./scripts/deploy.sh logs mysql
./scripts/deploy.sh logs redis
```

重点检查 `deploy/.env` 中 MySQL 和 Redis 密码是否为空，后端是否连接到了 `mysql`、`redis` 这两个 Docker 服务名。

### 9.2 Web 页面能打开但接口 404

检查 Nginx 代理：

```bash
./scripts/deploy.sh logs nginx
curl -I http://你的域名/api/
```

Web 端只能请求 `/api`，不要把前端构建成直连后端 IP。

### 9.3 小程序请求失败

逐项检查：

1. `MINI_API_BASE_URL` 是否是 `https://域名/api`。
2. 微信公众平台是否配置 request 合法域名。
3. 域名证书是否有效，且公网可以访问。
4. 小程序是否重新构建并重新上传。

### 9.4 `npm run build:weapp` 卡住

使用脚本或手动执行：

```bash
TARO_APP_API_BASE_URL=https://erp.example.com/api ./node_modules/.bin/taro build --type weapp --no-check
```

### 9.5 数据库没有初始化

MySQL 官方镜像只会在空数据目录首次启动时执行 `/docker-entrypoint-initdb.d/`。如果数据卷已经存在，修改 `sql/init.sql` 不会自动重放。

查看数据卷：

```bash
docker volume ls | grep zhiyi
```

生产环境需要通过迁移 SQL 显式升级，不要删除数据卷。

# 智衣ERP系统部署文档

本文档详细说明智衣ERP系统的部署流程，涵盖后端服务、Web前端、微信小程序三个子系统。

---

## 目录

1. [环境要求](#1-环境要求)
2. [数据库初始化](#2-数据库初始化)
3. [后端部署](#3-后端部署)
4. [Web前端部署](#4-web前端部署)
5. [Docker Compose部署](#5-docker-compose部署)
6. [微信小程序部署](#6-微信小程序部署)
7. [运维说明](#7-运维说明)

---

## 1. 环境要求

### 基础环境

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17 | 后端运行环境 |
| Maven | 3.8+ | 后端构建工具 |
| MySQL | 8.0 | 主数据库 |
| Redis | 7 | 缓存与会话存储 |
| Node.js | 18+ | 前端构建环境 |
| npm | 9+ | 前端包管理器 |
| Nginx | 1.24+ | Web前端反向代理 |

### 小程序开发环境

| 组件 | 说明 |
|------|------|
| 微信开发者工具 | 小程序上传与调试 |
| Taro CLI 4.2.0 | 小程序构建框架 |

### 服务器推荐配置

- CPU: 2核+
- 内存: 4GB+
- 磁盘: 50GB+ SSD
- 操作系统: Ubuntu 22.04 LTS / CentOS Stream 9

---

## 2. 数据库初始化

### 2.1 安装MySQL 8.0

**Ubuntu/Debian:**

```bash
sudo apt update
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
sudo mysql_secure_installation
```

**CentOS/RHEL:**

```bash
sudo dnf install -y mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld
sudo mysql_secure_installation
```

**Docker方式:**

```bash
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=your_strong_password \
  -v /data/mysql:/var/lib/mysql \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

### 2.2 MySQL配置

编辑 `/etc/mysql/mysql.conf.d/mysqld.cnf`（Ubuntu）或 `/etc/my.cnf`（CentOS）：

```ini
[mysqld]
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
default-time-zone = '+08:00'
max_connections = 200
innodb_buffer_pool_size = 1G
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
```

重启MySQL使配置生效：

```bash
sudo systemctl restart mysql
```

### 2.3 创建数据库并导入数据

项目SQL初始化文件位于 `sql/init.sql`，执行以下命令完成初始化：

```bash
mysql -u root -p < sql/init.sql
```

该脚本会自动完成以下操作：
- 创建 `zhiyi_erp` 数据库（字符集 utf8mb4）
- 创建所有业务表（factory、employee、role、order等）
- 插入默认初始数据

**默认管理员账号：**

| 字段 | 值 |
|------|-----|
| 登录账号 | `admin` |
| 登录密码 | `admin123` |
| 员工工号 | `A001` |
| 登录码 | `ADMIN001`（移动端使用） |

> 生产环境部署后请立即修改默认密码。

---

## 3. 后端部署

### 3.1 项目结构

```
zhiyi-server/
├── pom.xml                              # Maven配置
├── mvnw                                 # Maven Wrapper
└── src/main/
    ├── java/com/zhiyi/erp/             # 源代码
    │   ├── module/                      # 业务模块
    │   │   ├── admin/                   # 管理后台模块
    │   │   ├── order/                   # 订单模块
    │   │   ├── cutting/                 # 裁剪模块
    │   │   ├── finance/                 # 财务模块
    │   │   └── system/                  # 系统模块
    │   └── common/                      # 公共组件
    └── resources/
        ├── application.yml              # 主配置
        ├── application-dev.yml          # 开发环境配置
        └── application-prod.yml         # 生产环境配置
```

### 3.2 构建

```bash
cd zhiyi-server
./mvnw clean package -DskipTests
```

构建产物位于 `target/zhiyi-server-1.0.0.jar`。

### 3.3 生产环境配置

编辑 `src/main/resources/application-prod.yml`，根据实际环境修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zhiyi_erp?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: zhiyi
    password: your_strong_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
      database: 0

logging:
  level:
    com.zhiyi.erp: info

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

**环境变量方式配置（推荐）：**

生产环境配置已支持环境变量注入，可通过以下变量覆盖默认值：

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `MYSQL_HOST` | mysql | MySQL主机地址 |
| `MYSQL_PORT` | 3306 | MySQL端口 |
| `MYSQL_DB` | zhiyi_erp | 数据库名称 |
| `MYSQL_USER` | root | 数据库用户名 |
| `MYSQL_PASSWORD` | root | 数据库密码 |
| `REDIS_HOST` | redis | Redis主机地址 |
| `REDIS_PORT` | 6379 | Redis端口 |
| `REDIS_PASSWORD` | （空） | Redis密码 |

### 3.4 运行

**直接运行：**

```bash
java -jar target/zhiyi-server-1.0.0.jar --spring.profiles.active=prod
```

**指定JVM参数运行：**

```bash
java -Xms512m -Xmx1024m -XX:+UseG1GC \
  -jar target/zhiyi-server-1.0.0.jar \
  --spring.profiles.active=prod
```

**环境变量方式运行：**

```bash
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export MYSQL_DB=zhiyi_erp
export MYSQL_USER=zhiyi
export MYSQL_PASSWORD=your_strong_password
export REDIS_HOST=127.0.0.1
export REDIS_PASSWORD=your_redis_password

java -jar target/zhiyi-server-1.0.0.jar --spring.profiles.active=prod
```

### 3.5 Systemd服务配置

创建服务文件 `/etc/systemd/system/zhiyi-server.service`：

```ini
[Unit]
Description=Zhiyi ERP Backend Service
After=syslog.target network.target mysql.service redis.service

[Service]
Type=simple
User=zhiyi
Group=zhiyi
WorkingDirectory=/opt/zhiyi
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -XX:+UseG1GC -jar /opt/zhiyi/zhiyi-server-1.0.0.jar --spring.profiles.active=prod
Environment=MYSQL_HOST=127.0.0.1
Environment=MYSQL_PORT=3306
Environment=MYSQL_DB=zhiyi_erp
Environment=MYSQL_USER=zhiyi
Environment=MYSQL_PASSWORD=your_strong_password
Environment=REDIS_HOST=127.0.0.1
Environment=REDIS_PASSWORD=your_redis_password
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用并启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable zhiyi-server
sudo systemctl start zhiyi-server
sudo systemctl status zhiyi-server
```

查看日志：

```bash
sudo journalctl -u zhiyi-server -f
```

---

## 4. Web前端部署

### 4.1 项目结构

```
zhiyi-web/
├── package.json          # 依赖配置
├── vite.config.ts        # Vite构建配置
├── tsconfig.json         # TypeScript配置
├── index.html            # 入口HTML
└── src/
    ├── api/              # API接口定义
    ├── router/           # 路由配置
    ├── utils/request.ts  # Axios请求封装
    └── views/            # 页面组件
```

### 4.2 构建

```bash
cd zhiyi-web
npm install
npm run build
```

构建产物位于 `dist/` 目录。

> 构建脚本会先执行 `vue-tsc -b` 进行类型检查，再执行 `vite build`。如需跳过类型检查，可直接运行 `vite build`。

### 4.3 Nginx配置

创建Nginx配置文件 `/etc/nginx/conf.d/zhiyi-web.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 重定向到HTTPS
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL证书配置
    ssl_certificate /etc/nginx/ssl/your-domain.com.pem;
    ssl_certificate_key /etc/nginx/ssl/your-domain.com.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # 安全头
    add_header X-Frame-Options SAMEORIGIN;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";

    # 前端静态文件
    root /var/www/zhiyi-web;
    index index.html;

    # SPA路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript image/svg+xml;
}
```

### 4.4 部署静态文件

```bash
# 创建部署目录
sudo mkdir -p /var/www/zhiyi-web

# 复制构建产物
sudo cp -r dist/* /var/www/zhiyi-web/

# 设置权限
sudo chown -R www-data:www-data /var/www/zhiyi-web

# 测试并重载Nginx
sudo nginx -t
sudo systemctl reload nginx
```

### 4.5 SSL证书配置（Let's Encrypt）

**安装Certbot：**

```bash
sudo apt install -y certbot python3-certbot-nginx
```

**申请证书：**

```bash
sudo certbot --nginx -d your-domain.com
```

**自动续期：**

Certbot会自动配置续期定时任务。手动测试续期：

```bash
sudo certbot renew --dry-run
```

---

## 5. Docker Compose部署

### 5.1 项目目录结构

```
deploy/
├── docker-compose.yml
├── .env
├── nginx/
│   └── conf.d/
│       └── default.conf
├── mysql/
│   └── init/
│       └── init.sql
└── backend/
    └── Dockerfile
```

### 5.2 环境变量文件

创建 `deploy/.env`：

```env
# MySQL
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=zhiyi_erp
MYSQL_USER=zhiyi
MYSQL_PASSWORD=your_db_password

# Redis
REDIS_PASSWORD=your_redis_password

# 后端
SPRING_PROFILES_ACTIVE=prod
```

### 5.3 后端Dockerfile

创建 `deploy/backend/Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="zhiyi-erp"

WORKDIR /app

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 复制jar包
COPY zhiyi-server-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
```

构建后端镜像：

```bash
cd zhiyi-server
./mvnw clean package -DskipTests
cp target/zhiyi-server-1.0.0.jar ../deploy/backend/
cd ../deploy/backend
docker build -t zhiyi-server:1.0.0 .
```

### 5.4 Nginx配置

创建 `deploy/nginx/conf.d/default.conf`：

```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态文件
    root /usr/share/nginx/html;
    index index.html;

    # SPA路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API反向代理
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_read_timeout 60s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
}
```

### 5.5 Docker Compose文件

创建 `deploy/docker-compose.yml`：

```yaml
version: '3.8'

services:
  # MySQL数据库
  mysql:
    image: mysql:8.0
    container_name: zhiyi-mysql
    restart: always
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: Asia/Shanghai
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql/init:/docker-entrypoint-initdb.d
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+08:00
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - zhiyi-net

  # Redis缓存
  redis:
    image: redis:7-alpine
    container_name: zhiyi-redis
    restart: always
    ports:
      - "6379:6379"
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - zhiyi-net

  # 后端服务
  backend:
    image: zhiyi-server:1.0.0
    container_name: zhiyi-server
    restart: always
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DB: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - zhiyi-net

  # Nginx前端
  nginx:
    image: nginx:1.24-alpine
    container_name: zhiyi-nginx
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ../zhiyi-web/dist:/usr/share/nginx/html:ro
      # 如需SSL，取消注释以下行
      # - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - backend
    networks:
      - zhiyi-net

volumes:
  mysql_data:
    driver: local
  redis_data:
    driver: local

networks:
  zhiyi-net:
    driver: bridge
```

### 5.6 启动服务

```bash
cd deploy

# 复制SQL初始化文件
cp ../sql/init.sql ./mysql/init/

# 启动所有服务
docker compose up -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f backend
```

### 5.7 常用Docker命令

```bash
# 停止所有服务
docker compose down

# 停止并删除数据卷（慎用）
docker compose down -v

# 重新构建并启动后端
docker compose up -d --build backend

# 进入后端容器
docker compose exec backend sh

# 查看MySQL数据
docker compose exec mysql mysql -u zhiyi -p zhiyi_erp
```

---

## 6. 微信小程序部署

### 6.1 项目结构

```
zhiyi-mini/
├── project.config.json    # 小程序项目配置
├── package.json           # npm依赖
├── config/                # Taro构建配置
│   └── index.ts
└── src/                   # 源代码
    ├── app.vue            # 应用入口
    ├── pages/             # 页面
    └── utils/             # 工具函数
```

### 6.2 配置小程序AppID

编辑 `project.config.json`，将 `appid` 修改为实际的小程序AppID：

```json
{
  "miniprogramRoot": "dist/",
  "projectname": "zhiyi-mini",
  "description": "智衣ERP微信小程序",
  "appid": "your_actual_appid",
  "setting": {
    "urlCheck": true,
    "es6": false,
    "enhance": false,
    "compileHotReLoad": false,
    "postcss": false,
    "minified": false
  },
  "compileType": "miniprogram"
}
```

> AppID可在 [微信公众平台](https://mp.weixin.qq.com/) -> 开发管理 -> 开发设置 中获取。

### 6.3 配置服务器域名

在微信公众平台配置以下服务器域名：

| 类型 | 域名 |
|------|------|
| request合法域名 | `https://your-domain.com` |
| uploadFile合法域名 | `https://your-domain.com` |
| downloadFile合法域名 | `https://your-domain.com` |

> 域名必须已完成ICP备案，且支持HTTPS。

### 6.4 构建

```bash
cd zhiyi-mini
npm install
npm run build:weapp
```

构建产物位于 `dist/` 目录。

### 6.5 上传与发布

1. 打开微信开发者工具
2. 导入项目，选择 `zhiyi-mini` 目录
3. 确认 `dist/` 目录为小程序根目录
4. 点击"上传"按钮，填写版本号和描述
5. 在微信公众平台提交审核并发布

### 6.6 开发调试

```bash
# 开发模式（监听文件变化）
npm run dev:weapp
```

在微信开发者工具中预览和调试。

---

## 7. 运维说明

### 7.1 日志配置

**后端日志：**

后端使用Spring Boot默认日志框架。生产环境日志级别为 `info`，可在 `application-prod.yml` 中调整：

```yaml
logging:
  level:
    com.zhiyi.erp: info
    # 调试时可临时调整为 debug
    # com.zhiyi.erp: debug
```

**日志文件输出（推荐）：**

如需将日志输出到文件，在 `application-prod.yml` 中添加：

```yaml
logging:
  file:
    name: /var/log/zhiyi/zhiyi-server.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
```

**Nginx日志：**

访问日志：`/var/log/nginx/access.log`
错误日志：`/var/log/nginx/error.log`

### 7.2 数据库备份

**手动备份：**

```bash
mysqldump -u zhiyi -p zhiyi_erp > backup/zhiyi_erp_$(date +%Y%m%d_%H%M%S).sql
```

**自动备份脚本：**

创建 `/opt/zhiyi/scripts/backup.sh`：

```bash
#!/bin/bash
BACKUP_DIR="/data/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
KEEP_DAYS=7

mkdir -p ${BACKUP_DIR}

# 执行备份
mysqldump -u zhiyi -p'your_password' zhiyi_erp | gzip > ${BACKUP_DIR}/zhiyi_erp_${DATE}.sql.gz

# 删除过期备份
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +${KEEP_DAYS} -delete

echo "[$(date)] Backup completed: zhiyi_erp_${DATE}.sql.gz"
```

**配置定时任务：**

```bash
chmod +x /opt/zhiyi/scripts/backup.sh

# 每天凌晨2点执行备份
crontab -e
0 2 * * * /opt/zhiyi/scripts/backup.sh >> /var/log/zhiyi/backup.log 2>&1
```

**恢复备份：**

```bash
gunzip < backup/zhiyi_erp_20260530_020000.sql.gz | mysql -u zhiyi -p zhiyi_erp
```

### 7.3 监控端点

后端集成了Spring Boot Actuator，可使用以下端点进行监控：

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 健康检查 |
| `/actuator/info` | 应用信息 |
| `/actuator/metrics` | 指标数据 |

**API文档端点（开发环境）：**

| 端点 | 说明 |
|------|------|
| `/swagger-ui.html` | Swagger UI |
| `/v3/api-docs` | OpenAPI JSON |

> 生产环境建议关闭Swagger，可在 `application-prod.yml` 中设置 `springdoc.api-docs.enabled: false`。

### 7.4 常见问题排查

**后端无法启动：**

```bash
# 检查端口占用
lsof -i :8080

# 检查MySQL连接
mysql -u zhiyi -p -h 127.0.0.1 zhiyi_erp

# 检查Redis连接
redis-cli -a your_password ping

# 查看详细错误日志
journalctl -u zhiyi-server --no-pager -n 100
```

**前端页面空白：**

```bash
# 检查Nginx配置
sudo nginx -t

# 检查静态文件是否存在
ls -la /var/www/zhiyi-web/

# 检查Nginx错误日志
tail -f /var/log/nginx/error.log
```

**API请求404：**

```bash
# 确认后端服务运行中
curl http://localhost:8080/actuator/health

# 检查Nginx代理配置
curl -v https://your-domain.com/api/actuator/health
```

**小程序无法连接服务器：**

1. 确认已在微信公众平台配置request合法域名
2. 确认域名已备案且HTTPS证书有效
3. 在微信开发者工具中关闭"不校验合法域名"进行调试

**数据库连接池耗尽：**

```bash
# 查看当前连接数
mysql -u root -p -e "SHOW STATUS LIKE 'Threads_connected';"

# 查看最大连接数
mysql -u root -p -e "SHOW VARIABLES LIKE 'max_connections';"

# 调整连接数
mysql -u root -p -e "SET GLOBAL max_connections = 300;"
```

### 7.5 性能优化建议

**JVM调优：**

```bash
# 服务器内存4GB推荐配置
java -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/zhiyi/ \
  -jar zhiyi-server-1.0.0.jar --spring.profiles.active=prod
```

**MySQL优化：**

```ini
# /etc/mysql/mysql.conf.d/mysqld.cnf
innodb_buffer_pool_size = 2G        # 物理内存的50-70%
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2
query_cache_type = 0                # MySQL 8.0已移除查询缓存
```

**Redis优化：**

```conf
# /etc/redis/redis.conf
maxmemory 512mb
maxmemory-policy allkeys-lru
```

**Nginx优化：**

```nginx
# 在http块中添加
worker_processes auto;
worker_connections 1024;
keepalive_timeout 65;
client_max_body_size 10m;
```

### 7.6 版本更新流程

**后端更新：**

```bash
# 1. 拉取最新代码
cd zhiyi-server
git pull

# 2. 构建
./mvnw clean package -DskipTests

# 3. 停止服务
sudo systemctl stop zhiyi-server

# 4. 备份旧版本
cp /opt/zhiyi/zhiyi-server-1.0.0.jar /opt/zhiyi/backup/

# 5. 部署新版本
cp target/zhiyi-server-1.0.0.jar /opt/zhiyi/

# 6. 启动服务
sudo systemctl start zhiyi-server

# 7. 验证
curl http://localhost:8080/actuator/health
```

**前端更新：**

```bash
# 1. 拉取最新代码
cd zhiyi-web
git pull

# 2. 构建
npm install
npm run build

# 3. 部署
sudo cp -r dist/* /var/www/zhiyi-web/

# 4. 清除Nginx缓存（如有）
sudo nginx -s reload
```

**Docker环境更新：**

```bash
cd deploy

# 拉取最新代码并重新构建
cd ../zhiyi-server && ./mvnw clean package -DskipTests && cd ../deploy
cp ../zhiyi-server/target/zhiyi-server-1.0.0.jar ./backend/
docker compose up -d --build backend

# 更新前端
cp -r ../zhiyi-web/dist/* ./nginx/html/
docker compose restart nginx
```

---

## 附录：默认端口清单

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端API | 8080 | Spring Boot |
| Web前端 | 80/443 | Nginx |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Webpack Dev Server | 3000 | 前端开发服务器 |

---

*文档版本：1.0.0*
*最后更新：2026-05-30*

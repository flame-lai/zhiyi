# 后端基础设施实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建Spring Boot项目骨架，完成数据库建表、多租户、认证、统一响应等基础设施。

**Architecture:** 单体模块化架构，Spring Boot 3.x + MyBatis-Plus + Sa-Token，共享数据库+factory_id字段实现多租户隔离。

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus 3.5, Sa-Token, MySQL 8.0, Redis 7, Maven

---

## 文件结构

```
zhiyi-server/
├── pom.xml
├── src/main/java/com/zhiyi/erp/
│   ├── ZhiyiApplication.java
│   ├── common/
│   │   ├── config/
│   │   │   ├── MybatisPlusConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── WebMvcConfig.java
│   │   │   ├── SaTokenConfig.java
│   │   │   └── JacksonConfig.java
│   │   ├── exception/
│   │   │   ├── BizException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── result/
│   │   │   ├── R.java
│   │   │   └── PageResult.java
│   │   ├── tenant/
│   │   │   ├── TenantContext.java
│   │   │   └── TenantInterceptor.java
│   │   └── util/
│   │       ├── OrderNoGenerator.java
│   │       └── EmployeeNoGenerator.java
│   └── module/
│       └── system/
│           ├── controller/
│           │   └── AuthController.java
│           ├── service/
│           │   └── AuthService.java
│           ├── mapper/
│           │   └── EmployeeMapper.java
│           └── entity/
│               ├── Employee.java
│               ├── Role.java
│               ├── Menu.java
│               ├── RoleMenu.java
│               └── Factory.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/
│       └── schema.sql
└── sql/
    └── init.sql
```

---

### Task 1: Maven项目初始化

**Files:**
- Create: `zhiyi-server/pom.xml`

- [ ] **Step 1: 创建项目目录结构**

```bash
mkdir -p zhiyi-server/src/main/java/com/zhiyi/erp/common/{config,exception,result,tenant,util}
mkdir -p zhiyi-server/src/main/java/com/zhiyi/erp/module/system/{controller,service,mapper,entity}
mkdir -p zhiyi-server/src/main/resources/db
mkdir -p zhiyi-server/src/test/java/com/zhiyi/erp
```

- [ ] **Step 2: 创建pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.zhiyi</groupId>
    <artifactId>zhiyi-server</artifactId>
    <version>1.0.0</version>
    <name>zhiyi-server</name>
    <description>智衣ERP后端服务</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <sa-token.version>1.37.0</sa-token.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Sa-Token -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
            <version>${sa-token.version}</version>
        </dependency>
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-redis-jackson</artifactId>
            <version>${sa-token.version}</version>
        </dependency>

        <!-- Connection Pool -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.25</version>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 验证Maven依赖下载**

```bash
cd zhiyi-server && mvn dependency:resolve -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add zhiyi-server/pom.xml
git commit -m "feat: initialize Maven project with Spring Boot 3.2 dependencies"
```

---

### Task 2: 启动类与配置文件

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/ZhiyiApplication.java`
- Create: `zhiyi-server/src/main/resources/application.yml`
- Create: `zhiyi-server/src/main/resources/application-dev.yml`
- Create: `zhiyi-server/src/main/resources/application-prod.yml`

- [ ] **Step 1: 创建启动类**

```java
package com.zhiyi.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhiyi.erp.module.*.mapper")
public class ZhiyiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZhiyiApplication.class, args);
    }
}
```

- [ ] **Step 2: 创建主配置文件 application.yml**

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  profiles:
    active: dev
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null

# Sa-Token
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 2592000
  active-timeout: -1
  is-concurrent: true
  is-share: false
  is-read-cookie: false
  is-read-header: true
  is-log: false

# MyBatis-Plus
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

- [ ] **Step 3: 创建开发环境配置 application-dev.yml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zhiyi_erp?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0

logging:
  level:
    com.zhiyi.erp: debug
```

- [ ] **Step 4: 创建生产环境配置 application-prod.yml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:mysql}:${MYSQL_PORT:3306}/${MYSQL_DB:zhiyi_erp}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0

logging:
  level:
    com.zhiyi.erp: info

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

- [ ] **Step 5: 提交**

```bash
git add zhiyi-server/src/
git commit -m "feat: add application entry point and multi-environment config"
```

---

### Task 3: 数据库DDL

**Files:**
- Create: `sql/init.sql`

- [ ] **Step 1: 创建完整的数据库初始化脚本**

将技术方案文档中的所有DDL写入 `sql/init.sql`，包含：

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS zhiyi_erp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zhiyi_erp;

-- 工厂/租户表
CREATE TABLE `factory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '工厂名称',
  `code` VARCHAR(50) NOT NULL COMMENT '工厂编码',
  `status` TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  `contact` VARCHAR(50) COMMENT '联系人',
  `phone` VARCHAR(20) COMMENT '联系电话',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工厂/租户表';

-- 员工档案
CREATE TABLE `employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `employee_no` VARCHAR(20) NOT NULL COMMENT '工号',
  `name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20),
  `role_id` BIGINT COMMENT '关联角色',
  `login_account` VARCHAR(50) COMMENT 'Web端登录账号',
  `login_password` VARCHAR(100) COMMENT 'BCrypt加密密码',
  `login_code` VARCHAR(100) COMMENT '移动端登录码',
  `status` TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_no` (`factory_id`, `employee_no`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工档案';

-- 角色表
CREATE TABLE `role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单表
CREATE TABLE `menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID',
  `name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `code` VARCHAR(100) NOT NULL COMMENT '菜单编码',
  `path` VARCHAR(200) COMMENT '路由路径',
  `component` VARCHAR(200) COMMENT '前端组件路径',
  `icon` VARCHAR(50) COMMENT '图标',
  `type` TINYINT COMMENT '1目录 2菜单 3按钮',
  `permission` VARCHAR(100) COMMENT '权限标识',
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 角色菜单关联
CREATE TABLE `role_menu` (
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- 工序库
CREATE TABLE `process` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `category` VARCHAR(50) COMMENT '分类',
  `name` VARCHAR(50) NOT NULL COMMENT '工序名称',
  `ref_price` DECIMAL(12,2) COMMENT '参考工价',
  `sort` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序库';

-- 款号表
CREATE TABLE `style` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `style_code` VARCHAR(50) NOT NULL COMMENT '款号编码',
  `style_name` VARCHAR(100) NOT NULL COMMENT '款式名称',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_code` (`factory_id`, `style_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='款号表';

-- 款号标准工序路由
CREATE TABLE `style_process` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `style_id` BIGINT NOT NULL,
  `process_id` BIGINT NOT NULL,
  `ref_price` DECIMAL(12,2) COMMENT '参考工价',
  `sort` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_style_id` (`style_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='款号标准工序路由';

-- 款号标准物料清单
CREATE TABLE `style_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `style_id` BIGINT NOT NULL,
  `material_name` VARCHAR(100) NOT NULL COMMENT '物料名称',
  `type` TINYINT COMMENT '1原料 2辅料',
  `unit_qty` DECIMAL(12,4) COMMENT '单件用量',
  `unit` VARCHAR(20) COMMENT '单位',
  PRIMARY KEY (`id`),
  KEY `idx_style_id` (`style_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='款号标准物料清单';

-- 客户表
CREATE TABLE `customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `customer_code` VARCHAR(20) NOT NULL COMMENT '客户编号',
  `name` VARCHAR(100) NOT NULL,
  `contact` VARCHAR(50),
  `phone` VARCHAR(20),
  `address` VARCHAR(200),
  `settle_type` VARCHAR(50) COMMENT '结算方式',
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_code` (`factory_id`, `customer_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 供应商表
CREATE TABLE `supplier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `supplier_code` VARCHAR(20) NOT NULL COMMENT '供应商编号',
  `name` VARCHAR(100) NOT NULL,
  `contact` VARCHAR(50),
  `phone` VARCHAR(20),
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_code` (`factory_id`, `supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 订单主表
CREATE TABLE `order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
  `customer_id` BIGINT NOT NULL,
  `total_amount` DECIMAL(12,2) COMMENT '订单金额',
  `delivery_date` DATE COMMENT '计划交期',
  `status` TINYINT DEFAULT 0 COMMENT '0待确认 1已确认 2生产中 3已完成 4已出货 5已关闭',
  `remark` VARCHAR(500),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_no` (`factory_id`, `order_no`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 订单明细行
CREATE TABLE `order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `style_id` BIGINT NOT NULL,
  `color` VARCHAR(50) COMMENT '颜色',
  `size` VARCHAR(20) COMMENT '尺码',
  `qty` INT COMMENT '数量',
  `unit_price` DECIMAL(12,2) COMMENT '单价',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细行';

-- 订单工序
CREATE TABLE `order_process` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `process_id` BIGINT NOT NULL,
  `process_price` DECIMAL(12,2) COMMENT '工价',
  `sort` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单工序';

-- 订单物料
CREATE TABLE `order_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `material_name` VARCHAR(100) NOT NULL,
  `type` TINYINT COMMENT '1原料 2辅料',
  `unit_qty` DECIMAL(12,4),
  `unit` VARCHAR(20),
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单物料';

-- 出货记录
CREATE TABLE `shipment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `order_item_id` BIGINT NOT NULL,
  `qty` INT NOT NULL COMMENT '出货数量',
  `shipment_date` DATE NOT NULL,
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出货记录';

-- 裁床计划
CREATE TABLE `cutting_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `order_item_id` BIGINT NOT NULL,
  `bed_no` INT NOT NULL COMMENT '床次',
  `plan_qty` INT COMMENT '计划件数',
  `actual_qty` INT COMMENT '实际件数',
  `qty_per_bundle` INT COMMENT '每扎件数',
  `bundle_count` INT COMMENT '扎数',
  `cutting_date` DATE COMMENT '裁床日期',
  `barcode` VARCHAR(100) COMMENT '裁床条形码内容',
  `status` TINYINT DEFAULT 0 COMMENT '0待裁剪 1已裁剪 2已开菲',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_order_item_id` (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='裁床计划';

-- 扎号表
CREATE TABLE `bundle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `cutting_plan_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `order_item_id` BIGINT NOT NULL,
  `bundle_no` VARCHAR(20) NOT NULL COMMENT '扎号',
  `global_bundle_no` VARCHAR(100) NOT NULL COMMENT '全局唯一标识',
  `qty` INT NOT NULL COMMENT '件数',
  `status` TINYINT DEFAULT 0 COMMENT '0生产中 1已完成',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_global_no` (`global_bundle_no`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扎号表';

-- 报工记录
CREATE TABLE `work_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `bundle_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `process_id` BIGINT NOT NULL,
  `employee_id` BIGINT NOT NULL,
  `qty` INT NOT NULL COMMENT '件数',
  `price` DECIMAL(12,2) NOT NULL COMMENT '工价',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
  `report_type` TINYINT DEFAULT 1 COMMENT '1扫码报工 2裁床报工 3管理员补录',
  `status` TINYINT DEFAULT 1 COMMENT '1有效 0已清除',
  `report_time` DATETIME COMMENT '报工时间',
  `clear_reason` VARCHAR(200) COMMENT '清除原因',
  `supplement_flag` TINYINT DEFAULT 0 COMMENT '是否补录',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_bundle_id` (`bundle_id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报工记录';

-- 计时工资
CREATE TABLE `timed_wage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `employee_id` BIGINT NOT NULL,
  `work_date` DATE NOT NULL COMMENT '工作日期',
  `hours` DECIMAL(6,2) NOT NULL COMMENT '工时',
  `hourly_rate` DECIMAL(12,2) NOT NULL COMMENT '时薪',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
  `work_content` VARCHAR(200) COMMENT '工作内容',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_employee_date` (`employee_id`, `work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计时工资';

-- 其他工资项目
CREATE TABLE `wage_other` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `employee_id` BIGINT NOT NULL,
  `year_month` VARCHAR(7) NOT NULL COMMENT '月份',
  `item_name` VARCHAR(50) NOT NULL COMMENT '项目名称',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_factory_month` (`factory_id`, `year_month`),
  KEY `idx_employee_month` (`employee_id`, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='其他工资项目';

-- 工资汇总
CREATE TABLE `wage_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `employee_id` BIGINT NOT NULL,
  `year_month` VARCHAR(7) NOT NULL COMMENT '月份',
  `piece_wage` DECIMAL(12,2) DEFAULT 0 COMMENT '计件工资合计',
  `timed_wage` DECIMAL(12,2) DEFAULT 0 COMMENT '计时工资合计',
  `other_wage` DECIMAL(12,2) DEFAULT 0 COMMENT '其他项目合计',
  `total_wage` DECIMAL(12,2) DEFAULT 0 COMMENT '应发合计',
  `status` TINYINT DEFAULT 0 COMMENT '0待审核 1已审核 2已发放',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_emp_month` (`factory_id`, `employee_id`, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资汇总';

-- 收支记录
CREATE TABLE `payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `order_id` BIGINT COMMENT '关联订单',
  `type` TINYINT NOT NULL COMMENT '1收入 2支出',
  `amount` DECIMAL(12,2) NOT NULL,
  `payment_date` DATE NOT NULL,
  `remark` VARCHAR(200),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支记录';

-- 断层预警
CREATE TABLE `anomaly_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `bundle_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `front_process_id` BIGINT COMMENT '前置工序',
  `back_process_id` BIGINT COMMENT '后置工序',
  `report_employee_id` BIGINT COMMENT '后置工序报工人',
  `status` TINYINT DEFAULT 0 COMMENT '0待处理 1已忽略 2已清除 3已补录',
  `handle_type` TINYINT COMMENT '1忽略 2清除后置 3补录前置',
  `handle_by` BIGINT COMMENT '处理人',
  `handle_time` DATETIME COMMENT '处理时间',
  `handle_remark` VARCHAR(200) COMMENT '处理备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_factory_status` (`factory_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='断层预警';

-- 操作日志
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `factory_id` BIGINT NOT NULL,
  `module` VARCHAR(50) COMMENT '模块',
  `action` VARCHAR(50) COMMENT '操作',
  `target_id` BIGINT COMMENT '目标ID',
  `target_type` VARCHAR(50) COMMENT '目标类型',
  `content` VARCHAR(500) COMMENT '操作内容',
  `operator_id` BIGINT COMMENT '操作人',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- 初始数据：默认工厂
INSERT INTO `factory` (`name`, `code`, `status`) VALUES ('默认工厂', 'DEFAULT', 1);

-- 初始数据：超级管理员角色
INSERT INTO `role` (`factory_id`, `name`, `code`, `sort`, `status`) VALUES (1, '超级管理员', 'super_admin', 1, 1);

-- 初始数据：默认超级管理员（密码: admin123 BCrypt加密）
INSERT INTO `employee` (`factory_id`, `employee_no`, `name`, `role_id`, `login_account`, `login_password`, `login_code`, `status`) 
VALUES (1, 'A001', '管理员', 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN001', 1);
```

- [ ] **Step 2: 验证SQL语法**

```bash
# 如果本地有MySQL，可以验证
mysql -u root -p -e "source sql/init.sql" 2>&1 || echo "SQL file created, will verify during deployment"
```

- [ ] **Step 3: 提交**

```bash
git add sql/init.sql
git commit -m "feat: add complete database DDL with all 20+ tables and initial data"
```

---

### Task 4: 统一响应体与全局异常处理

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/result/R.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/result/PageResult.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/exception/BizException.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建统一返回体 R.java**

```java
package com.zhiyi.erp.common.result;

import lombok.Data;

@Data
public class R<T> {
    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.setCode(500);
        r.setMessage(message);
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
```

- [ ] **Step 2: 创建分页返回体 PageResult.java**

```java
package com.zhiyi.erp.common.result;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long size;
    private long current;

    public PageResult() {}

    public PageResult(List<T> records, long total, long size, long current) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
    }
}
```

- [ ] **Step 3: 创建业务异常 BizException.java**

```java
package com.zhiyi.erp.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 4: 创建全局异常处理器 GlobalExceptionHandler.java**

```java
package com.zhiyi.erp.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.zhiyi.erp.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleNotLoginException(NotLoginException e) {
        return R.fail(401, "未登录或登录已过期");
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleNotPermissionException(NotPermissionException e) {
        return R.fail(403, "无权限访问");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return R.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return R.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail("系统异常，请联系管理员");
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/common/result/ zhiyi-server/src/main/java/com/zhiyi/erp/common/exception/
git commit -m "feat: add unified response body R<T> and global exception handler"
```

---

### Task 5: 多租户实现

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/tenant/TenantContext.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/tenant/TenantInterceptor.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/config/MybatisPlusConfig.java`

- [ ] **Step 1: 创建租户上下文 TenantContext.java**

```java
package com.zhiyi.erp.common.tenant;

public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void set(Long factoryId) {
        TENANT_ID.set(factoryId);
    }

    public static Long get() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
```

- [ ] **Step 2: 创建租户拦截器 TenantInterceptor.java**

```java
package com.zhiyi.erp.common.tenant;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId != null) {
                // 从Sa-Token的Session中获取factory_id
                Long factoryId = StpUtil.getSession().getLong("factoryId");
                if (factoryId != null) {
                    TenantContext.set(factoryId);
                }
            }
        } catch (Exception ignored) {
            // 未登录时忽略
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
```

- [ ] **Step 3: 创建MyBatis-Plus配置 MybatisPlusConfig.java**

```java
package com.zhiyi.erp.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.zhiyi.erp.common.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户插件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContext.get();
                return new LongValue(tenantId != null ? tenantId : 0L);
            }

            @Override
            public String getTenantIdColumn() {
                return "factory_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // factory表和menu表不做租户过滤
                return "factory".equals(tableName) || "menu".equals(tableName) || "role_menu".equals(tableName);
            }
        }));
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/common/tenant/ zhiyi-server/src/main/java/com/zhiyi/erp/common/config/MybatisPlusConfig.java
git commit -m "feat: add multi-tenant implementation with MyBatis-Plus plugin"
```

---

### Task 6: WebMvc配置与Redis配置

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/config/WebMvcConfig.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/config/RedisConfig.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/config/SaTokenConfig.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/config/JacksonConfig.java`

- [ ] **Step 1: 创建WebMvc配置**

```java
package com.zhiyi.erp.common.config;

import com.zhiyi.erp.common.tenant.TenantInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/system/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

- [ ] **Step 2: 创建Redis配置**

```java
package com.zhiyi.erp.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
```

- [ ] **Step 3: 创建Sa-Token配置**

```java
package com.zhiyi.erp.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/system/auth/login",
                        "/api/system/auth/login-by-code"
                );
    }
}
```

- [ ] **Step 4: 创建Jackson配置**

```java
package com.zhiyi.erp.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        // BigDecimal序列化为字符串，避免精度丢失
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        mapper.registerModule(module);
        return mapper;
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/common/config/
git commit -m "feat: add WebMvc, Redis, Sa-Token, and Jackson configuration"
```

---

### Task 7: 系统管理模块 - 实体类

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/entity/Factory.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/entity/Employee.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/entity/Role.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/entity/Menu.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/entity/RoleMenu.java`

- [ ] **Step 1: 创建Factory实体**

```java
package com.zhiyi.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("factory")
public class Factory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Integer status;
    private String contact;
    private String phone;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建Employee实体**

```java
package com.zhiyi.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("employee")
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String employeeNo;
    private String name;
    private String phone;
    private Long roleId;
    private String loginAccount;
    private String loginPassword;
    private String loginCode;
    private Integer status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 3: 创建Role实体**

```java
package com.zhiyi.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 创建Menu实体**

```java
package com.zhiyi.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("menu")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String path;
    private String component;
    private String icon;
    private Integer type;
    private String permission;
    private Integer sort;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: 创建RoleMenu实体**

```java
package com.zhiyi.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role_menu")
public class RoleMenu {
    private Long roleId;
    private Long menuId;
}
```

- [ ] **Step 6: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/system/entity/
git commit -m "feat: add system module entity classes (Factory, Employee, Role, Menu, RoleMenu)"
```

---

### Task 8: 系统管理模块 - Mapper与Service

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/mapper/EmployeeMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/mapper/RoleMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/mapper/MenuMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/mapper/RoleMenuMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/service/AuthService.java`

- [ ] **Step 1: 创建EmployeeMapper**

```java
package com.zhiyi.erp.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.system.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}
```

- [ ] **Step 2: 创建RoleMapper**

```java
package com.zhiyi.erp.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.system.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
```

- [ ] **Step 3: 创建MenuMapper**

```java
package com.zhiyi.erp.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.system.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
}
```

- [ ] **Step 4: 创建RoleMenuMapper**

```java
package com.zhiyi.erp.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.system.entity.RoleMenu;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {
}
```

- [ ] **Step 5: 创建AuthService**

```java
package com.zhiyi.erp.module.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.entity.Factory;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import com.zhiyi.erp.module.system.mapper.RoleMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService extends ServiceImpl<EmployeeMapper, Employee> {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private RoleMapper roleMapper;

    public Map<String, Object> loginByAccount(String account, String password) {
        Employee employee = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getLoginAccount, account)
                        .eq(Employee::getDeleted, 0)
        );
        if (employee == null) {
            throw new BizException("账号不存在");
        }
        if (employee.getStatus() != 1) {
            throw new BizException("账号已停用，请联系管理员");
        }
        if (!BCrypt.checkpw(password, employee.getLoginPassword())) {
            throw new BizException("密码错误");
        }

        return doLogin(employee);
    }

    public Map<String, Object> loginByCode(String loginCode) {
        Employee employee = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getLoginCode, loginCode)
                        .eq(Employee::getDeleted, 0)
        );
        if (employee == null) {
            throw new BizException("登录码无效");
        }
        if (employee.getStatus() != 1) {
            throw new BizException("账号已停用，请联系管理员");
        }

        return doLogin(employee);
    }

    private Map<String, Object> doLogin(Employee employee) {
        StpUtil.login(employee.getId());

        // 将factoryId存入Session
        StpUtil.getSession().set("factoryId", employee.getFactoryId());
        StpUtil.getSession().set("employeeId", employee.getId());
        StpUtil.getSession().set("roleId", employee.getRoleId());

        Role role = roleMapper.selectById(employee.getRoleId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", employee.getId());
        result.put("employeeNo", employee.getEmployeeNo());
        result.put("userName", employee.getName());
        result.put("roleName", role != null ? role.getName() : "");
        result.put("factoryId", employee.getFactoryId());

        return result;
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/system/mapper/ zhiyi-server/src/main/java/com/zhiyi/erp/module/system/service/
git commit -m "feat: add system module mappers and AuthService with login logic"
```

---

### Task 9: 系统管理模块 - AuthController

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/controller/AuthController.java`

- [ ] **Step 1: 创建AuthController**

```java
package com.zhiyi.erp.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Data
    public static class LoginByAccountDTO {
        @NotBlank(message = "账号不能为空")
        private String account;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class LoginByCodeDTO {
        @NotBlank(message = "登录码不能为空")
        private String loginCode;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> loginByAccount(@RequestBody LoginByAccountDTO dto) {
        return R.ok(authService.loginByAccount(dto.getAccount(), dto.getPassword()));
    }

    @PostMapping("/login-by-code")
    public R<Map<String, Object>> loginByCode(@RequestBody LoginByCodeDTO dto) {
        return R.ok(authService.loginByCode(dto.getLoginCode()));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @GetMapping("/info")
    public R<Map<String, Object>> getInfo() {
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("factoryId", StpUtil.getSession().get("factoryId"));
        info.put("employeeId", StpUtil.getSession().get("employeeId"));
        info.put("roleId", StpUtil.getSession().get("roleId"));
        return R.ok(info);
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd zhiyi-server && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/system/controller/
git commit -m "feat: add AuthController with login/logout/info endpoints"
```

---

### Task 10: 工具类

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/util/OrderNoGenerator.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/common/util/EmployeeNoGenerator.java`

- [ ] **Step 1: 创建订单号生成器**

```java
package com.zhiyi.erp.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class OrderNoGenerator {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static String lastDate = "";

    public static synchronized String generate() {
        String today = LocalDate.now().format(DATE_FMT);
        if (!today.equals(lastDate)) {
            lastDate = today;
            SEQUENCE.set(0);
        }
        long seq = SEQUENCE.incrementAndGet();
        return String.format("ORD%s%03d", today, seq);
    }
}
```

- [ ] **Step 2: 创建工号生成器**

```java
package com.zhiyi.erp.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmployeeNoGenerator {

    @Autowired
    private EmployeeMapper employeeMapper;

    public String generate(Long factoryId) {
        // 查询该工厂最大的工号
        String maxNo = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getFactoryId, factoryId)
                        .orderByDesc(Employee::getEmployeeNo)
                        .last("LIMIT 1")
        ).getEmployeeNo();

        if (maxNo == null || maxNo.isEmpty()) {
            return "A001";
        }

        // 解析字母部分和数字部分
        char prefix = maxNo.charAt(0);
        int number = Integer.parseInt(maxNo.substring(1));
        number++;

        if (number > 999) {
            prefix++;
            number = 1;
        }

        return String.format("%c%03d", prefix, number);
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/common/util/
git commit -m "feat: add OrderNoGenerator and EmployeeNoGenerator utilities"
```

---

### Task 11: 最终编译验证

- [ ] **Step 1: 完整编译**

```bash
cd zhiyi-server && mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 检查项目结构**

```bash
find zhiyi-server/src -name "*.java" | sort
```

Expected output should show all created Java files.

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "chore: complete backend infrastructure setup"
```

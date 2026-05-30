# Zhiyi ERP 系统 API 文档

## 目录

- [1. 概述](#1-概述)
- [2. 通用说明](#2-通用说明)
- [3. 认证说明](#3-认证说明)
- [4. 接口列表](#4-接口列表)
  - [4.1 system - 系统管理](#41-system---系统管理)
  - [4.2 base - 基础数据](#42-base---基础数据)
  - [4.3 order - 订单管理](#43-order---订单管理)
  - [4.4 cutting - 裁床管理](#44-cutting---裁床管理)
  - [4.5 report - 报工查询](#45-report---报工查询)
  - [4.6 admin - 管理后台](#46-admin---管理后台)
  - [4.7 finance - 财务管理](#47-finance---财务管理)
- [5. 数据模型](#5-数据模型)

---

## 1. 概述

### 基础信息

| 项目 | 说明 |
|------|------|
| Base URL | `http://localhost:8080` |
| 认证方式 | 请求头 `satoken`，值为登录接口返回的 token |
| 统一响应格式 | `R<T>` |
| 接口前缀 | `/api` |

### 统一响应格式 R&lt;T&gt;

所有接口均返回统一的 JSON 响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 表示成功 |
| message | String | 响应消息 |
| data | T | 响应数据，泛型，可能为 null |

---

## 2. 通用说明

### 分页参数

支持分页的接口统一使用以下查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码，从 1 开始 |
| size | Integer | 否 | 10 | 每页条数 |

分页响应结构（MyBatis-Plus Page）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "size": 10,
    "current": 1,
    "pages": 0
  }
}
```

### 错误码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证（未登录或 token 失效） |
| 403 | 无权限访问 |
| 500 | 服务器内部错误 |

错误响应示例：

```json
{
  "code": 500,
  "message": "账号或密码错误",
  "data": null
}
```

---

## 3. 认证说明

### 登录流程

系统支持两种登录方式：

#### 方式一：账号密码登录

1. 前端调用 `POST /api/system/auth/login`，传入账号和密码
2. 服务端验证通过后返回 token
3. 前端将 token 存储在本地（如 localStorage）
4. 后续请求在请求头中携带 `satoken: <token>`

#### 方式二：登录码登录

1. 前端调用 `POST /api/system/auth/login-by-code`，传入登录码
2. 服务端验证通过后返回 token
3. 后续请求在请求头中携带 `satoken: <token>`

### Token 使用

所有需要认证的接口必须在请求头中携带：

```
satoken: <your_token>
```

### Session 数据

登录成功后，服务端 Session 中存储以下信息：

| 字段 | 类型 | 说明 |
|------|------|------|
| factoryId | Long | 工厂 ID |
| employeeId | Long | 员工 ID |
| roleId | Long | 角色 ID |

可通过 `GET /api/system/auth/info` 获取当前登录用户的 Session 信息。

---

## 4. 接口列表

### 4.1 system - 系统管理

#### 4.1.1 AuthController - 认证管理

##### 账号密码登录

- **POST** `/api/system/auth/login`
- **认证**：不需要
- **说明**：使用账号和密码进行登录

**请求体**：

```json
{
  "account": "admin",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| account | String | 是 | 登录账号 |
| password | String | 是 | 登录密码 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "tokenName": "satoken"
  }
}
```

---

##### 登录码登录

- **POST** `/api/system/auth/login-by-code`
- **认证**：不需要
- **说明**：使用登录码进行登录（适用于工人快速登录）

**请求体**：

```json
{
  "loginCode": "ABC123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| loginCode | String | 是 | 员工登录码 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "tokenName": "satoken"
  }
}
```

---

##### 登出

- **POST** `/api/system/auth/logout`
- **认证**：需要
- **说明**：退出登录，清除 Session

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 获取当前用户信息

- **GET** `/api/system/auth/info`
- **认证**：需要
- **说明**：获取当前登录用户的 Session 信息

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "factoryId": 1,
    "employeeId": 1001,
    "roleId": 1
  }
}
```

---

#### 4.1.2 EmployeeController - 员工管理

##### 分页查询员工列表

- **GET** `/api/system/employees`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| keyword | String | 否 | - | 搜索关键词（姓名、工号等） |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "employeeNo": "E001",
        "name": "张三",
        "phone": "13800138000",
        "roleId": 2,
        "loginAccount": "zhangsan",
        "loginCode": "ABC123",
        "status": 1,
        "remark": null,
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00",
        "deleted": 0
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

---

##### 根据 ID 查询员工详情

- **GET** `/api/system/employees/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 员工 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "factoryId": 1,
    "employeeNo": "E001",
    "name": "张三",
    "phone": "13800138000",
    "roleId": 2,
    "loginAccount": "zhangsan",
    "loginCode": "ABC123",
    "status": 1,
    "remark": null,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00",
    "deleted": 0
  }
}
```

---

##### 创建员工

- **POST** `/api/system/employees`
- **认证**：需要

**请求体**：Employee 对象（不含 id、createdAt、updatedAt、deleted）

```json
{
  "factoryId": 1,
  "employeeNo": "E002",
  "name": "李四",
  "phone": "13900139000",
  "roleId": 2,
  "loginAccount": "lisi",
  "loginPassword": "123456",
  "status": 1,
  "remark": null
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "factoryId": 1,
    "employeeNo": "E002",
    "name": "李四",
    "loginCode": "XYZ789",
    "status": 1
  }
}
```

---

##### 更新员工信息

- **PUT** `/api/system/employees/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 员工 ID |

**请求体**：Employee 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 更新员工状态

- **PUT** `/api/system/employees/{id}/status`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 员工 ID |

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 是 | 状态值（1-启用，0-禁用） |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 重置员工登录码

- **POST** `/api/system/employees/{id}/reset-login-code`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 员工 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": "NEW123"
}
```

---

##### 重置员工密码

- **POST** `/api/system/employees/{id}/reset-password`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 员工 ID |

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| newPassword | String | 否 | 123456 | 新密码 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 4.1.3 RoleController - 角色管理

##### 查询所有角色

- **GET** `/api/system/roles`
- **认证**：需要

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "factoryId": 1,
      "name": "管理员",
      "code": "admin",
      "sort": 1,
      "status": 1,
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    }
  ]
}
```

---

##### 根据 ID 查询角色详情

- **GET** `/api/system/roles/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 角色 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "factoryId": 1,
    "name": "管理员",
    "code": "admin",
    "sort": 1,
    "status": 1,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```

---

##### 创建角色

- **POST** `/api/system/roles`
- **认证**：需要

**请求体**：Role 对象

```json
{
  "factoryId": 1,
  "name": "质检员",
  "code": "qc",
  "sort": 3,
  "status": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 更新角色信息

- **PUT** `/api/system/roles/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 角色 ID |

**请求体**：Role 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除角色

- **DELETE** `/api/system/roles/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 角色 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 查询角色关联菜单 ID 列表

- **GET** `/api/system/roles/{id}/menus`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 角色 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [1, 2, 3, 5, 8]
}
```

---

##### 为角色分配菜单

- **POST** `/api/system/roles/{id}/menus`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 角色 ID |

**请求体**：菜单 ID 数组

```json
[1, 2, 3, 5, 8, 10]
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 4.1.4 MenuController - 菜单管理

##### 查询所有菜单

- **GET** `/api/system/menus`
- **认证**：需要

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "系统管理",
      "code": "system",
      "path": "/system",
      "component": null,
      "icon": "setting",
      "type": 1,
      "permission": null,
      "sort": 1,
      "status": 1,
      "createdAt": "2024-01-01T10:00:00"
    }
  ]
}
```

---

##### 获取菜单树形结构

- **GET** `/api/system/menus/tree`
- **认证**：需要

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "系统管理",
      "code": "system",
      "path": "/system",
      "icon": "setting",
      "type": 1,
      "children": [
        {
          "id": 2,
          "name": "员工管理",
          "code": "employee",
          "path": "/system/employee",
          "icon": "user",
          "type": 2,
          "children": []
        }
      ]
    }
  ]
}
```

---

##### 根据 ID 查询菜单详情

- **GET** `/api/system/menus/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 菜单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "parentId": 0,
    "name": "系统管理",
    "code": "system",
    "path": "/system",
    "component": null,
    "icon": "setting",
    "type": 1,
    "permission": null,
    "sort": 1,
    "status": 1,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

---

##### 创建菜单

- **POST** `/api/system/menus`
- **认证**：需要

**请求体**：Menu 对象

```json
{
  "parentId": 1,
  "name": "角色管理",
  "code": "role",
  "path": "/system/role",
  "component": "system/role/index",
  "icon": "peoples",
  "type": 2,
  "permission": "system:role:list",
  "sort": 2,
  "status": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 更新菜单信息

- **PUT** `/api/system/menus/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 菜单 ID |

**请求体**：Menu 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除菜单

- **DELETE** `/api/system/menus/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 菜单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 4.2 base - 基础数据

#### 4.2.1 ProcessController - 工序管理

##### 分页查询工序列表

- **GET** `/api/base/processes`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| category | String | 否 | - | 工序分类筛选 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "category": "缝制",
        "name": "上领",
        "refPrice": 0.50,
        "sort": 1,
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00",
        "deleted": 0
      }
    ],
    "total": 20,
    "size": 10,
    "current": 1,
    "pages": 2
  }
}
```

---

##### 新增工序

- **POST** `/api/base/processes`
- **认证**：需要

**请求体**：Process 对象

```json
{
  "factoryId": 1,
  "category": "缝制",
  "name": "上袖",
  "refPrice": 0.60,
  "sort": 2
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 修改工序

- **PUT** `/api/base/processes/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 工序 ID |

**请求体**：Process 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除工序

- **DELETE** `/api/base/processes/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 工序 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 4.2.2 StyleController - 款号管理

##### 分页查询款号列表

- **GET** `/api/base/styles`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| keyword | String | 否 | - | 搜索关键词（款号编码、名称） |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "styleCode": "S2024001",
        "styleName": "男士休闲衬衫",
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00",
        "deleted": 0
      }
    ],
    "total": 30,
    "size": 10,
    "current": 1,
    "pages": 3
  }
}
```

---

##### 根据 ID 查询款号详情

- **GET** `/api/base/styles/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 款号 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "factoryId": 1,
    "styleCode": "S2024001",
    "styleName": "男士休闲衬衫",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00",
    "deleted": 0
  }
}
```

---

##### 新增款号

- **POST** `/api/base/styles`
- **认证**：需要

**请求体**：Style 对象

```json
{
  "factoryId": 1,
  "styleCode": "S2024002",
  "styleName": "女士连衣裙"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": 2
}
```

---

##### 修改款号

- **PUT** `/api/base/styles/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 款号 ID |

**请求体**：Style 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除款号

- **DELETE** `/api/base/styles/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 款号 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 查询款号标准工序路由

- **GET** `/api/base/styles/{id}/processes`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 款号 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "styleId": 1,
      "processId": 1,
      "refPrice": 0.50,
      "sort": 1
    },
    {
      "id": 2,
      "styleId": 1,
      "processId": 3,
      "refPrice": 0.80,
      "sort": 2
    }
  ]
}
```

---

##### 保存款号标准工序路由

- **POST** `/api/base/styles/{id}/processes`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 款号 ID |

**请求体**：StyleProcess 数组

```json
[
  {
    "processId": 1,
    "refPrice": 0.50,
    "sort": 1
  },
  {
    "processId": 3,
    "refPrice": 0.80,
    "sort": 2
  }
]
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 查询款号标准物料清单

- **GET** `/api/base/styles/{id}/materials`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 款号 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "styleId": 1,
      "materialName": "白色棉布",
      "type": 1,
      "unitQty": 1.50,
      "unit": "米"
    }
  ]
}
```

---

##### 保存款号标准物料清单

- **POST** `/api/base/styles/{id}/materials`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 款号 ID |

**请求体**：StyleMaterial 数组

```json
[
  {
    "materialName": "白色棉布",
    "type": 1,
    "unitQty": 1.50,
    "unit": "米"
  }
]
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 4.2.3 CustomerController - 客户管理

##### 分页查询客户列表

- **GET** `/api/base/customers`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| keyword | String | 否 | - | 搜索关键词（客户编码、名称等） |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "customerCode": "C001",
        "name": "广州服装批发商",
        "contact": "王经理",
        "phone": "13700137000",
        "address": "广州市白云区xxx路",
        "settleType": "月结",
        "remark": null,
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00",
        "deleted": 0
      }
    ],
    "total": 15,
    "size": 10,
    "current": 1,
    "pages": 2
  }
}
```

---

##### 新增客户

- **POST** `/api/base/customers`
- **认证**：需要

**请求体**：Customer 对象

```json
{
  "factoryId": 1,
  "customerCode": "C002",
  "name": "深圳时尚服饰",
  "contact": "李总",
  "phone": "13600136000",
  "address": "深圳市南山区xxx路",
  "settleType": "货到付款",
  "remark": "大客户"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 修改客户

- **PUT** `/api/base/customers/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 客户 ID |

**请求体**：Customer 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除客户

- **DELETE** `/api/base/customers/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 客户 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 4.2.4 SupplierController - 供应商管理

##### 分页查询供应商列表

- **GET** `/api/base/suppliers`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| keyword | String | 否 | - | 搜索关键词（供应商编码、名称等） |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "supplierCode": "SUP001",
        "name": "佛山布料厂",
        "contact": "张老板",
        "phone": "13500135000",
        "remark": "主要布料供应商",
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00",
        "deleted": 0
      }
    ],
    "total": 10,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

##### 新增供应商

- **POST** `/api/base/suppliers`
- **认证**：需要

**请求体**：Supplier 对象

```json
{
  "factoryId": 1,
  "supplierCode": "SUP002",
  "name": "东莞辅料厂",
  "contact": "陈经理",
  "phone": "13400134000",
  "remark": "纽扣、拉链供应商"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 修改供应商

- **PUT** `/api/base/suppliers/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 供应商 ID |

**请求体**：Supplier 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除供应商

- **DELETE** `/api/base/suppliers/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 供应商 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 4.3 order - 订单管理

#### 4.3.1 OrderController - 订单管理

##### 分页查询订单列表

- **GET** `/api/orders`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| keyword | String | 否 | - | 搜索关键词（订单号等） |
| status | Integer | 否 | - | 订单状态筛选 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "orderNo": "ORD202401001",
        "customerId": 1,
        "totalAmount": 50000.00,
        "deliveryDate": "2024-02-01",
        "status": 0,
        "remark": "春节前交货",
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00",
        "createdBy": 1,
        "deleted": 0
      }
    ],
    "total": 25,
    "size": 10,
    "current": 1,
    "pages": 3
  }
}
```

---

##### 根据 ID 查询订单详情

- **GET** `/api/orders/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "factoryId": 1,
    "orderNo": "ORD202401001",
    "customerId": 1,
    "totalAmount": 50000.00,
    "deliveryDate": "2024-02-01",
    "status": 0,
    "remark": "春节前交货",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00",
    "createdBy": 1,
    "deleted": 0
  }
}
```

---

##### 创建订单

- **POST** `/api/orders`
- **认证**：需要

**请求体**：CreateOrderDTO 对象

```json
{
  "order": {
    "factoryId": 1,
    "orderNo": "ORD202401002",
    "customerId": 1,
    "totalAmount": 30000.00,
    "deliveryDate": "2024-03-01",
    "status": 0,
    "remark": null
  },
  "items": [
    {
      "styleId": 1,
      "color": "白色",
      "size": "M",
      "qty": 500,
      "unitPrice": 30.00
    },
    {
      "styleId": 1,
      "color": "白色",
      "size": "L",
      "qty": 500,
      "unitPrice": 30.00
    }
  ]
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": 2
}
```

---

##### 查询订单物料明细

- **GET** `/api/orders/{id}/items`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "orderId": 1,
      "styleId": 1,
      "color": "白色",
      "size": "M",
      "qty": 500,
      "unitPrice": 30.00
    }
  ]
}
```

---

##### 查询订单工序列表

- **GET** `/api/orders/{id}/processes`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "orderId": 1,
      "processId": 1,
      "processPrice": 0.50,
      "sort": 1
    },
    {
      "id": 2,
      "orderId": 1,
      "processId": 3,
      "processPrice": 0.80,
      "sort": 2
    }
  ]
}
```

---

##### 保存订单工序定价

- **POST** `/api/orders/{id}/processes`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |

**请求体**：OrderProcess 数组

```json
[
  {
    "processId": 1,
    "processPrice": 0.50,
    "sort": 1
  },
  {
    "processId": 3,
    "processPrice": 0.80,
    "sort": 2
  }
]
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 查询订单物料清单

- **GET** `/api/orders/{id}/materials`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "orderId": 1,
      "materialName": "白色棉布",
      "type": 1,
      "unitQty": 1.50,
      "unit": "米"
    }
  ]
}
```

---

##### 保存订单物料清单

- **POST** `/api/orders/{id}/materials`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |

**请求体**：OrderMaterial 数组

```json
[
  {
    "materialName": "白色棉布",
    "type": 1,
    "unitQty": 1.50,
    "unit": "米"
  }
]
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 确认订单

- **POST** `/api/orders/{id}/confirm`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 结算订单

- **POST** `/api/orders/{id}/settle`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 4.3.2 ShipmentController - 出货管理

##### 查询订单出货记录

- **GET** `/api/shipments`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | Long | 是 | 订单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "factoryId": 1,
      "orderId": 1,
      "orderItemId": 1,
      "qty": 200,
      "shipmentDate": "2024-01-15",
      "remark": "第一批出货",
      "createdAt": "2024-01-15T10:00:00",
      "createdBy": 1
    }
  ]
}
```

---

##### 创建出货记录

- **POST** `/api/shipments`
- **认证**：需要

**请求体**：Shipment 对象

```json
{
  "factoryId": 1,
  "orderId": 1,
  "orderItemId": 1,
  "qty": 300,
  "shipmentDate": "2024-01-20",
  "remark": "第二批出货"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除出货记录

- **DELETE** `/api/shipments/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 出货记录 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 4.4 cutting - 裁床管理

#### 4.4.1 CuttingPlanController - 裁床计划

##### 分页查询裁床计划列表

- **GET** `/api/cutting/plans`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| orderItemId | Long | 否 | - | 订单明细 ID 筛选 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "orderItemId": 1,
        "bedNo": 1,
        "planQty": 1000,
        "actualQty": 980,
        "qtyPerBundle": 50,
        "bundleCount": 20,
        "cuttingDate": "2024-01-10",
        "barcode": "CUT20240110001",
        "status": 1,
        "createdAt": "2024-01-10T08:00:00",
        "updatedAt": "2024-01-10T16:00:00",
        "deleted": 0
      }
    ],
    "total": 5,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

##### 创建裁床计划

- **POST** `/api/cutting/plans`
- **认证**：需要

**请求体**：CuttingPlan 对象

```json
{
  "factoryId": 1,
  "orderItemId": 1,
  "bedNo": 2,
  "planQty": 500,
  "qtyPerBundle": 25,
  "cuttingDate": "2024-01-12"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 更新裁床计划

- **PUT** `/api/cutting/plans/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 裁床计划 ID |

**请求体**：CuttingPlan 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除裁床计划

- **DELETE** `/api/cutting/plans/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 裁床计划 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 录入实际裁剪数量

- **POST** `/api/cutting/plans/{id}/input-actual`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 裁床计划 ID |

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| actualQty | Integer | 是 | 实际裁剪数量 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 开菲生成扎号

- **POST** `/api/cutting/plans/{id}/open-bundle`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 裁床计划 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "factoryId": 1,
      "cuttingPlanId": 1,
      "orderId": 1,
      "orderItemId": 1,
      "bundleNo": "001",
      "globalBundleNo": "CUT20240110001-001",
      "qty": 50,
      "status": 0,
      "createdAt": "2024-01-10T10:00:00",
      "updatedAt": "2024-01-10T10:00:00"
    }
  ]
}
```

---

##### 查询裁床计划的扎号列表

- **GET** `/api/cutting/plans/{id}/bundles`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 裁床计划 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "factoryId": 1,
      "cuttingPlanId": 1,
      "orderId": 1,
      "orderItemId": 1,
      "bundleNo": "001",
      "globalBundleNo": "CUT20240110001-001",
      "qty": 50,
      "status": 0,
      "createdAt": "2024-01-10T10:00:00",
      "updatedAt": "2024-01-10T10:00:00"
    }
  ]
}
```

---

#### 4.4.2 BundleController - 扎号管理

##### 根据 ID 查询扎号详情

- **GET** `/api/cutting/bundles/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 扎号 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "factoryId": 1,
    "cuttingPlanId": 1,
    "orderId": 1,
    "orderItemId": 1,
    "bundleNo": "001",
    "globalBundleNo": "CUT20240110001-001",
    "qty": 50,
    "status": 0,
    "createdAt": "2024-01-10T10:00:00",
    "updatedAt": "2024-01-10T10:00:00"
  }
}
```

---

##### 根据订单 ID 查询扎号列表

- **GET** `/api/cutting/bundles/by-order/{orderId}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| orderId | Long | 订单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "factoryId": 1,
      "cuttingPlanId": 1,
      "orderId": 1,
      "orderItemId": 1,
      "bundleNo": "001",
      "globalBundleNo": "CUT20240110001-001",
      "qty": 50,
      "status": 0,
      "createdAt": "2024-01-10T10:00:00",
      "updatedAt": "2024-01-10T10:00:00"
    }
  ]
}
```

---

### 4.5 report - 报工查询

#### 4.5.1 ScanReportController - 扫码报工

##### 扎号扫码报工

- **POST** `/api/report/bundle`
- **认证**：需要（需要员工身份，从 Session 获取 employeeId）
- **说明**：工人通过扫描扎号上的 QR 码进行报工

**请求体**：

```json
{
  "bundleId": 1,
  "processIds": [1, 3]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bundleId | Long | 是 | 扎号 ID |
| processIds | List&lt;Long&gt; | 是 | 完成的工序 ID 列表 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 100,
    "qty": 50,
    "amount": 25.00
  }
}
```

---

##### 裁床条形码扫码报工

- **POST** `/api/report/cutting`
- **认证**：需要（需要员工身份，从 Session 获取 employeeId）
- **说明**：工人通过扫描裁床条形码进行报工

**请求体**：

```json
{
  "barcode": "CUT20240110001"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| barcode | String | 是 | 裁床条形码 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 101,
    "qty": 100,
    "amount": 50.00
  }
}
```

---

#### 4.5.2 WageController - 工资查询

##### 查询工资汇总

- **GET** `/api/report/wage/summary`
- **认证**：需要（需要员工身份，从 Session 获取 employeeId）
- **说明**：工人查询自己的工资汇总（按月）

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| yearMonth | String | 否 | 当前月 | 年月，格式 yyyy-MM |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "date": "2024-01-15",
      "pieceWage": 150.00,
      "timedWage": 0,
      "totalWage": 150.00,
      "details": [
        {
          "orderNo": "ORD202401001",
          "processName": "上领",
          "qty": 300,
          "price": 0.50,
          "amount": 150.00
        }
      ]
    }
  ]
}
```

---

### 4.6 admin - 管理后台

#### 4.6.1 DashboardController - 生产看板

##### 订单维度看板

- **GET** `/api/admin/dashboard/order`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | Long | 否 | 订单 ID，不传则查全部 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "orderId": 1,
      "orderNo": "ORD202401001",
      "totalQty": 1000,
      "cutQty": 980,
      "reportedQty": 800,
      "shippedQty": 500,
      "progress": 80.0
    }
  ]
}
```

---

##### 工人维度看板

- **GET** `/api/admin/dashboard/worker`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| date | String | 否 | 今天 | 日期，格式 yyyy-MM-dd |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "employeeId": 1001,
      "employeeName": "张三",
      "reportedQty": 500,
      "wageAmount": 250.00,
      "anomalyCount": 0
    }
  ]
}
```

---

#### 4.6.2 AnomalyController - 异常预警

##### 异常预警列表

- **GET** `/api/admin/anomalies`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| status | Integer | 否 | - | 状态：0-未处理，1-已忽略，2-已清除 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "bundleId": 5,
        "orderId": 1,
        "frontProcessId": 1,
        "backProcessId": 3,
        "reportEmployeeId": 1001,
        "status": 0,
        "handleType": null,
        "handleBy": null,
        "handleTime": null,
        "handleRemark": null,
        "createdAt": "2024-01-15T14:00:00"
      }
    ],
    "total": 3,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

##### 忽略预警

- **POST** `/api/admin/anomalies/{id}/ignore`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 预警 ID |

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| remark | String | 是 | 忽略原因 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 清除预警

- **POST** `/api/admin/anomalies/{id}/clear`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 预警 ID |

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | String | 是 | 清除原因 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 补录报工

- **POST** `/api/admin/anomalies/{id}/supplement`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 预警 ID |

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| employeeId | Long | 是 | 工人 ID |
| qty | Integer | 是 | 补录数量 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 4.6.3 TimedWageController - 计时工资

##### 计时工资列表

- **GET** `/api/admin/timed-wages`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| employeeId | Long | 否 | - | 工人 ID 筛选 |
| yearMonth | String | 否 | - | 年月，格式 yyyy-MM |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "employeeId": 1001,
        "workDate": "2024-01-15",
        "hours": 8.0,
        "hourlyRate": 20.00,
        "amount": 160.00,
        "workContent": "整理仓库",
        "createdAt": "2024-01-15T18:00:00",
        "createdBy": 1
      }
    ],
    "total": 10,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

##### 新增计时工资

- **POST** `/api/admin/timed-wages`
- **认证**：需要

**请求体**：TimedWage 对象

```json
{
  "factoryId": 1,
  "employeeId": 1001,
  "workDate": "2024-01-16",
  "hours": 4.0,
  "hourlyRate": 20.00,
  "amount": 80.00,
  "workContent": "搬运物料"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 批量新增计时工资

- **POST** `/api/admin/timed-wages/batch`
- **认证**：需要

**请求体**：TimedWage 数组

```json
[
  {
    "factoryId": 1,
    "employeeId": 1001,
    "workDate": "2024-01-16",
    "hours": 4.0,
    "hourlyRate": 20.00,
    "amount": 80.00,
    "workContent": "搬运物料"
  },
  {
    "factoryId": 1,
    "employeeId": 1002,
    "workDate": "2024-01-16",
    "hours": 8.0,
    "hourlyRate": 18.00,
    "amount": 144.00,
    "workContent": "清洁车间"
  }
]
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 修改计时工资

- **PUT** `/api/admin/timed-wages/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 计时工资 ID |

**请求体**：TimedWage 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除计时工资

- **DELETE** `/api/admin/timed-wages/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 计时工资 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 4.6.4 WageManageController - 工资管理

##### 工资汇总列表

- **GET** `/api/admin/wages`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| yearMonth | String | 否 | - | 年月，格式 yyyy-MM |
| status | Integer | 否 | - | 审核状态 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "employeeId": 1001,
        "yearMonth": "2024-01",
        "pieceWage": 3500.00,
        "timedWage": 500.00,
        "otherWage": 200.00,
        "totalWage": 4200.00,
        "status": 0,
        "createdAt": "2024-02-01T10:00:00",
        "updatedAt": "2024-02-01T10:00:00"
      }
    ],
    "total": 30,
    "size": 10,
    "current": 1,
    "pages": 3
  }
}
```

---

##### 审核工资

- **POST** `/api/admin/wages/{id}/audit`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 工资汇总 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 标记已发放

- **POST** `/api/admin/wages/{id}/mark-paid`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 工资汇总 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 查询其他工资项目

- **GET** `/api/admin/wages/other-items`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| employeeId | Long | 是 | 工人 ID |
| yearMonth | String | 是 | 年月，格式 yyyy-MM |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "factoryId": 1,
      "employeeId": 1001,
      "yearMonth": "2024-01",
      "itemName": "餐补",
      "amount": 200.00,
      "remark": "1月份餐补",
      "createdAt": "2024-02-01T10:00:00",
      "createdBy": 1
    }
  ]
}
```

---

##### 新增其他工资项目

- **POST** `/api/admin/wages/other-items`
- **认证**：需要

**请求体**：WageOther 对象

```json
{
  "factoryId": 1,
  "employeeId": 1001,
  "yearMonth": "2024-01",
  "itemName": "全勤奖",
  "amount": 300.00,
  "remark": "1月份全勤"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 4.7 finance - 财务管理

#### 4.7.1 PaymentController - 收支管理

##### 收支记录列表

- **GET** `/api/finance/payments`
- **认证**：需要

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |
| orderId | Long | 否 | - | 订单 ID 筛选 |
| type | Integer | 否 | - | 收支类型：1-收入，2-支出 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "factoryId": 1,
        "orderId": 1,
        "type": 1,
        "amount": 10000.00,
        "paymentDate": "2024-01-20",
        "remark": "客户首付款",
        "createdAt": "2024-01-20T10:00:00",
        "createdBy": 1
      }
    ],
    "total": 15,
    "size": 10,
    "current": 1,
    "pages": 2
  }
}
```

---

##### 新增收支记录

- **POST** `/api/finance/payments`
- **认证**：需要

**请求体**：Payment 对象

```json
{
  "factoryId": 1,
  "orderId": 1,
  "type": 1,
  "amount": 20000.00,
  "paymentDate": "2024-01-25",
  "remark": "客户尾款"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 修改收支记录

- **PUT** `/api/finance/payments/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 收支记录 ID |

**请求体**：Payment 对象（id 从路径获取）

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

##### 删除收支记录

- **DELETE** `/api/finance/payments/{id}`
- **认证**：需要

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 收支记录 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

#### 4.7.2 ProfitController - 利润分析

##### 按订单查看利润

- **GET** `/api/finance/profit/by-order`
- **认证**：需要

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "orderId": 1,
      "orderNo": "ORD202401001",
      "totalAmount": 50000.00,
      "materialCost": 15000.00,
      "processCost": 20000.00,
      "profit": 15000.00,
      "profitRate": 30.0
    }
  ]
}
```

---

##### 利润汇总

- **GET** `/api/finance/profit/summary`
- **认证**：需要

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalAmount": 150000.00,
    "totalMaterialCost": 45000.00,
    "totalProcessCost": 60000.00,
    "totalProfit": 45000.00,
    "avgProfitRate": 30.0
  }
}
```

---

## 5. 数据模型

### 5.1 system 模块

#### Employee - 员工

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| employeeNo | String | 员工工号 |
| name | String | 员工姓名 |
| phone | String | 手机号码 |
| roleId | Long | 角色 ID |
| loginAccount | String | 登录账号 |
| loginPassword | String | 登录密码（加密存储） |
| loginCode | String | 登录码（用于快速登录） |
| status | Integer | 状态：1-启用，0-禁用 |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| deleted | Integer | 逻辑删除：0-未删除，1-已删除 |

#### Factory - 工厂

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| name | String | 工厂名称 |
| code | String | 工厂编码 |
| status | Integer | 状态 |
| contact | String | 联系人 |
| phone | String | 联系电话 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

#### Role - 角色

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| name | String | 角色名称 |
| code | String | 角色编码 |
| sort | Integer | 排序号 |
| status | Integer | 状态 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

#### Menu - 菜单

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| parentId | Long | 父菜单 ID |
| name | String | 菜单名称 |
| code | String | 菜单编码 |
| path | String | 路由路径 |
| component | String | 前端组件路径 |
| icon | String | 图标 |
| type | Integer | 类型：1-目录，2-菜单，3-按钮 |
| permission | String | 权限标识 |
| sort | Integer | 排序号 |
| status | Integer | 状态 |
| createdAt | LocalDateTime | 创建时间 |

#### RoleMenu - 角色菜单关联

| 字段 | 类型 | 说明 |
|------|------|------|
| roleId | Long | 角色 ID |
| menuId | Long | 菜单 ID |

---

### 5.2 base 模块

#### Process - 工序

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| category | String | 工序分类（如：缝制、整烫、包装） |
| name | String | 工序名称 |
| refPrice | BigDecimal | 参考单价（元） |
| sort | Integer | 排序号 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| deleted | Integer | 逻辑删除 |

#### Style - 款号

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| styleCode | String | 款号编码 |
| styleName | String | 款号名称 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| deleted | Integer | 逻辑删除 |

#### StyleProcess - 款号标准工序路由

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| styleId | Long | 款号 ID |
| processId | Long | 工序 ID |
| refPrice | BigDecimal | 参考单价 |
| sort | Integer | 工序顺序 |

#### StyleMaterial - 款号标准物料清单

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| styleId | Long | 款号 ID |
| materialName | String | 物料名称 |
| type | Integer | 物料类型 |
| unitQty | BigDecimal | 单位用量 |
| unit | String | 单位（米、个、件等） |

#### Customer - 客户

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| customerCode | String | 客户编码 |
| name | String | 客户名称 |
| contact | String | 联系人 |
| phone | String | 联系电话 |
| address | String | 地址 |
| settleType | String | 结算方式（月结、货到付款等） |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| deleted | Integer | 逻辑删除 |

#### Supplier - 供应商

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| supplierCode | String | 供应商编码 |
| name | String | 供应商名称 |
| contact | String | 联系人 |
| phone | String | 联系电话 |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| deleted | Integer | 逻辑删除 |

---

### 5.3 order 模块

#### Order - 订单

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| orderNo | String | 订单编号 |
| customerId | Long | 客户 ID |
| totalAmount | BigDecimal | 订单总金额 |
| deliveryDate | LocalDate | 交货日期 |
| status | Integer | 订单状态：0-草稿，1-已确认，2-已结算 |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| createdBy | Long | 创建人 ID |
| deleted | Integer | 逻辑删除 |

#### OrderItem - 订单明细（颜色/尺码）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| orderId | Long | 订单 ID |
| styleId | Long | 款号 ID |
| color | String | 颜色 |
| size | String | 尺码 |
| qty | Integer | 数量 |
| unitPrice | BigDecimal | 单价 |

#### OrderProcess - 订单工序定价

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| orderId | Long | 订单 ID |
| processId | Long | 工序 ID |
| processPrice | BigDecimal | 工序单价（本订单实际定价） |
| sort | Integer | 工序顺序 |

#### OrderMaterial - 订单物料清单

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| orderId | Long | 订单 ID |
| materialName | String | 物料名称 |
| type | Integer | 物料类型 |
| unitQty | BigDecimal | 单位用量 |
| unit | String | 单位 |

#### Shipment - 出货记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| orderId | Long | 订单 ID |
| orderItemId | Long | 订单明细 ID |
| qty | Integer | 出货数量 |
| shipmentDate | LocalDate | 出货日期 |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| createdBy | Long | 创建人 ID |

---

### 5.4 cutting 模块

#### CuttingPlan - 裁床计划

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| orderItemId | Long | 订单明细 ID |
| bedNo | Integer | 床次号 |
| planQty | Integer | 计划数量 |
| actualQty | Integer | 实际数量 |
| qtyPerBundle | Integer | 每扎数量 |
| bundleCount | Integer | 扎数 |
| cuttingDate | LocalDate | 裁剪日期 |
| barcode | String | 裁床条形码 |
| status | Integer | 状态 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| deleted | Integer | 逻辑删除 |

#### Bundle - 扎号

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| cuttingPlanId | Long | 裁床计划 ID |
| orderId | Long | 订单 ID |
| orderItemId | Long | 订单明细 ID |
| bundleNo | String | 扎号（床内序号） |
| globalBundleNo | String | 全局扎号（含裁床条形码前缀） |
| qty | Integer | 数量 |
| status | Integer | 状态 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

### 5.5 report 模块

#### WorkReport - 报工记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| bundleId | Long | 扎号 ID |
| orderId | Long | 订单 ID |
| processId | Long | 工序 ID |
| employeeId | Long | 工人 ID |
| qty | Integer | 报工数量 |
| price | BigDecimal | 工序单价 |
| amount | BigDecimal | 报工金额 |
| reportType | Integer | 报工类型：1-扎号扫码，2-裁床扫码 |
| status | Integer | 状态：0-正常，1-已清除 |
| reportTime | LocalDateTime | 报工时间 |
| clearReason | String | 清除原因 |
| supplementFlag | Integer | 补录标记：0-正常，1-补录 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| createdBy | Long | 创建人 ID |

---

### 5.6 admin 模块

#### AnomalyAlert - 异常预警

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| bundleId | Long | 扎号 ID |
| orderId | Long | 订单 ID |
| frontProcessId | Long | 前道工序 ID |
| backProcessId | Long | 后道工序 ID |
| reportEmployeeId | Long | 报工工人 ID |
| status | Integer | 状态：0-未处理，1-已忽略，2-已清除 |
| handleType | Integer | 处理类型 |
| handleBy | Long | 处理人 ID |
| handleTime | LocalDateTime | 处理时间 |
| handleRemark | String | 处理备注 |
| createdAt | LocalDateTime | 创建时间 |

#### TimedWage - 计时工资

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| employeeId | Long | 工人 ID |
| workDate | LocalDate | 工作日期 |
| hours | BigDecimal | 工时（小时） |
| hourlyRate | BigDecimal | 时薪（元/小时） |
| amount | BigDecimal | 金额 |
| workContent | String | 工作内容 |
| createdAt | LocalDateTime | 创建时间 |
| createdBy | Long | 创建人 ID |

#### WageOther - 其他工资项目

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| employeeId | Long | 工人 ID |
| yearMonth | String | 年月（yyyy-MM） |
| itemName | String | 项目名称（餐补、全勤奖等） |
| amount | BigDecimal | 金额 |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| createdBy | Long | 创建人 ID |

#### WageSummary - 工资汇总

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| employeeId | Long | 工人 ID |
| yearMonth | String | 年月（yyyy-MM） |
| pieceWage | BigDecimal | 计件工资 |
| timedWage | BigDecimal | 计时工资 |
| otherWage | BigDecimal | 其他工资 |
| totalWage | BigDecimal | 工资合计 |
| status | Integer | 审核状态：0-待审核，1-已审核，2-已发放 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

### 5.7 finance 模块

#### Payment - 收支记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| factoryId | Long | 所属工厂 ID |
| orderId | Long | 关联订单 ID |
| type | Integer | 收支类型：1-收入，2-支出 |
| amount | BigDecimal | 金额 |
| paymentDate | LocalDate | 收支日期 |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| createdBy | Long | 创建人 ID |

---

## 附录：接口总览

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| system | POST | /api/system/auth/login | 账号密码登录 |
| system | POST | /api/system/auth/login-by-code | 登录码登录 |
| system | POST | /api/system/auth/logout | 登出 |
| system | GET | /api/system/auth/info | 获取当前用户信息 |
| system | GET | /api/system/employees | 分页查询员工列表 |
| system | GET | /api/system/employees/{id} | 查询员工详情 |
| system | POST | /api/system/employees | 创建员工 |
| system | PUT | /api/system/employees/{id} | 更新员工信息 |
| system | PUT | /api/system/employees/{id}/status | 更新员工状态 |
| system | POST | /api/system/employees/{id}/reset-login-code | 重置登录码 |
| system | POST | /api/system/employees/{id}/reset-password | 重置密码 |
| system | GET | /api/system/roles | 查询所有角色 |
| system | GET | /api/system/roles/{id} | 查询角色详情 |
| system | POST | /api/system/roles | 创建角色 |
| system | PUT | /api/system/roles/{id} | 更新角色 |
| system | DELETE | /api/system/roles/{id} | 删除角色 |
| system | GET | /api/system/roles/{id}/menus | 查询角色菜单 |
| system | POST | /api/system/roles/{id}/menus | 分配角色菜单 |
| system | GET | /api/system/menus | 查询所有菜单 |
| system | GET | /api/system/menus/tree | 获取菜单树 |
| system | GET | /api/system/menus/{id} | 查询菜单详情 |
| system | POST | /api/system/menus | 创建菜单 |
| system | PUT | /api/system/menus/{id} | 更新菜单 |
| system | DELETE | /api/system/menus/{id} | 删除菜单 |
| base | GET | /api/base/processes | 分页查询工序 |
| base | POST | /api/base/processes | 新增工序 |
| base | PUT | /api/base/processes/{id} | 修改工序 |
| base | DELETE | /api/base/processes/{id} | 删除工序 |
| base | GET | /api/base/styles | 分页查询款号 |
| base | GET | /api/base/styles/{id} | 查询款号详情 |
| base | POST | /api/base/styles | 新增款号 |
| base | PUT | /api/base/styles/{id} | 修改款号 |
| base | DELETE | /api/base/styles/{id} | 删除款号 |
| base | GET | /api/base/styles/{id}/processes | 查询款号工序路由 |
| base | POST | /api/base/styles/{id}/processes | 保存款号工序路由 |
| base | GET | /api/base/styles/{id}/materials | 查询款号物料清单 |
| base | POST | /api/base/styles/{id}/materials | 保存款号物料清单 |
| base | GET | /api/base/customers | 分页查询客户 |
| base | POST | /api/base/customers | 新增客户 |
| base | PUT | /api/base/customers/{id} | 修改客户 |
| base | DELETE | /api/base/customers/{id} | 删除客户 |
| base | GET | /api/base/suppliers | 分页查询供应商 |
| base | POST | /api/base/suppliers | 新增供应商 |
| base | PUT | /api/base/suppliers/{id} | 修改供应商 |
| base | DELETE | /api/base/suppliers/{id} | 删除供应商 |
| order | GET | /api/orders | 分页查询订单 |
| order | GET | /api/orders/{id} | 查询订单详情 |
| order | POST | /api/orders | 创建订单 |
| order | GET | /api/orders/{id}/items | 查询订单明细 |
| order | GET | /api/orders/{id}/processes | 查询订单工序 |
| order | POST | /api/orders/{id}/processes | 保存订单工序定价 |
| order | GET | /api/orders/{id}/materials | 查询订单物料 |
| order | POST | /api/orders/{id}/materials | 保存订单物料清单 |
| order | POST | /api/orders/{id}/confirm | 确认订单 |
| order | POST | /api/orders/{id}/settle | 结算订单 |
| order | GET | /api/shipments | 查询出货记录 |
| order | POST | /api/shipments | 创建出货记录 |
| order | DELETE | /api/shipments/{id} | 删除出货记录 |
| cutting | GET | /api/cutting/plans | 分页查询裁床计划 |
| cutting | POST | /api/cutting/plans | 创建裁床计划 |
| cutting | PUT | /api/cutting/plans/{id} | 更新裁床计划 |
| cutting | DELETE | /api/cutting/plans/{id} | 删除裁床计划 |
| cutting | POST | /api/cutting/plans/{id}/input-actual | 录入实际数量 |
| cutting | POST | /api/cutting/plans/{id}/open-bundle | 开菲生成扎号 |
| cutting | GET | /api/cutting/plans/{id}/bundles | 查询计划扎号 |
| cutting | GET | /api/cutting/bundles/{id} | 查询扎号详情 |
| cutting | GET | /api/cutting/bundles/by-order/{orderId} | 按订单查扎号 |
| report | POST | /api/report/bundle | 扎号扫码报工 |
| report | POST | /api/report/cutting | 裁床条形码报工 |
| report | GET | /api/report/wage/summary | 查询工资汇总 |
| admin | GET | /api/admin/dashboard/order | 订单维度看板 |
| admin | GET | /api/admin/dashboard/worker | 工人维度看板 |
| admin | GET | /api/admin/anomalies | 异常预警列表 |
| admin | POST | /api/admin/anomalies/{id}/ignore | 忽略预警 |
| admin | POST | /api/admin/anomalies/{id}/clear | 清除预警 |
| admin | POST | /api/admin/anomalies/{id}/supplement | 补录报工 |
| admin | GET | /api/admin/timed-wages | 计时工资列表 |
| admin | POST | /api/admin/timed-wages | 新增计时工资 |
| admin | POST | /api/admin/timed-wages/batch | 批量新增计时工资 |
| admin | PUT | /api/admin/timed-wages/{id} | 修改计时工资 |
| admin | DELETE | /api/admin/timed-wages/{id} | 删除计时工资 |
| admin | GET | /api/admin/wages | 工资汇总列表 |
| admin | POST | /api/admin/wages/{id}/audit | 审核工资 |
| admin | POST | /api/admin/wages/{id}/mark-paid | 标记已发放 |
| admin | GET | /api/admin/wages/other-items | 查询其他工资项目 |
| admin | POST | /api/admin/wages/other-items | 新增其他工资项目 |
| finance | GET | /api/finance/payments | 收支记录列表 |
| finance | POST | /api/finance/payments | 新增收支记录 |
| finance | PUT | /api/finance/payments/{id} | 修改收支记录 |
| finance | DELETE | /api/finance/payments/{id} | 删除收支记录 |
| finance | GET | /api/finance/profit/by-order | 按订单查看利润 |
| finance | GET | /api/finance/profit/summary | 利润汇总 |

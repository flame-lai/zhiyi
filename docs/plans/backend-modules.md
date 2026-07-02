# 后端业务模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现全部7个后端业务模块的Controller、Service、Mapper、Entity，完成所有REST API。

**Architecture:** 单体模块化，每个模块独立package，共享common层。所有业务表含factory_id，MyBatis-Plus自动追加租户条件。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus 3.5, Sa-Token, Java 17

---

## 模块实现顺序

```
1. system（认证、员工、角色、菜单）← 基础设施阶段已完成
2. base（工序、款号、客户、供应商）
3. order（订单、明细、工序定价、物料、出货）
4. cutting（裁床计划、扎号、打印）
5. report（扫码报工、工资查询）
6. admin（看板、预警、计时工资、工资管理）
7. finance（成本、收入、利润、结算）
```

---

## Module 1: system 系统管理（补充）

### Task 1: 员工管理Controller

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/controller/EmployeeController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/service/EmployeeService.java`

- [ ] **Step 1: 创建EmployeeService**

```java
package com.zhiyi.erp.module.system.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.common.util.EmployeeNoGenerator;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmployeeService extends ServiceImpl<EmployeeMapper, Employee> {

    @Autowired
    private EmployeeNoGenerator employeeNoGenerator;

    public Page<Employee> page(Page<Employee> page, String keyword) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Employee::getName, keyword)
                    .or()
                    .like(Employee::getEmployeeNo, keyword);
        }
        wrapper.orderByDesc(Employee::getCreatedAt);
        return page(page, wrapper);
    }

    public Employee create(Employee employee) {
        // 自动生成工号
        if (employee.getEmployeeNo() == null || employee.getEmployeeNo().isEmpty()) {
            employee.setEmployeeNo(employeeNoGenerator.generate(TenantContext.get()));
        }
        // 登录账号默认等于工号
        if (employee.getLoginAccount() == null || employee.getLoginAccount().isEmpty()) {
            employee.setLoginAccount(employee.getEmployeeNo());
        }
        // 默认密码
        if (employee.getLoginPassword() != null && !employee.getLoginPassword().isEmpty()) {
            employee.setLoginPassword(BCrypt.hashpw(employee.getLoginPassword()));
        } else {
            employee.setLoginPassword(BCrypt.hashpw("123456"));
        }
        // 生成登录码
        if (employee.getLoginCode() == null || employee.getLoginCode().isEmpty()) {
            employee.setLoginCode(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        employee.setFactoryId(TenantContext.get());
        save(employee);
        return employee;
    }

    public void updateStatus(Long id, Integer status) {
        Employee employee = getById(id);
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        employee.setStatus(status);
        updateById(employee);
    }

    public void resetLoginCode(Long id) {
        Employee employee = getById(id);
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        employee.setLoginCode(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        updateById(employee);
    }

    public void resetPassword(Long id, String newPassword) {
        Employee employee = getById(id);
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        employee.setLoginPassword(BCrypt.hashpw(newPassword != null ? newPassword : "123456"));
        updateById(employee);
    }
}
```

- [ ] **Step 2: 创建EmployeeController**

```java
package com.zhiyi.erp.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.service.EmployeeService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public R<Page<Employee>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return R.ok(employeeService.page(new Page<>(page, size), keyword));
    }

    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id) {
        return R.ok(employeeService.getById(id));
    }

    @PostMapping
    public R<Employee> create(@RequestBody Employee employee) {
        return R.ok(employeeService.create(employee));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Employee employee) {
        employee.setId(id);
        employeeService.updateById(employee);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        employeeService.updateStatus(id, status);
        return R.ok();
    }

    @PostMapping("/{id}/reset-login-code")
    public R<String> resetLoginCode(@PathVariable Long id) {
        employeeService.resetLoginCode(id);
        Employee emp = employeeService.getById(id);
        return R.ok(emp.getLoginCode());
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestParam(defaultValue = "123456") String newPassword) {
        employeeService.resetPassword(id, newPassword);
        return R.ok();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/system/
git commit -m "feat: add EmployeeController with CRUD, status toggle, reset login code/password"
```

---

### Task 2: 角色管理Controller

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/controller/RoleController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/service/RoleService.java`

- [ ] **Step 1: 创建RoleService**

```java
package com.zhiyi.erp.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.entity.RoleMenu;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import com.zhiyi.erp.module.system.mapper.RoleMapper;
import com.zhiyi.erp.module.system.mapper.RoleMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    public List<Role> listAll() {
        return list(new LambdaQueryWrapper<Role>()
                .eq(Role::getFactoryId, TenantContext.get())
                .orderByAsc(Role::getSort));
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        // 检查是否有员工关联
        long count = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>().eq(Employee::getRoleId, id));
        if (count > 0) {
            throw new BizException("该角色下有" + count + "名员工，请先转移员工");
        }
        removeById(id);
        // 删除角色菜单关联
        roleMenuMapper.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 先删除原有权限
        roleMenuMapper.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        // 添加新权限
        for (Long menuId : menuIds) {
            RoleMenu rm = new RoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    public List<Long> getRoleMenuIds(Long roleId) {
        List<RoleMenu> list = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        return list.stream().map(RoleMenu::getMenuId).toList();
    }
}
```

- [ ] **Step 2: 创建RoleController**

```java
package com.zhiyi.erp.module.system.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public R<List<Role>> list() {
        return R.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    public R<Role> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @PostMapping
    public R<Void> create(@RequestBody Role role) {
        roleService.save(role);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleService.updateById(role);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    @GetMapping("/{id}/menus")
    public R<List<Long>> getRoleMenus(@PathVariable Long id) {
        return R.ok(roleService.getRoleMenuIds(id));
    }

    @PostMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/system/
git commit -m "feat: add RoleController with CRUD and permission assignment"
```

---

### Task 3: 菜单管理Controller

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/controller/MenuController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/system/service/MenuService.java`

- [ ] **Step 1: 创建MenuService**

```java
package com.zhiyi.erp.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.module.system.entity.Menu;
import com.zhiyi.erp.module.system.entity.RoleMenu;
import com.zhiyi.erp.module.system.mapper.MenuMapper;
import com.zhiyi.erp.module.system.mapper.RoleMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuService extends ServiceImpl<MenuMapper, Menu> {

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    public List<Menu> listAll() {
        return list(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort));
    }

    public List<Map<String, Object>> getTree() {
        List<Menu> allMenus = listAll();
        return buildTree(allMenus, 0L);
    }

    private List<Map<String, Object>> buildTree(List<Menu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    Map<String, Object> node = new java.util.LinkedHashMap<>();
                    node.put("id", m.getId());
                    node.put("name", m.getName());
                    node.put("code", m.getCode());
                    node.put("path", m.getPath());
                    node.put("component", m.getComponent());
                    node.put("icon", m.getIcon());
                    node.put("type", m.getType());
                    node.put("permission", m.getPermission());
                    node.put("sort", m.getSort());
                    node.put("status", m.getStatus());
                    node.put("children", buildTree(menus, m.getId()));
                    return node;
                })
                .toList();
    }

    public void deleteMenu(Long id) {
        // 检查是否有子菜单
        long count = count(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
        if (count > 0) {
            throw new BizException("该菜单下有子菜单，请先删除子菜单");
        }
        removeById(id);
    }

    public List<Menu> getMenusByRoleId(Long roleId) {
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        if (roleMenus.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).toList();
        return listByIds(menuIds);
    }
}
```

- [ ] **Step 2: 创建MenuController**

```java
package com.zhiyi.erp.module.system.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Menu;
import com.zhiyi.erp.module.system.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping
    public R<List<Menu>> list() {
        return R.ok(menuService.listAll());
    }

    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(menuService.getTree());
    }

    @GetMapping("/{id}")
    public R<Menu> getById(@PathVariable Long id) {
        return R.ok(menuService.getById(id));
    }

    @PostMapping
    public R<Void> create(@RequestBody Menu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Menu menu) {
        menu.setId(id);
        menuService.updateById(menu);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return R.ok();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/system/
git commit -m "feat: add MenuController with tree structure and CRUD"
```

---

## Module 2: base 基础配置

### Task 4: 基础配置模块 - 实体与Mapper

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/entity/Process.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/entity/Style.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/entity/StyleProcess.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/entity/StyleMaterial.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/entity/Customer.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/entity/Supplier.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/mapper/ProcessMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/mapper/StyleMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/mapper/StyleProcessMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/mapper/StyleMaterialMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/mapper/CustomerMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/mapper/SupplierMapper.java`

- [ ] **Step 1: 创建实体类**

```java
// Process.java
package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("process")
public class Process {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String category;
    private String name;
    private BigDecimal refPrice;
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}

// Style.java
package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("style")
public class Style {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String styleCode;
    private String styleName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}

// StyleProcess.java
package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("style_process")
public class StyleProcess {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long styleId;
    private Long processId;
    private BigDecimal refPrice;
    private Integer sort;
}

// StyleMaterial.java
package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("style_material")
public class StyleMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long styleId;
    private String materialName;
    private Integer type;
    private BigDecimal unitQty;
    private String unit;
}

// Customer.java
package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String customerCode;
    private String name;
    private String contact;
    private String phone;
    private String address;
    private String settleType;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}

// Supplier.java
package com.zhiyi.erp.module.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("supplier")
public class Supplier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String supplierCode;
    private String name;
    private String contact;
    private String phone;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 2: 创建Mapper接口**

```java
// ProcessMapper.java
package com.zhiyi.erp.module.base.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.base.entity.Process;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface ProcessMapper extends BaseMapper<Process> {}

// StyleMapper.java
package com.zhiyi.erp.module.base.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.base.entity.Style;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface StyleMapper extends BaseMapper<Style> {}

// StyleProcessMapper.java
package com.zhiyi.erp.module.base.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.base.entity.StyleProcess;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface StyleProcessMapper extends BaseMapper<StyleProcess> {}

// StyleMaterialMapper.java
package com.zhiyi.erp.module.base.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.base.entity.StyleMaterial;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface StyleMaterialMapper extends BaseMapper<StyleMaterial> {}

// CustomerMapper.java
package com.zhiyi.erp.module.base.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.base.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {}

// SupplierMapper.java
package com.zhiyi.erp.module.base.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.base.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {}
```

- [ ] **Step 3: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/base/
git commit -m "feat: add base module entities and mappers (Process, Style, Customer, Supplier)"
```

---

### Task 5: 基础配置模块 - Service与Controller

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/service/ProcessService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/service/StyleService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/service/CustomerService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/service/SupplierService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/controller/ProcessController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/controller/StyleController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/controller/CustomerController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/base/controller/SupplierController.java`

- [ ] **Step 1: 创建Service类**

```java
// ProcessService.java
package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Process;
import com.zhiyi.erp.module.base.mapper.ProcessMapper;
import org.springframework.stereotype.Service;

@Service
public class ProcessService extends ServiceImpl<ProcessMapper, Process> {
    public Page<Process> page(Page<Process> page, String category) {
        LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Process::getFactoryId, TenantContext.get());
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Process::getCategory, category);
        }
        wrapper.orderByAsc(Process::getSort);
        return page(page, wrapper);
    }
}

// StyleService.java
package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Style;
import com.zhiyi.erp.module.base.entity.StyleMaterial;
import com.zhiyi.erp.module.base.entity.StyleProcess;
import com.zhiyi.erp.module.base.mapper.StyleMapper;
import com.zhiyi.erp.module.base.mapper.StyleMaterialMapper;
import com.zhiyi.erp.module.base.mapper.StyleProcessMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StyleService extends ServiceImpl<StyleMapper, Style> {

    @Autowired
    private StyleProcessMapper styleProcessMapper;
    @Autowired
    private StyleMaterialMapper styleMaterialMapper;

    public Page<Style> page(Page<Style> page, String keyword) {
        LambdaQueryWrapper<Style> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Style::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Style::getStyleCode, keyword).or().like(Style::getStyleName, keyword));
        }
        wrapper.orderByDesc(Style::getCreatedAt);
        return page(page, wrapper);
    }

    public List<StyleProcess> getStyleProcesses(Long styleId) {
        return styleProcessMapper.selectList(
                new LambdaQueryWrapper<StyleProcess>()
                        .eq(StyleProcess::getStyleId, styleId)
                        .orderByAsc(StyleProcess::getSort));
    }

    public List<StyleMaterial> getStyleMaterials(Long styleId) {
        return styleMaterialMapper.selectList(
                new LambdaQueryWrapper<StyleMaterial>()
                        .eq(StyleMaterial::getStyleId, styleId));
    }

    @Transactional
    public void saveStyleProcesses(Long styleId, List<StyleProcess> processes) {
        styleProcessMapper.delete(
                new LambdaQueryWrapper<StyleProcess>().eq(StyleProcess::getStyleId, styleId));
        for (int i = 0; i < processes.size(); i++) {
            StyleProcess sp = processes.get(i);
            sp.setStyleId(styleId);
            sp.setSort(i + 1);
            styleProcessMapper.insert(sp);
        }
    }

    @Transactional
    public void saveStyleMaterials(Long styleId, List<StyleMaterial> materials) {
        styleMaterialMapper.delete(
                new LambdaQueryWrapper<StyleMaterial>().eq(StyleMaterial::getStyleId, styleId));
        for (StyleMaterial sm : materials) {
            sm.setStyleId(styleId);
            styleMaterialMapper.insert(sm);
        }
    }
}

// CustomerService.java
package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Customer;
import com.zhiyi.erp.module.base.mapper.CustomerMapper;
import org.springframework.stereotype.Service;

@Service
public class CustomerService extends ServiceImpl<CustomerMapper, Customer> {
    public Page<Customer> page(Page<Customer> page, String keyword) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Customer::getName, keyword).or().like(Customer::getContact, keyword));
        }
        wrapper.orderByDesc(Customer::getCreatedAt);
        return page(page, wrapper);
    }
}

// SupplierService.java
package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Supplier;
import com.zhiyi.erp.module.base.mapper.SupplierMapper;
import org.springframework.stereotype.Service;

@Service
public class SupplierService extends ServiceImpl<SupplierMapper, Supplier> {
    public Page<Supplier> page(Page<Supplier> page, String keyword) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Supplier::getName, keyword).or().like(Supplier::getContact, keyword));
        }
        wrapper.orderByDesc(Supplier::getCreatedAt);
        return page(page, wrapper);
    }
}
```

- [ ] **Step 2: 创建Controller类**

```java
// ProcessController.java
package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Process;
import com.zhiyi.erp.module.base.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/processes")
public class ProcessController {
    @Autowired
    private ProcessService processService;

    @GetMapping
    public R<Page<Process>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category) {
        return R.ok(processService.page(new Page<>(page, size), category));
    }

    @PostMapping
    public R<Void> create(@RequestBody Process process) {
        processService.save(process);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Process process) {
        process.setId(id);
        processService.updateById(process);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        processService.removeById(id);
        return R.ok();
    }
}

// StyleController.java
package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Style;
import com.zhiyi.erp.module.base.entity.StyleMaterial;
import com.zhiyi.erp.module.base.entity.StyleProcess;
import com.zhiyi.erp.module.base.service.StyleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/base/styles")
public class StyleController {
    @Autowired
    private StyleService styleService;

    @GetMapping
    public R<Page<Style>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return R.ok(styleService.page(new Page<>(page, size), keyword));
    }

    @GetMapping("/{id}")
    public R<Style> getById(@PathVariable Long id) {
        return R.ok(styleService.getById(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody Style style) {
        styleService.save(style);
        return R.ok(style.getId());
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Style style) {
        style.setId(id);
        styleService.updateById(style);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        styleService.removeById(id);
        return R.ok();
    }

    @GetMapping("/{id}/processes")
    public R<List<StyleProcess>> getProcesses(@PathVariable Long id) {
        return R.ok(styleService.getStyleProcesses(id));
    }

    @PostMapping("/{id}/processes")
    public R<Void> saveProcesses(@PathVariable Long id, @RequestBody List<StyleProcess> processes) {
        styleService.saveStyleProcesses(id, processes);
        return R.ok();
    }

    @GetMapping("/{id}/materials")
    public R<List<StyleMaterial>> getMaterials(@PathVariable Long id) {
        return R.ok(styleService.getStyleMaterials(id));
    }

    @PostMapping("/{id}/materials")
    public R<Void> saveMaterials(@PathVariable Long id, @RequestBody List<StyleMaterial> materials) {
        styleService.saveStyleMaterials(id, materials);
        return R.ok();
    }
}

// CustomerController.java
package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Customer;
import com.zhiyi.erp.module.base.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/customers")
public class CustomerController {
    @Autowired
    private CustomerService customerService;

    @GetMapping
    public R<Page<Customer>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return R.ok(customerService.page(new Page<>(page, size), keyword));
    }

    @PostMapping
    public R<Void> create(@RequestBody Customer customer) {
        customerService.save(customer);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        customerService.updateById(customer);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        customerService.removeById(id);
        return R.ok();
    }
}

// SupplierController.java
package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Supplier;
import com.zhiyi.erp.module.base.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/suppliers")
public class SupplierController {
    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public R<Page<Supplier>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return R.ok(supplierService.page(new Page<>(page, size), keyword));
    }

    @PostMapping
    public R<Void> create(@RequestBody Supplier supplier) {
        supplierService.save(supplier);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplier.setId(id);
        supplierService.updateById(supplier);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        supplierService.removeById(id);
        return R.ok();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/base/
git commit -m "feat: add base module services and controllers (Process, Style, Customer, Supplier)"
```

---

## Module 3: order 订单管理

### Task 6: 订单模块 - 实体与Mapper

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/entity/Order.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/entity/OrderItem.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/entity/OrderProcess.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/entity/OrderMaterial.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/entity/Shipment.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/mapper/OrderMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/mapper/OrderItemMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/mapper/OrderProcessMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/mapper/OrderMaterialMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/mapper/ShipmentMapper.java`

- [ ] **Step 1: 创建实体类**

```java
// Order.java
package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private String orderNo;
    private Long customerId;
    private BigDecimal totalAmount;
    private LocalDate deliveryDate;
    private Integer status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private Long createdBy;
    @TableLogic
    private Integer deleted;
}

// OrderItem.java
package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long styleId;
    private String color;
    private String size;
    private Integer qty;
    private BigDecimal unitPrice;
}

// OrderProcess.java
package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("order_process")
public class OrderProcess {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long processId;
    private BigDecimal processPrice;
    private Integer sort;
}

// OrderMaterial.java
package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("order_material")
public class OrderMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String materialName;
    private Integer type;
    private BigDecimal unitQty;
    private String unit;
}

// Shipment.java
package com.zhiyi.erp.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("shipment")
public class Shipment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long orderId;
    private Long orderItemId;
    private Integer qty;
    private LocalDate shipmentDate;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
}
```

- [ ] **Step 2: 创建Mapper接口**

```java
// OrderMapper.java
package com.zhiyi.erp.module.order.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface OrderMapper extends BaseMapper<Order> {}

// OrderItemMapper.java
package com.zhiyi.erp.module.order.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {}

// OrderProcessMapper.java
package com.zhiyi.erp.module.order.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.order.entity.OrderProcess;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface OrderProcessMapper extends BaseMapper<OrderProcess> {}

// OrderMaterialMapper.java
package com.zhiyi.erp.module.order.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.order.entity.OrderMaterial;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface OrderMaterialMapper extends BaseMapper<OrderMaterial> {}

// ShipmentMapper.java
package com.zhiyi.erp.module.order.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.order.entity.Shipment;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface ShipmentMapper extends BaseMapper<Shipment> {}
```

- [ ] **Step 3: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/order/
git commit -m "feat: add order module entities and mappers"
```

---

### Task 7: 订单模块 - Service与Controller

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/service/OrderService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/service/ShipmentService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/controller/OrderController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/order/controller/ShipmentController.java`

- [ ] **Step 1: 创建OrderService**

```java
package com.zhiyi.erp.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.common.util.OrderNoGenerator;
import com.zhiyi.erp.module.base.entity.StyleProcess;
import com.zhiyi.erp.module.base.service.StyleService;
import com.zhiyi.erp.module.order.entity.*;
import com.zhiyi.erp.module.order.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderProcessMapper orderProcessMapper;
    @Autowired
    private OrderMaterialMapper orderMaterialMapper;
    @Autowired
    private StyleService styleService;

    public Page<Order> page(Page<Order> page, String keyword, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Order::getOrderNo, keyword);
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return page(page, wrapper);
    }

    @Transactional
    public Long createOrder(Order order, List<OrderItem> items) {
        order.setOrderNo(OrderNoGenerator.generate());
        order.setFactoryId(TenantContext.get());
        order.setStatus(0);
        save(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);

            // 从款号模板带出工序
            List<StyleProcess> styleProcesses = styleService.getStyleProcesses(item.getStyleId());
            for (StyleProcess sp : styleProcesses) {
                OrderProcess op = new OrderProcess();
                op.setOrderId(order.getId());
                op.setProcessId(sp.getProcessId());
                op.setProcessPrice(sp.getRefPrice());
                op.setSort(sp.getSort());
                orderProcessMapper.insert(op);
            }
        }

        return order.getId();
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    public List<OrderProcess> getOrderProcesses(Long orderId) {
        return orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>()
                        .eq(OrderProcess::getOrderId, orderId)
                        .orderByAsc(OrderProcess::getSort));
    }

    @Transactional
    public void saveOrderProcesses(Long orderId, List<OrderProcess> processes) {
        orderProcessMapper.delete(
                new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getOrderId, orderId));
        for (int i = 0; i < processes.size(); i++) {
            OrderProcess op = processes.get(i);
            op.setOrderId(orderId);
            op.setSort(i + 1);
            orderProcessMapper.insert(op);
        }
    }

    public List<OrderMaterial> getOrderMaterials(Long orderId) {
        return orderMaterialMapper.selectList(
                new LambdaQueryWrapper<OrderMaterial>().eq(OrderMaterial::getOrderId, orderId));
    }

    @Transactional
    public void saveOrderMaterials(Long orderId, List<OrderMaterial> materials) {
        orderMaterialMapper.delete(
                new LambdaQueryWrapper<OrderMaterial>().eq(OrderMaterial::getOrderId, orderId));
        for (OrderMaterial om : materials) {
            om.setOrderId(orderId);
            orderMaterialMapper.insert(om);
        }
    }

    public void confirmOrder(Long id) {
        Order order = getById(id);
        if (order == null) throw new BizException("订单不存在");
        if (order.getStatus() != 0) throw new BizException("当前状态不可确认");
        order.setStatus(1);
        updateById(order);
    }

    public void settleOrder(Long id) {
        Order order = getById(id);
        if (order == null) throw new BizException("订单不存在");
        order.setStatus(5);
        updateById(order);
    }
}
```

- [ ] **Step 2: 创建ShipmentService**

```java
package com.zhiyi.erp.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.entity.Shipment;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import com.zhiyi.erp.module.order.mapper.ShipmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentService extends ServiceImpl<ShipmentMapper, Shipment> {

    @Autowired
    private OrderMapper orderMapper;

    public List<Shipment> listByOrder(Long orderId) {
        return list(new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getOrderId, orderId)
                .orderByDesc(Shipment::getShipmentDate));
    }

    public void createShipment(Shipment shipment) {
        shipment.setFactoryId(TenantContext.get());
        save(shipment);

        // 更新订单状态
        Order order = orderMapper.selectById(shipment.getOrderId());
        if (order != null && order.getStatus() < 2) {
            order.setStatus(2); // 生产中
            orderMapper.updateById(order);
        }
    }
}
```

- [ ] **Step 3: 创建Controller类**

```java
// OrderController.java
package com.zhiyi.erp.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.order.entity.*;
import com.zhiyi.erp.module.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping
    public R<Page<Order>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(orderService.page(new Page<>(page, size), keyword, status));
    }

    @GetMapping("/{id}")
    public R<Order> getById(@PathVariable Long id) {
        return R.ok(orderService.getById(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody CreateOrderDTO dto) {
        return R.ok(orderService.createOrder(dto.getOrder(), dto.getItems()));
    }

    @GetMapping("/{id}/items")
    public R<List<OrderItem>> getItems(@PathVariable Long id) {
        return R.ok(orderService.getOrderItems(id));
    }

    @GetMapping("/{id}/processes")
    public R<List<OrderProcess>> getProcesses(@PathVariable Long id) {
        return R.ok(orderService.getOrderProcesses(id));
    }

    @PostMapping("/{id}/processes")
    public R<Void> saveProcesses(@PathVariable Long id, @RequestBody List<OrderProcess> processes) {
        orderService.saveOrderProcesses(id, processes);
        return R.ok();
    }

    @GetMapping("/{id}/materials")
    public R<List<OrderMaterial>> getMaterials(@PathVariable Long id) {
        return R.ok(orderService.getOrderMaterials(id));
    }

    @PostMapping("/{id}/materials")
    public R<Void> saveMaterials(@PathVariable Long id, @RequestBody List<OrderMaterial> materials) {
        orderService.saveOrderMaterials(id, materials);
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        orderService.confirmOrder(id);
        return R.ok();
    }

    @PostMapping("/{id}/settle")
    public R<Void> settle(@PathVariable Long id) {
        orderService.settleOrder(id);
        return R.ok();
    }

    public static class CreateOrderDTO {
        private Order order;
        private List<OrderItem> items;
        public Order getOrder() { return order; }
        public void setOrder(Order order) { this.order = order; }
        public List<OrderItem> getItems() { return items; }
        public void setItems(List<OrderItem> items) { this.items = items; }
    }
}

// ShipmentController.java
package com.zhiyi.erp.module.order.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.order.entity.Shipment;
import com.zhiyi.erp.module.order.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    @Autowired
    private ShipmentService shipmentService;

    @GetMapping
    public R<List<Shipment>> list(@RequestParam Long orderId) {
        return R.ok(shipmentService.listByOrder(orderId));
    }

    @PostMapping
    public R<Void> create(@RequestBody Shipment shipment) {
        shipmentService.createShipment(shipment);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        shipmentService.removeById(id);
        return R.ok();
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/order/
git commit -m "feat: add order module services and controllers with full CRUD"
```

---

## Module 4: cutting 裁床开菲

### Task 8: 裁床模块 - 实体、Service与Controller

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/cutting/entity/CuttingPlan.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/cutting/entity/Bundle.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/cutting/mapper/CuttingPlanMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/cutting/mapper/BundleMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/cutting/service/CuttingService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/cutting/controller/CuttingPlanController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/cutting/controller/BundleController.java`

- [ ] **Step 1: 创建实体类**

```java
// CuttingPlan.java
package com.zhiyi.erp.module.cutting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("cutting_plan")
public class CuttingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long orderItemId;
    private Integer bedNo;
    private Integer planQty;
    private Integer actualQty;
    private Integer qtyPerBundle;
    private Integer bundleCount;
    private LocalDate cuttingDate;
    private String barcode;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}

// Bundle.java
package com.zhiyi.erp.module.cutting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("bundle")
public class Bundle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long cuttingPlanId;
    private Long orderId;
    private Long orderItemId;
    private String bundleNo;
    private String globalBundleNo;
    private Integer qty;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建Mapper和Service**

```java
// CuttingPlanMapper.java
package com.zhiyi.erp.module.cutting.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface CuttingPlanMapper extends BaseMapper<CuttingPlan> {}

// BundleMapper.java
package com.zhiyi.erp.module.cutting.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface BundleMapper extends BaseMapper<Bundle> {}

// CuttingService.java
package com.zhiyi.erp.module.cutting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanMapper;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CuttingService extends ServiceImpl<CuttingPlanMapper, CuttingPlan> {

    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private OrderMapper orderMapper;

    public Page<CuttingPlan> page(Page<CuttingPlan> page, Long orderItemId) {
        LambdaQueryWrapper<CuttingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CuttingPlan::getFactoryId, TenantContext.get());
        if (orderItemId != null) {
            wrapper.eq(CuttingPlan::getOrderItemId, orderItemId);
        }
        wrapper.orderByDesc(CuttingPlan::getCreatedAt);
        return page(page, wrapper);
    }

    public void inputActualQty(Long id, Integer actualQty) {
        CuttingPlan plan = getById(id);
        if (plan == null) throw new BizException("裁床计划不存在");
        plan.setActualQty(actualQty);
        updateById(plan);
    }

    @Transactional
    public List<Bundle> openBundle(Long id) {
        CuttingPlan plan = getById(id);
        if (plan == null) throw new BizException("裁床计划不存在");
        if (plan.getActualQty() == null || plan.getActualQty() <= 0) {
            throw new BizException("请先录入实际件数");
        }
        if (plan.getStatus() >= 2) throw new BizException("已开菲");

        int actualQty = plan.getActualQty();
        int qtyPerBundle = plan.getQtyPerBundle();
        int bundleCount = (int) Math.ceil((double) actualQty / qtyPerBundle);
        plan.setBundleCount(bundleCount);
        plan.setStatus(2);
        updateById(plan);

        // 获取订单号
        Order order = orderMapper.selectById(
                bundleMapper.selectOne(
                        new LambdaQueryWrapper<Bundle>().eq(Bundle::getCuttingPlanId, id).last("LIMIT 1")
                ) != null ? null : null
        );

        List<Bundle> bundles = new ArrayList<>();
        for (int i = 1; i <= bundleCount; i++) {
            int qty = (i < bundleCount) ? qtyPerBundle : (actualQty - (bundleCount - 1) * qtyPerBundle);
            Bundle bundle = new Bundle();
            bundle.setFactoryId(TenantContext.get());
            bundle.setCuttingPlanId(id);
            bundle.setOrderId(plan.getOrderItemId()); // 需要从order_item关联
            bundle.setOrderItemId(plan.getOrderItemId());
            bundle.setBundleNo(plan.getBedNo() + "-" + String.format("%03d", i));
            bundle.setGlobalBundleNo(plan.getBarcode() + "-" + plan.getBedNo() + "-" + String.format("%03d", i));
            bundle.setQty(qty);
            bundle.setStatus(0);
            bundleMapper.insert(bundle);
            bundles.add(bundle);
        }

        return bundles;
    }

    public List<Bundle> getBundles(Long cuttingPlanId) {
        return bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getCuttingPlanId, cuttingPlanId)
                        .orderByAsc(Bundle::getBundleNo));
    }

    public Bundle getBundleById(Long id) {
        return bundleMapper.selectById(id);
    }

    public List<Bundle> getBundlesByOrderId(Long orderId) {
        return bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getOrderId, orderId)
                        .orderByAsc(Bundle::getBundleNo));
    }
}
```

- [ ] **Step 3: 创建Controller**

```java
// CuttingPlanController.java
package com.zhiyi.erp.module.cutting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.service.CuttingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cutting/plans")
public class CuttingPlanController {
    @Autowired
    private CuttingService cuttingService;

    @GetMapping
    public R<Page<CuttingPlan>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long orderItemId) {
        return R.ok(cuttingService.page(new Page<>(page, size), orderItemId));
    }

    @PostMapping
    public R<Void> create(@RequestBody CuttingPlan plan) {
        cuttingService.save(plan);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody CuttingPlan plan) {
        plan.setId(id);
        cuttingService.updateById(plan);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        cuttingService.removeById(id);
        return R.ok();
    }

    @PostMapping("/{id}/input-actual")
    public R<Void> inputActualQty(@PathVariable Long id, @RequestParam Integer actualQty) {
        cuttingService.inputActualQty(id, actualQty);
        return R.ok();
    }

    @PostMapping("/{id}/open-bundle")
    public R<List<Bundle>> openBundle(@PathVariable Long id) {
        return R.ok(cuttingService.openBundle(id));
    }

    @GetMapping("/{id}/bundles")
    public R<List<Bundle>> getBundles(@PathVariable Long id) {
        return R.ok(cuttingService.getBundles(id));
    }
}

// BundleController.java
package com.zhiyi.erp.module.cutting.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.service.CuttingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cutting/bundles")
public class BundleController {
    @Autowired
    private CuttingService cuttingService;

    @GetMapping("/{id}")
    public R<Bundle> getById(@PathVariable Long id) {
        return R.ok(cuttingService.getBundleById(id));
    }

    @GetMapping("/by-order/{orderId}")
    public R<List<Bundle>> getByOrderId(@PathVariable Long orderId) {
        return R.ok(cuttingService.getBundlesByOrderId(orderId));
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/cutting/
git commit -m "feat: add cutting module with cutting plan, bundle generation, and open bundle logic"
```

---

## Module 5: report 报工模块

### Task 9: 报工模块 - 实体、Service与Controller

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/report/entity/WorkReport.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/report/mapper/WorkReportMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/report/service/ReportService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/report/controller/ScanReportController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/report/controller/WageController.java`

- [ ] **Step 1: 创建实体和Mapper**

```java
// WorkReport.java
package com.zhiyi.erp.module.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("work_report")
public class WorkReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long bundleId;
    private Long orderId;
    private Long processId;
    private Long employeeId;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
    private Integer reportType;
    private Integer status;
    private LocalDateTime reportTime;
    private String clearReason;
    private Integer supplementFlag;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private Long createdBy;
}

// WorkReportMapper.java
package com.zhiyi.erp.module.report.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface WorkReportMapper extends BaseMapper<WorkReport> {}
```

- [ ] **Step 2: 创建ReportService**

```java
package com.zhiyi.erp.module.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanMapper;
import com.zhiyi.erp.module.order.entity.OrderProcess;
import com.zhiyi.erp.module.order.mapper.OrderProcessMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService extends ServiceImpl<WorkReportMapper, WorkReport> {

    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private CuttingPlanMapper cuttingPlanMapper;
    @Autowired
    private OrderProcessMapper orderProcessMapper;

    /**
     * 扫扎号QR码报工
     */
    @Transactional
    public Map<String, Object> reportByBundle(Long employeeId, Long bundleId, List<Long> processIds) {
        Bundle bundle = bundleMapper.selectById(bundleId);
        if (bundle == null) throw new BizException("扎号不存在");
        if (bundle.getStatus() == 1) throw new BizException("该扎已全部完成");

        List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getOrderId, bundle.getOrderId()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;

        for (Long processId : processIds) {
            // 检查是否已报工
            long count = count(new LambdaQueryWrapper<WorkReport>()
                    .eq(WorkReport::getBundleId, bundleId)
                    .eq(WorkReport::getProcessId, processId)
                    .eq(WorkReport::getStatus, 1));
            if (count > 0) continue;

            OrderProcess op = orderProcesses.stream()
                    .filter(p -> p.getProcessId().equals(processId))
                    .findFirst()
                    .orElseThrow(() -> new BizException("工序不存在"));

            WorkReport report = new WorkReport();
            report.setFactoryId(TenantContext.get());
            report.setBundleId(bundleId);
            report.setOrderId(bundle.getOrderId());
            report.setProcessId(processId);
            report.setEmployeeId(employeeId);
            report.setQty(bundle.getQty());
            report.setPrice(op.getProcessPrice());
            report.setAmount(op.getProcessPrice().multiply(BigDecimal.valueOf(bundle.getQty())));
            report.setReportType(1);
            report.setStatus(1);
            report.setReportTime(LocalDateTime.now());
            report.setSupplementFlag(0);
            save(report);

            totalAmount = totalAmount.add(report.getAmount());
            totalQty += bundle.getQty();
        }

        // 检查是否所有工序完成
        checkBundleComplete(bundleId, bundle.getOrderId());

        Map<String, Object> result = new HashMap<>();
        result.put("qty", totalQty);
        result.put("amount", totalAmount);
        return result;
    }

    /**
     * 扫裁床条形码报工（整床裁剪）
     */
    @Transactional
    public Map<String, Object> reportByCuttingBarcode(Long employeeId, String barcode) {
        CuttingPlan plan = cuttingPlanMapper.selectOne(
                new LambdaQueryWrapper<CuttingPlan>()
                        .eq(CuttingPlan::getBarcode, barcode)
                        .eq(CuttingPlan::getFactoryId, TenantContext.get()));
        if (plan == null) throw new BizException("裁床码无效");
        if (plan.getStatus() < 2) throw new BizException("请先开菲");

        // 检查是否已报工
        List<Bundle> bundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>().eq(Bundle::getCuttingPlanId, plan.getId()));
        if (bundles.isEmpty()) throw new BizException("无扎号数据");

        // 获取裁剪工序
        List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getOrderId, bundles.get(0).getOrderId()));
        // 假设裁剪是第一道工序
        OrderProcess cuttingProcess = orderProcesses.isEmpty() ? null : orderProcesses.get(0);
        if (cuttingProcess == null) throw new BizException("无裁剪工序");

        long alreadyReported = count(new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getBundleId, bundles.get(0).getId())
                .eq(WorkReport::getProcessId, cuttingProcess.getProcessId())
                .eq(WorkReport::getStatus, 1));
        if (alreadyReported > 0) throw new BizException("该床已报工");

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;

        for (Bundle bundle : bundles) {
            WorkReport report = new WorkReport();
            report.setFactoryId(TenantContext.get());
            report.setBundleId(bundle.getId());
            report.setOrderId(bundle.getOrderId());
            report.setProcessId(cuttingProcess.getProcessId());
            report.setEmployeeId(employeeId);
            report.setQty(bundle.getQty());
            report.setPrice(cuttingProcess.getProcessPrice());
            report.setAmount(cuttingProcess.getProcessPrice().multiply(BigDecimal.valueOf(bundle.getQty())));
            report.setReportType(2);
            report.setStatus(1);
            report.setReportTime(LocalDateTime.now());
            report.setSupplementFlag(0);
            save(report);

            totalAmount = totalAmount.add(report.getAmount());
            totalQty += bundle.getQty();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("qty", totalQty);
        result.put("amount", totalAmount);
        return result;
    }

    private void checkBundleComplete(Long bundleId, Long orderId) {
        List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getOrderId, orderId));
        List<WorkReport> reports = list(new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getBundleId, bundleId)
                .eq(WorkReport::getStatus, 1));

        boolean allComplete = orderProcesses.stream()
                .allMatch(op -> reports.stream()
                        .anyMatch(r -> r.getProcessId().equals(op.getProcessId())));

        Bundle bundle = bundleMapper.selectById(bundleId);
        if (allComplete && bundle.getStatus() != 1) {
            bundle.setStatus(1);
            bundleMapper.updateById(bundle);
        }
    }

    public List<Map<String, Object>> getEmployeeWageSummary(Long employeeId, String yearMonth) {
        // 获取计件工资汇总
        List<WorkReport> reports = list(new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getEmployeeId, employeeId)
                .eq(WorkReport::getStatus, 1)
                .apply("DATE_FORMAT(report_time, '%Y-%m') = {0}", yearMonth));

        BigDecimal totalPieceWage = reports.stream()
                .map(WorkReport::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按日期分组
        Map<String, List<WorkReport>> byDate = reports.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getReportTime().toLocalDate().toString()));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPieceWage", totalPieceWage);
        summary.put("dailyDetails", byDate);
        return List.of(summary);
    }
}
```

- [ ] **Step 3: 创建Controller**

```java
// ScanReportController.java
package com.zhiyi.erp.module.report.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.report.service.ReportService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ScanReportController {
    @Autowired
    private ReportService reportService;

    @Data
    public static class BundleReportDTO {
        private Long bundleId;
        private List<Long> processIds;
    }

    @Data
    public static class CuttingReportDTO {
        private String barcode;
    }

    @PostMapping("/bundle")
    public R<Map<String, Object>> reportByBundle(@RequestBody BundleReportDTO dto) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(reportService.reportByBundle(employeeId, dto.getBundleId(), dto.getProcessIds()));
    }

    @PostMapping("/cutting")
    public R<Map<String, Object>> reportByCutting(@RequestBody CuttingReportDTO dto) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(reportService.reportByCuttingBarcode(employeeId, dto.getBarcode()));
    }
}

// WageController.java
package com.zhiyi.erp.module.report.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report/wage")
public class WageController {
    @Autowired
    private ReportService reportService;

    @GetMapping("/summary")
    public R<List<Map<String, Object>>> getSummary(
            @RequestParam(required = false) String yearMonth) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        if (yearMonth == null) {
            yearMonth = LocalDate.now().toString().substring(0, 7);
        }
        return R.ok(reportService.getEmployeeWageSummary(employeeId, yearMonth));
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/report/
git commit -m "feat: add report module with scan report (bundle + cutting barcode) and wage query"
```

---

## Module 6: admin 管理后台

### Task 10: 管理后台模块

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/entity/TimedWage.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/entity/AnomalyAlert.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/entity/WageSummary.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/entity/WageOther.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/mapper/TimedWageMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/mapper/AnomalyAlertMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/mapper/WageSummaryMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/mapper/WageOtherMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/service/DashboardService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/service/AnomalyService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/service/TimedWageService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/service/WageManageService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/controller/DashboardController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/controller/AnomalyController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/controller/TimedWageController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/controller/WageManageController.java`

- [ ] **Step 1: 创建实体类**

```java
// TimedWage.java
package com.zhiyi.erp.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("timed_wage")
public class TimedWage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long employeeId;
    private LocalDate workDate;
    private BigDecimal hours;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
    private String workContent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
}

// AnomalyAlert.java
package com.zhiyi.erp.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("anomaly_alert")
public class AnomalyAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long bundleId;
    private Long orderId;
    private Long frontProcessId;
    private Long backProcessId;
    private Long reportEmployeeId;
    private Integer status;
    private Integer handleType;
    private Long handleBy;
    private LocalDateTime handleTime;
    private String handleRemark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

// WageSummary.java
package com.zhiyi.erp.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wage_summary")
public class WageSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long employeeId;
    private String yearMonth;
    private BigDecimal pieceWage;
    private BigDecimal timedWage;
    private BigDecimal otherWage;
    private BigDecimal totalWage;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

// WageOther.java
package com.zhiyi.erp.module.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wage_other")
public class WageOther {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long employeeId;
    private String yearMonth;
    private String itemName;
    private BigDecimal amount;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
}
```

- [ ] **Step 2: 创建Mapper接口**

```java
// TimedWageMapper.java
package com.zhiyi.erp.module.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.admin.entity.TimedWage;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface TimedWageMapper extends BaseMapper<TimedWage> {}

// AnomalyAlertMapper.java
package com.zhiyi.erp.module.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.admin.entity.AnomalyAlert;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface AnomalyAlertMapper extends BaseMapper<AnomalyAlert> {}

// WageSummaryMapper.java
package com.zhiyi.erp.module.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.admin.entity.WageSummary;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface WageSummaryMapper extends BaseMapper<WageSummary> {}

// WageOtherMapper.java
package com.zhiyi.erp.module.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.admin.entity.WageOther;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface WageOtherMapper extends BaseMapper<WageOther> {}
```

- [ ] **Step 3: 创建Service类**

```java
// DashboardService.java
package com.zhiyi.erp.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.order.entity.OrderProcess;
import com.zhiyi.erp.module.order.mapper.OrderProcessMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private WorkReportMapper workReportMapper;
    @Autowired
    private OrderProcessMapper orderProcessMapper;

    public List<Map<String, Object>> getOrderDashboard(Long orderId) {
        List<Bundle> bundles;
        if (orderId != null) {
            bundles = bundleMapper.selectList(
                    new LambdaQueryWrapper<Bundle>().eq(Bundle::getOrderId, orderId));
        } else {
            bundles = bundleMapper.selectList(
                    new LambdaQueryWrapper<Bundle>().eq(Bundle::getFactoryId, TenantContext.get()));
        }

        // 按订单分组
        Map<Long, List<Bundle>> byOrder = bundles.stream()
                .collect(Collectors.groupingBy(Bundle::getOrderId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Bundle>> entry : byOrder.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("orderId", entry.getKey());
            item.put("totalBundles", entry.getValue().size());

            List<Long> bundleIds = entry.getValue().stream().map(Bundle::getId).toList();
            List<WorkReport> reports = workReportMapper.selectList(
                    new LambdaQueryWrapper<WorkReport>()
                            .in(WorkReport::getBundleId, bundleIds)
                            .eq(WorkReport::getStatus, 1));

            List<OrderProcess> processes = orderProcessMapper.selectList(
                    new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getOrderId, entry.getKey()));

            List<Map<String, Object>> processProgress = new ArrayList<>();
            for (OrderProcess op : processes) {
                long completed = reports.stream()
                        .filter(r -> r.getProcessId().equals(op.getProcessId()))
                        .map(WorkReport::getBundleId)
                        .distinct()
                        .count();
                Map<String, Object> pp = new HashMap<>();
                pp.put("processId", op.getProcessId());
                pp.put("completed", completed);
                pp.put("total", entry.getValue().size());
                processProgress.add(pp);
            }
            item.put("processProgress", processProgress);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getWorkerDashboard(String date) {
        // 简化实现：查询当日报工汇总
        List<WorkReport> reports = workReportMapper.selectList(
                new LambdaQueryWrapper<WorkReport>()
                        .eq(WorkReport::getFactoryId, TenantContext.get())
                        .eq(WorkReport::getStatus, 1)
                        .apply("DATE(report_time) = {0}", date));

        Map<Long, List<WorkReport>> byEmployee = reports.stream()
                .collect(Collectors.groupingBy(WorkReport::getEmployeeId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<WorkReport>> entry : byEmployee.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", entry.getKey());
            item.put("totalQty", entry.getValue().stream().mapToInt(WorkReport::getQty).sum());
            item.put("totalAmount", entry.getValue().stream()
                    .map(WorkReport::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            result.add(item);
        }
        return result;
    }
}

// AnomalyService.java
package com.zhiyi.erp.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.entity.AnomalyAlert;
import com.zhiyi.erp.module.admin.mapper.AnomalyAlertMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnomalyService extends ServiceImpl<AnomalyAlertMapper, AnomalyAlert> {

    @Autowired
    private WorkReportMapper workReportMapper;

    public Page<AnomalyAlert> page(Page<AnomalyAlert> page, Integer status) {
        LambdaQueryWrapper<AnomalyAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyAlert::getFactoryId, TenantContext.get());
        if (status != null) {
            wrapper.eq(AnomalyAlert::getStatus, status);
        }
        wrapper.orderByDesc(AnomalyAlert::getCreatedAt);
        return page(page, wrapper);
    }

    @Transactional
    public void ignore(Long id, String remark) {
        AnomalyAlert alert = getById(id);
        alert.setStatus(1);
        alert.setHandleType(1);
        alert.setHandleRemark(remark);
        alert.setHandleTime(LocalDateTime.now());
        updateById(alert);
    }

    @Transactional
    public void clearReport(Long id, String reason) {
        AnomalyAlert alert = getById(id);
        // 清除后置工序的报工记录
        WorkReport report = workReportMapper.selectOne(
                new LambdaQueryWrapper<WorkReport>()
                        .eq(WorkReport::getBundleId, alert.getBundleId())
                        .eq(WorkReport::getProcessId, alert.getBackProcessId())
                        .eq(WorkReport::getStatus, 1));
        if (report != null) {
            report.setStatus(0);
            report.setClearReason(reason);
            workReportMapper.updateById(report);
        }
        alert.setStatus(2);
        alert.setHandleType(2);
        alert.setHandleTime(LocalDateTime.now());
        updateById(alert);
    }

    @Transactional
    public void supplement(Long id, Long employeeId, Integer qty) {
        AnomalyAlert alert = getById(id);
        // 补录前置工序
        WorkReport report = new WorkReport();
        report.setFactoryId(TenantContext.get());
        report.setBundleId(alert.getBundleId());
        report.setOrderId(alert.getOrderId());
        report.setProcessId(alert.getFrontProcessId());
        report.setEmployeeId(employeeId);
        report.setQty(qty);
        report.setReportType(3);
        report.setStatus(1);
        report.setReportTime(LocalDateTime.now());
        report.setSupplementFlag(1);
        workReportMapper.insert(report);

        alert.setStatus(3);
        alert.setHandleType(3);
        alert.setHandleTime(LocalDateTime.now());
        updateById(alert);
    }
}

// TimedWageService.java
package com.zhiyi.erp.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.entity.TimedWage;
import com.zhiyi.erp.module.admin.mapper.TimedWageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimedWageService extends ServiceImpl<TimedWageMapper, TimedWage> {

    public Page<TimedWage> page(Page<TimedWage> page, Long employeeId, String yearMonth) {
        LambdaQueryWrapper<TimedWage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimedWage::getFactoryId, TenantContext.get());
        if (employeeId != null) {
            wrapper.eq(TimedWage::getEmployeeId, employeeId);
        }
        if (yearMonth != null) {
            wrapper.apply("DATE_FORMAT(work_date, '%Y-%m') = {0}", yearMonth);
        }
        wrapper.orderByDesc(TimedWage::getWorkDate);
        return page(page, wrapper);
    }

    public void createBatch(List<TimedWage> wages) {
        for (TimedWage wage : wages) {
            wage.setFactoryId(TenantContext.get());
            save(wage);
        }
    }
}

// WageManageService.java
package com.zhiyi.erp.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.entity.WageOther;
import com.zhiyi.erp.module.admin.entity.WageSummary;
import com.zhiyi.erp.module.admin.mapper.WageOtherMapper;
import com.zhiyi.erp.module.admin.mapper.WageSummaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WageManageService extends ServiceImpl<WageSummaryMapper, WageSummary> {

    @Autowired
    private WageOtherMapper wageOtherMapper;

    public Page<WageSummary> page(Page<WageSummary> page, String yearMonth, Integer status) {
        LambdaQueryWrapper<WageSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WageSummary::getFactoryId, TenantContext.get());
        if (yearMonth != null) {
            wrapper.eq(WageSummary::getYearMonth, yearMonth);
        }
        if (status != null) {
            wrapper.eq(WageSummary::getStatus, status);
        }
        return page(page, wrapper);
    }

    public void audit(Long id) {
        WageSummary summary = getById(id);
        summary.setStatus(1);
        updateById(summary);
    }

    public void markPaid(Long id) {
        WageSummary summary = getById(id);
        summary.setStatus(2);
        updateById(summary);
    }

    public List<WageOther> getOtherItems(Long employeeId, String yearMonth) {
        return wageOtherMapper.selectList(
                new LambdaQueryWrapper<WageOther>()
                        .eq(WageOther::getEmployeeId, employeeId)
                        .eq(WageOther::getYearMonth, yearMonth));
    }

    public void addOtherItem(WageOther item) {
        item.setFactoryId(TenantContext.get());
        wageOtherMapper.insert(item);
    }
}
```

- [ ] **Step 4: 创建Controller**

```java
// DashboardController.java
package com.zhiyi.erp.module.admin.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/order")
    public R<List<Map<String, Object>>> orderDashboard(
            @RequestParam(required = false) Long orderId) {
        return R.ok(dashboardService.getOrderDashboard(orderId));
    }

    @GetMapping("/worker")
    public R<List<Map<String, Object>>> workerDashboard(
            @RequestParam(required = false) String date) {
        if (date == null) date = LocalDate.now().toString();
        return R.ok(dashboardService.getWorkerDashboard(date));
    }
}

// AnomalyController.java
package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.entity.AnomalyAlert;
import com.zhiyi.erp.module.admin.service.AnomalyService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/anomalies")
public class AnomalyController {
    @Autowired
    private AnomalyService anomalyService;

    @GetMapping
    public R<Page<AnomalyAlert>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return R.ok(anomalyService.page(new Page<>(page, size), status));
    }

    @PostMapping("/{id}/ignore")
    public R<Void> ignore(@PathVariable Long id, @RequestParam String remark) {
        anomalyService.ignore(id, remark);
        return R.ok();
    }

    @PostMapping("/{id}/clear")
    public R<Void> clear(@PathVariable Long id, @RequestParam String reason) {
        anomalyService.clearReport(id, reason);
        return R.ok();
    }

    @PostMapping("/{id}/supplement")
    public R<Void> supplement(@PathVariable Long id, @RequestParam Long employeeId, @RequestParam Integer qty) {
        anomalyService.supplement(id, employeeId, qty);
        return R.ok();
    }
}

// TimedWageController.java
package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.entity.TimedWage;
import com.zhiyi.erp.module.admin.service.TimedWageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/timed-wages")
public class TimedWageController {
    @Autowired
    private TimedWageService timedWageService;

    @GetMapping
    public R<Page<TimedWage>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String yearMonth) {
        return R.ok(timedWageService.page(new Page<>(page, size), employeeId, yearMonth));
    }

    @PostMapping
    public R<Void> create(@RequestBody TimedWage wage) {
        timedWageService.save(wage);
        return R.ok();
    }

    @PostMapping("/batch")
    public R<Void> createBatch(@RequestBody List<TimedWage> wages) {
        timedWageService.createBatch(wages);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody TimedWage wage) {
        wage.setId(id);
        timedWageService.updateById(wage);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        timedWageService.removeById(id);
        return R.ok();
    }
}

// WageManageController.java
package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.entity.WageOther;
import com.zhiyi.erp.module.admin.entity.WageSummary;
import com.zhiyi.erp.module.admin.service.WageManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/wages")
public class WageManageController {
    @Autowired
    private WageManageService wageManageService;

    @GetMapping
    public R<Page<WageSummary>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) Integer status) {
        return R.ok(wageManageService.page(new Page<>(page, size), yearMonth, status));
    }

    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id) {
        wageManageService.audit(id);
        return R.ok();
    }

    @PostMapping("/{id}/mark-paid")
    public R<Void> markPaid(@PathVariable Long id) {
        wageManageService.markPaid(id);
        return R.ok();
    }

    @GetMapping("/other-items")
    public R<List<WageOther>> getOtherItems(
            @RequestParam Long employeeId, @RequestParam String yearMonth) {
        return R.ok(wageManageService.getOtherItems(employeeId, yearMonth));
    }

    @PostMapping("/other-items")
    public R<Void> addOtherItem(@RequestBody WageOther item) {
        wageManageService.addOtherItem(item);
        return R.ok();
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/admin/
git commit -m "feat: add admin module with dashboard, anomaly, timed wage, and wage management"
```

---

## Module 7: finance 财务模块

### Task 11: 财务模块

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/finance/entity/Payment.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/finance/mapper/PaymentMapper.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/finance/service/PaymentService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/finance/service/ProfitService.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/finance/controller/PaymentController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/finance/controller/ProfitController.java`

- [ ] **Step 1: 创建实体和Mapper**

```java
// Payment.java
package com.zhiyi.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factoryId;
    private Long orderId;
    private Integer type;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
}

// PaymentMapper.java
package com.zhiyi.erp.module.finance.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.finance.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {}
```

- [ ] **Step 2: 创建Service**

```java
// PaymentService.java
package com.zhiyi.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.finance.entity.Payment;
import com.zhiyi.erp.module.finance.mapper.PaymentMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService extends ServiceImpl<PaymentMapper, Payment> {

    public Page<Payment> page(Page<Payment> page, Long orderId, Integer type) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getFactoryId, TenantContext.get());
        if (orderId != null) {
            wrapper.eq(Payment::getOrderId, orderId);
        }
        if (type != null) {
            wrapper.eq(Payment::getType, type);
        }
        wrapper.orderByDesc(Payment::getPaymentDate);
        return page(page, wrapper);
    }

    public BigDecimal getOrderReceivedAmount(Long orderId) {
        List<Payment> payments = list(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getType, 1));
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

// ProfitService.java
package com.zhiyi.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfitService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private WorkReportMapper workReportMapper;

    public List<Map<String, Object>> getProfitByOrder() {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getFactoryId, TenantContext.get())
                        .eq(Order::getDeleted, 0));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Order order : orders) {
            List<WorkReport> reports = workReportMapper.selectList(
                    new LambdaQueryWrapper<WorkReport>()
                            .eq(WorkReport::getOrderId, order.getId())
                            .eq(WorkReport::getStatus, 1));

            BigDecimal cost = reports.stream()
                    .map(WorkReport::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal revenue = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal profit = revenue.subtract(cost);

            Map<String, Object> item = new HashMap<>();
            item.put("orderId", order.getId());
            item.put("orderNo", order.getOrderNo());
            item.put("revenue", revenue);
            item.put("cost", cost);
            item.put("profit", profit);
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                item.put("profitRate", profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, java.math.RoundingMode.HALF_UP));
            } else {
                item.put("profitRate", BigDecimal.ZERO);
            }
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getProfitSummary() {
        List<Map<String, Object>> byOrder = getProfitByOrder();
        BigDecimal totalRevenue = byOrder.stream()
                .map(m -> (BigDecimal) m.get("revenue"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = byOrder.stream()
                .map(m -> (BigDecimal) m.get("cost"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = totalRevenue.subtract(totalCost);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalCost", totalCost);
        summary.put("totalProfit", totalProfit);
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            summary.put("avgProfitRate", totalProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, java.math.RoundingMode.HALF_UP));
        } else {
            summary.put("avgProfitRate", BigDecimal.ZERO);
        }
        return summary;
    }
}
```

- [ ] **Step 3: 创建Controller**

```java
// PaymentController.java
package com.zhiyi.erp.module.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.finance.entity.Payment;
import com.zhiyi.erp.module.finance.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public R<Page<Payment>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer type) {
        return R.ok(paymentService.page(new Page<>(page, size), orderId, type));
    }

    @PostMapping
    public R<Void> create(@RequestBody Payment payment) {
        paymentService.save(payment);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Payment payment) {
        payment.setId(id);
        paymentService.updateById(payment);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        paymentService.removeById(id);
        return R.ok();
    }
}

// ProfitController.java
package com.zhiyi.erp.module.finance.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.finance.service.ProfitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/profit")
public class ProfitController {
    @Autowired
    private ProfitService profitService;

    @GetMapping("/by-order")
    public R<List<Map<String, Object>>> byOrder() {
        return R.ok(profitService.getProfitByOrder());
    }

    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        return R.ok(profitService.getProfitSummary());
    }
}
```

- [ ] **Step 4: 最终编译验证**

```bash
cd zhiyi-server && mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/finance/
git commit -m "feat: add finance module with payment management and profit analysis"
```

---

## 编译验证

- [ ] **最终验证：完整编译**

```bash
cd zhiyi-server && mvn clean package -DskipTests
```

Expected: BUILD SUCCESS，生成 `target/zhiyi-server-1.0.0.jar`

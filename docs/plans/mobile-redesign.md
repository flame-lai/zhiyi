# 移动端模块重实现 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 Taro 小程序基础上重做移动端全部功能，同时新增后端 `/api/mobile/**` 接口，完整覆盖需求文档中员工端和管理员端功能。

**Architecture:** 后端新增 `module/mobile` 包含 Controller/DTO/VO/Service，复用现有 Mapper 和 Service 业务能力。前端在 `zhiyi-mini/src` 上重构页面、API 层、组件和权限控制。保留旧接口不动。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Sa-Token (后端), Taro 4 + Vue3 + TypeScript + Sass (前端)

---

## 文件结构总览

### 后端新建文件

```text
zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/
├── controller/
│   ├── MobileAuthController.java
│   ├── MobileReportController.java
│   └── MobileAdminController.java
├── dto/
│   ├── MobileLoginByCodeDTO.java
│   ├── MobileLoginByAccountDTO.java
│   ├── ScanResolveRequest.java
│   ├── ReportSubmitRequest.java
│   ├── AnomalyIgnoreRequest.java
│   ├── AnomalyClearRequest.java
│   ├── AnomalySupplementRequest.java
│   └── TimedWageRequest.java
├── vo/
│   ├── MobileLoginVO.java
│   ├── MobileMenuVO.java
│   ├── MobileProfileVO.java
│   ├── BundleScanVO.java
│   ├── CuttingScanVO.java
│   ├── ProcessReportStatusVO.java
│   ├── ReportSubmitResultVO.java
│   ├── WageSummaryVO.java
│   ├── WorkReportRecordVO.java
│   ├── DashboardOrderVO.java
│   ├── DashboardWorkerVO.java
│   ├── AnomalyVO.java
│   ├── TimedWageVO.java
│   ├── BundleDetailVO.java
│   └── OrderSearchVO.java
└── service/
    └── MobileService.java
```

### 后端修改文件

```text
zhiyi-server/src/main/java/com/zhiyi/erp/module/system/mapper/EmployeeMapper.java
  — 新增 selectByGlobalBundleNo 给 MobileService 用（或直接在 MobileService 中用 BundleMapper）
```

### 前端新建文件

```text
zhiyi-mini/src/api/mobileAuth.ts
zhiyi-mini/src/api/mobileReport.ts
zhiyi-mini/src/api/mobileAdmin.ts
zhiyi-mini/src/api/types.ts
zhiyi-mini/src/utils/auth.ts
zhiyi-mini/src/utils/format.ts
zhiyi-mini/src/components/AppCard.vue
zhiyi-mini/src/components/StatusTag.vue
zhiyi-mini/src/components/EmptyState.vue
zhiyi-mini/src/components/ConfirmPanel.vue
zhiyi-mini/src/components/ProcessCheckList.vue
zhiyi-mini/src/pages/report-records/index.vue
zhiyi-mini/src/pages/admin/dashboard/index.vue       (重写)
zhiyi-mini/src/pages/admin/anomaly/index.vue          (重写)
zhiyi-mini/src/pages/admin/anomaly-detail/index.vue   (新建)
zhiyi-mini/src/pages/admin/timed-wage/index.vue       (重写)
zhiyi-mini/src/pages/admin/bundle-query/index.vue     (新建)
zhiyi-mini/src/pages/admin/order-query/index.vue      (新建)
```

### 前端修改文件

```text
zhiyi-mini/src/utils/request.ts     — 加强 token 处理和环境配置
zhiyi-mini/src/app.config.ts        — 新增页面路由
zhiyi-mini/src/pages/login/index.vue — 重写为双 Tab 登录
zhiyi-mini/src/pages/index/index.vue — 重写为权限动态首页
zhiyi-mini/src/pages/scan/index.vue  — 重写为三步扫码流程
zhiyi-mini/src/pages/wage/index.vue  — 重写工资页
zhiyi-mini/src/pages/admin/query/index.vue — 删除，拆分为 bundle-query 和 order-query
```

---

## Task 1: 后端移动端认证接口

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/controller/MobileAuthController.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/dto/MobileLoginByCodeDTO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/dto/MobileLoginByAccountDTO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/MobileLoginVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/MobileMenuVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/MobileProfileVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/service/MobileService.java`

- [ ] **Step 1: 创建 DTO 类**

`MobileLoginByCodeDTO.java`:
```java
package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileLoginByCodeDTO {
    @NotBlank(message = "登录码不能为空")
    private String loginCode;
}
```

`MobileLoginByAccountDTO.java`:
```java
package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileLoginByAccountDTO {
    @NotBlank(message = "账号不能为空")
    private String account;
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 2: 创建 VO 类**

`MobileMenuVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

@Data
public class MobileMenuVO {
    private String code;
    private String title;
    private String path;
    private String group;  // employee / admin
    private String icon;
    private Integer sort;
}
```

`MobileLoginVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class MobileLoginVO {
    private String token;
    private EmployeeInfo employee;
    private RoleInfo role;
    private List<MobileMenuVO> menus;
    private List<String> permissions;
    private String defaultHomeType;  // employee / admin

    @Data
    public static class EmployeeInfo {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    public static class RoleInfo {
        private Long id;
        private String name;
    }
}
```

`MobileProfileVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class MobileProfileVO {
    private EmployeeInfo employee;
    private RoleInfo role;
    private List<MobileMenuVO> menus;
    private List<String> permissions;
    private String defaultHomeType;

    @Data
    public static class EmployeeInfo {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    public static class RoleInfo {
        private Long id;
        private String name;
    }
}
```

- [ ] **Step 3: 创建 MobileService 认证方法**

`MobileService.java`:
```java
package com.zhiyi.erp.module.mobile.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.module.mobile.vo.*;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.entity.Menu;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import com.zhiyi.erp.module.system.mapper.RoleMapper;
import com.zhiyi.erp.module.system.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MobileService {

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private MenuService menuService;

    public MobileLoginVO loginByCode(String loginCode) {
        Employee employee = employeeMapper.selectByLoginCode(loginCode);
        if (employee == null) throw new BizException("账号不存在");
        if (employee.getStatus() != 1) throw new BizException("账号已停用，请联系管理员");
        return buildLoginVO(employee);
    }

    public MobileLoginVO loginByAccount(String account, String password) {
        Employee employee = employeeMapper.selectByLoginAccount(account);
        if (employee == null) throw new BizException("账号不存在");
        if (employee.getStatus() != 1) throw new BizException("账号已停用，请联系管理员");
        if (!cn.hutool.crypto.digest.BCrypt.checkpw(password, employee.getLoginPassword())) {
            throw new BizException("密码错误");
        }
        return buildLoginVO(employee);
    }

    public MobileProfileVO getProfile(Long employeeId) {
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) throw new BizException("用户不存在");
        return buildProfileVO(employee);
    }

    private MobileLoginVO buildLoginVO(Employee employee) {
        StpUtil.login(employee.getId());
        StpUtil.getSession().set("factoryId", employee.getFactoryId());
        StpUtil.getSession().set("employeeId", employee.getId());
        StpUtil.getSession().set("roleId", employee.getRoleId());

        MobileLoginVO vo = new MobileLoginVO();
        vo.setToken(StpUtil.getTokenValue());

        MobileLoginVO.EmployeeInfo empInfo = new MobileLoginVO.EmployeeInfo();
        empInfo.setId(employee.getId());
        empInfo.setCode(employee.getEmployeeNo());
        empInfo.setName(employee.getName());
        vo.setEmployee(empInfo);

        Role role = roleMapper.selectById(employee.getRoleId());
        MobileLoginVO.RoleInfo roleInfo = new MobileLoginVO.RoleInfo();
        roleInfo.setId(role != null ? role.getId() : null);
        roleInfo.setName(role != null ? role.getName() : "");
        vo.setRole(roleInfo);

        List<MobileMenuVO> menus = getMobileMenus(employee.getRoleId(), role);
        vo.setMenus(menus);

        List<String> permissions = getMobilePermissions(employee.getRoleId(), role);
        vo.setPermissions(permissions);

        // 判断默认首页类型
        boolean hasAdmin = menus.stream().anyMatch(m -> "admin".equals(m.getGroup()));
        boolean hasEmployee = menus.stream().anyMatch(m -> "employee".equals(m.getGroup()));
        if (hasAdmin && !hasEmployee) {
            vo.setDefaultHomeType("admin");
        } else {
            vo.setDefaultHomeType("employee");
        }

        return vo;
    }

    private MobileProfileVO buildProfileVO(Employee employee) {
        MobileProfileVO vo = new MobileProfileVO();

        MobileProfileVO.EmployeeInfo empInfo = new MobileProfileVO.EmployeeInfo();
        empInfo.setId(employee.getId());
        empInfo.setCode(employee.getEmployeeNo());
        empInfo.setName(employee.getName());
        vo.setEmployee(empInfo);

        Role role = roleMapper.selectById(employee.getRoleId());
        MobileProfileVO.RoleInfo roleInfo = new MobileProfileVO.RoleInfo();
        roleInfo.setId(role != null ? role.getId() : null);
        roleInfo.setName(role != null ? role.getName() : "");
        vo.setRole(roleInfo);

        vo.setMenus(getMobileMenus(employee.getRoleId(), role));
        vo.setPermissions(getMobilePermissions(employee.getRoleId(), role));

        boolean hasAdmin = vo.getMenus().stream().anyMatch(m -> "admin".equals(m.getGroup()));
        boolean hasEmployee = vo.getMenus().stream().anyMatch(m -> "employee".equals(m.getGroup()));
        vo.setDefaultHomeType(hasAdmin && !hasEmployee ? "admin" : "employee");

        return vo;
    }

    private List<MobileMenuVO> getMobileMenus(Long roleId, Role role) {
        List<Menu> menus;
        if (role != null && "super_admin".equalsIgnoreCase(role.getCode())) {
            menus = menuService.listAll();
        } else {
            menus = menuService.getMenusByRoleId(roleId);
        }
        return menus.stream()
                .filter(m -> "mobile".equals(m.getPlatform()) && m.getStatus() == 1)
                .sorted(Comparator.comparingInt(m -> m.getSort() != null ? m.getSort() : 0))
                .map(m -> {
                    MobileMenuVO vo = new MobileMenuVO();
                    vo.setCode(m.getCode());
                    vo.setTitle(m.getName());
                    vo.setPath(m.getPath());
                    vo.setIcon(m.getIcon());
                    vo.setSort(m.getSort());
                    // 根据 code 前缀区分 group
                    String code = m.getCode();
                    if (code != null && code.startsWith("mobile:admin")) {
                        vo.setGroup("admin");
                    } else {
                        vo.setGroup("employee");
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private List<String> getMobilePermissions(Long roleId, Role role) {
        List<Menu> menus;
        if (role != null && "super_admin".equalsIgnoreCase(role.getCode())) {
            menus = menuService.listAll();
        } else {
            menus = menuService.getMenusByRoleId(roleId);
        }
        return menus.stream()
                .filter(m -> "mobile".equals(m.getPlatform()) && m.getPermission() != null && !m.getPermission().isEmpty())
                .map(Menu::getPermission)
                .distinct()
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 4: 创建 MobileAuthController**

`MobileAuthController.java`:
```java
package com.zhiyi.erp.module.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.mobile.dto.MobileLoginByAccountDTO;
import com.zhiyi.erp.module.mobile.dto.MobileLoginByCodeDTO;
import com.zhiyi.erp.module.mobile.service.MobileService;
import com.zhiyi.erp.module.mobile.vo.MobileLoginVO;
import com.zhiyi.erp.module.mobile.vo.MobileProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "移动端认证", description = "移动端登录、登出、用户信息")
@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {

    @Autowired
    private MobileService mobileService;

    @Operation(summary = "工号/登录码登录")
    @PostMapping("/login-by-code")
    public R<MobileLoginVO> loginByCode(@Valid @RequestBody MobileLoginByCodeDTO dto) {
        return R.ok(mobileService.loginByCode(dto.getLoginCode()));
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login-by-account")
    public R<MobileLoginVO> loginByAccount(@Valid @RequestBody MobileLoginByAccountDTO dto) {
        return R.ok(mobileService.loginByAccount(dto.getAccount(), dto.getPassword()));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息（含菜单权限）")
    @GetMapping("/profile")
    public R<MobileProfileVO> getProfile() {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(mobileService.getProfile(employeeId));
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `cd zhiyi-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/
git commit -m "feat(mobile): add mobile auth controller, DTOs, VOs and service"
```

---

## Task 2: 后端扫码报工接口

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/dto/ScanResolveRequest.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/dto/ReportSubmitRequest.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/BundleScanVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/CuttingScanVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/ProcessReportStatusVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/ReportSubmitResultVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/controller/MobileReportController.java`
- Modify: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/service/MobileService.java` — 新增扫码解析和报工方法

- [ ] **Step 1: 创建扫码相关 DTO**

`ScanResolveRequest.java`:
```java
package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanResolveRequest {
    @NotBlank(message = "扫码内容不能为空")
    private String code;
}
```

`ReportSubmitRequest.java`:
```java
package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ReportSubmitRequest {
    @NotBlank(message = "扫码类型不能为空")
    private String scanType;  // BUNDLE / CUTTING

    @NotBlank(message = "扫码内容不能为空")
    private String code;

    @NotEmpty(message = "请选择至少一个扎号")
    private List<Long> selectedBundleIds;

    @NotEmpty(message = "请选择至少一道工序")
    private List<Long> selectedProcessIds;
}
```

- [ ] **Step 2: 创建扫码相关 VO**

`ProcessReportStatusVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProcessReportStatusVO {
    private Long processId;
    private String processName;
    private BigDecimal price;
    private String status;  // AVAILABLE / DONE / PARTIAL_DONE / UNAVAILABLE
    private String doneBy;
    private String doneTime;
    private String hint;
}
```

`BundleScanVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class BundleScanVO {
    private String scanType = "BUNDLE";
    private String code;
    private BundleInfo bundle;
    private List<ProcessReportStatusVO> processes;

    @Data
    public static class BundleInfo {
        private Long id;
        private String globalBundleNo;
        private String bundleNo;
        private Integer qty;
        private String orderNo;
        private String styleNo;
        private String color;
        private String size;
    }
}
```

`CuttingScanVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class CuttingScanVO {
    private String scanType = "CUTTING";
    private String code;
    private CuttingInfo cutting;
    private List<BundleItem> bundles;
    private List<ProcessReportStatusVO> processes;

    @Data
    public static class CuttingInfo {
        private Long id;
        private String barcode;
        private Integer bedNo;
        private Integer totalBundleCount;
        private Integer totalQty;
        private String orderNo;
        private String styleNo;
        private String color;
        private String size;
    }

    @Data
    public static class BundleItem {
        private Long id;
        private String globalBundleNo;
        private String bundleNo;
        private Integer qty;
        private Boolean disabled;
        private String disabledReason;
    }
}
```

`ReportSubmitResultVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ReportSubmitResultVO {
    private Integer totalQty;
    private BigDecimal totalAmount;
    private List<ReportItem> items;
    private List<SkippedItem> skippedItems;

    @Data
    public static class ReportItem {
        private Long processId;
        private String processName;
        private Integer qty;
        private BigDecimal price;
        private BigDecimal amount;
    }

    @Data
    public static class SkippedItem {
        private String bundleNo;
        private String processName;
        private String reason;
    }
}
```

- [ ] **Step 3: 在 MobileService 中新增扫码解析和报工方法**

在 `MobileService.java` 中新增以下依赖注入和方法：

```java
@Autowired
private com.zhiyi.erp.module.cutting.mapper.BundleMapper bundleMapper;
@Autowired
private com.zhiyi.erp.module.cutting.mapper.CuttingPlanMapper cuttingPlanMapper;
@Autowired
private com.zhiyi.erp.module.order.mapper.OrderProcessMapper orderProcessMapper;
@Autowired
private com.zhiyi.erp.module.order.mapper.OrderMapper orderMapper;
@Autowired
private com.zhiyi.erp.module.base.mapper.StyleMapper styleMapper;
@Autowired
private com.zhiyi.erp.module.base.mapper.ProcessMapper processMapper;
@Autowired
private com.zhiyi.erp.module.report.mapper.WorkReportMapper workReportMapper;
@Autowired
private com.zhiyi.erp.module.report.service.ReportService reportService;
@Autowired
private com.zhiyi.erp.module.system.mapper.EmployeeMapper employeeMapperForReport;
```

扫码解析方法 `resolveScan`:

```java
public Object resolveScan(String code) {
    // 判断码类型：BUN 开头为扎号QR码，CUT 开头为裁床条形码
    if (code.startsWith("BUN")) {
        return resolveBundleScan(code);
    } else if (code.startsWith("CUT")) {
        return resolveCuttingScan(code);
    } else {
        throw new BizException("未识别到有效扎号或裁床码");
    }
}

private BundleScanVO resolveBundleScan(String globalBundleNo) {
    Bundle bundle = bundleMapper.selectOne(
            new LambdaQueryWrapper<Bundle>()
                    .eq(Bundle::getGlobalBundleNo, globalBundleNo));
    if (bundle == null) throw new BizException("扎号不存在");

    BundleScanVO vo = new BundleScanVO();
    vo.setCode(globalBundleNo);

    BundleScanVO.BundleInfo info = new BundleScanVO.BundleInfo();
    info.setId(bundle.getId());
    info.setGlobalBundleNo(bundle.getGlobalBundleNo());
    info.setBundleNo(bundle.getBundleNo());
    info.setQty(bundle.getQty());

    // 查订单号和款号
    com.zhiyi.erp.module.order.entity.Order order = orderMapper.selectById(bundle.getOrderId());
    info.setOrderNo(order != null ? order.getOrderNo() : "");
    if (order != null && order.getStyleId() != null) {
        com.zhiyi.erp.module.base.entity.Style style = styleMapper.selectById(order.getStyleId());
        info.setStyleNo(style != null ? style.getStyleCode() : "");
    }
    info.setColor(bundle.getColor());
    info.setSize(bundle.getSize());
    vo.setBundle(info);

    // 工序状态
    vo.setProcesses(buildProcessStatusList(bundle.getOrderId(), bundle.getId()));
    return vo;
}

private CuttingScanVO resolveCuttingScan(String barcode) {
    com.zhiyi.erp.module.cutting.entity.CuttingPlan plan = cuttingPlanMapper.selectOne(
            new LambdaQueryWrapper<com.zhiyi.erp.module.cutting.entity.CuttingPlan>()
                    .eq(com.zhiyi.erp.module.cutting.entity.CuttingPlan::getBarcode, barcode));
    if (plan == null) throw new BizException("裁床码无效");
    if (plan.getStatus() < 2) throw new BizException("请先开菲");

    List<Bundle> bundles = bundleMapper.selectList(
            new LambdaQueryWrapper<Bundle>().eq(Bundle::getCuttingPlanId, plan.getId()));
    if (bundles.isEmpty()) throw new BizException("无扎号数据");

    CuttingScanVO vo = new CuttingScanVO();
    vo.setCode(barcode);

    CuttingScanVO.CuttingInfo cuttingInfo = new CuttingScanVO.CuttingInfo();
    cuttingInfo.setId(plan.getId());
    cuttingInfo.setBarcode(plan.getBarcode());
    cuttingInfo.setBedNo(plan.getBedNo());
    cuttingInfo.setTotalBundleCount(bundles.size());
    cuttingInfo.setTotalQty(bundles.stream().mapToInt(Bundle::getQty).sum());

    // 取第一个扎号的订单信息（同床同订单）
    com.zhiyi.erp.module.order.entity.Order order = orderMapper.selectById(plan.getOrderId());
    cuttingInfo.setOrderNo(order != null ? order.getOrderNo() : "");
    if (order != null && order.getStyleId() != null) {
        com.zhiyi.erp.module.base.entity.Style style = styleMapper.selectById(order.getStyleId());
        cuttingInfo.setStyleNo(style != null ? style.getStyleCode() : "");
    }
    cuttingInfo.setColor(bundles.get(0).getColor());
    cuttingInfo.setSize(bundles.get(0).getSize());
    vo.setCutting(cuttingInfo);

    // 扎号列表
    List<CuttingScanVO.BundleItem> bundleItems = bundles.stream().map(b -> {
        CuttingScanVO.BundleItem item = new CuttingScanVO.BundleItem();
        item.setId(b.getId());
        item.setGlobalBundleNo(b.getGlobalBundleNo());
        item.setBundleNo(b.getBundleNo());
        item.setQty(b.getQty());
        item.setDisabled(false);
        return item;
    }).collect(Collectors.toList());
    vo.setBundles(bundleItems);

    // 工序状态（取第一个扎号的工序，整床共享同一订单的工序）
    vo.setProcesses(buildProcessStatusList(plan.getOrderId(), bundles.get(0).getId()));
    return vo;
}

private List<ProcessReportStatusVO> buildProcessStatusList(Long orderId, Long sampleBundleId) {
    List<com.zhiyi.erp.module.order.entity.OrderProcess> orderProcesses =
            orderProcessMapper.selectList(new LambdaQueryWrapper<com.zhiyi.erp.module.order.entity.OrderProcess>()
                    .eq(com.zhiyi.erp.module.order.entity.OrderProcess::getOrderId, orderId));

    // 查该扎号的所有有效报工记录
    List<com.zhiyi.erp.module.report.entity.WorkReport> reports = workReportMapper.selectList(
            new LambdaQueryWrapper<com.zhiyi.erp.module.report.entity.WorkReport>()
                    .eq(com.zhiyi.erp.module.report.entity.WorkReport::getBundleId, sampleBundleId)
                    .eq(com.zhiyi.erp.module.report.entity.WorkReport::getStatus, 1));
    Map<Long, com.zhiyi.erp.module.report.entity.WorkReport> reportMap = reports.stream()
            .collect(Collectors.toMap(com.zhiyi.erp.module.report.entity.WorkReport::getProcessId, r -> r, (a, b) -> a));

    return orderProcesses.stream().map(op -> {
        ProcessReportStatusVO ps = new ProcessReportStatusVO();
        ps.setProcessId(op.getProcessId());
        // 查工序名称
        com.zhiyi.erp.module.base.entity.Process process = processMapper.selectById(op.getProcessId());
        ps.setProcessName(process != null ? process.getName() : "");
        ps.setPrice(op.getProcessPrice());

        com.zhiyi.erp.module.report.entity.WorkReport report = reportMap.get(op.getProcessId());
        if (report != null) {
            ps.setStatus("DONE");
            // 查报工人
            Employee emp = employeeMapper.selectById(report.getEmployeeId());
            ps.setDoneBy(emp != null ? emp.getName() : "");
            ps.setDoneTime(report.getReportTime() != null ? report.getReportTime().toString() : "");
        } else {
            ps.setStatus("AVAILABLE");
        }
        return ps;
    }).collect(Collectors.toList());
}
```

报工提交方法 `submitReport`:

```java
@Transactional
public ReportSubmitResultVO submitReport(Long employeeId, ReportSubmitRequest req) {
    // 验证所选扎号确实属于该扫码结果
    if ("BUNDLE".equals(req.getScanType())) {
        Bundle bundle = bundleMapper.selectOne(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getGlobalBundleNo, req.getCode()));
        if (bundle == null) throw new BizException("扎号不存在");
        if (!req.getSelectedBundleIds().contains(bundle.getId())) {
            throw new BizException("所选扎号与扫码结果不符");
        }
    } else if ("CUTTING".equals(req.getScanType())) {
        com.zhiyi.erp.module.cutting.entity.CuttingPlan plan = cuttingPlanMapper.selectOne(
                new LambdaQueryWrapper<com.zhiyi.erp.module.cutting.entity.CuttingPlan>()
                        .eq(com.zhiyi.erp.module.cutting.entity.CuttingPlan::getBarcode, req.getCode()));
        if (plan == null) throw new BizException("裁床码无效");
        List<Bundle> planBundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>().eq(Bundle::getCuttingPlanId, plan.getId()));
        List<Long> validBundleIds = planBundles.stream().map(Bundle::getId).collect(Collectors.toList());
        for (Long bid : req.getSelectedBundleIds()) {
            if (!validBundleIds.contains(bid)) throw new BizException("所选扎号与扫码结果不符");
        }
    }

    ReportSubmitResultVO result = new ReportSubmitResultVO();
    List<ReportSubmitResultVO.ReportItem> items = new ArrayList<>();
    List<ReportSubmitResultVO.SkippedItem> skipped = new ArrayList<>();
    java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
    int totalQty = 0;

    for (Long bundleId : req.getSelectedBundleIds()) {
        Bundle bundle = bundleMapper.selectById(bundleId);
        if (bundle == null) continue;

        for (Long processId : req.getSelectedProcessIds()) {
            // 检查是否已报工
            long count = workReportMapper.selectCount(
                    new LambdaQueryWrapper<com.zhiyi.erp.module.report.entity.WorkReport>()
                            .eq(com.zhiyi.erp.module.report.entity.WorkReport::getBundleId, bundleId)
                            .eq(com.zhiyi.erp.module.report.entity.WorkReport::getProcessId, processId)
                            .eq(com.zhiyi.erp.module.report.entity.WorkReport::getStatus, 1));
            if (count > 0) {
                // 跳过
                com.zhiyi.erp.module.base.entity.Process p = processMapper.selectById(processId);
                ReportSubmitResultVO.SkippedItem si = new ReportSubmitResultVO.SkippedItem();
                si.setBundleNo(bundle.getBundleNo());
                si.setProcessName(p != null ? p.getName() : "");
                si.setReason("该工序已报工");
                skipped.add(si);
                continue;
            }

            // 查工序价格
            com.zhiyi.erp.module.order.entity.OrderProcess op = orderProcessMapper.selectOne(
                    new LambdaQueryWrapper<com.zhiyi.erp.module.order.entity.OrderProcess>()
                            .eq(com.zhiyi.erp.module.order.entity.OrderProcess::getOrderId, bundle.getOrderId())
                            .eq(com.zhiyi.erp.module.order.entity.OrderProcess::getProcessId, processId));
            if (op == null) continue;

            java.math.BigDecimal amount = op.getProcessPrice().multiply(java.math.BigDecimal.valueOf(bundle.getQty()));

            com.zhiyi.erp.module.report.entity.WorkReport report = new com.zhiyi.erp.module.report.entity.WorkReport();
            report.setFactoryId(com.zhiyi.erp.common.tenant.TenantContext.get());
            report.setBundleId(bundleId);
            report.setOrderId(bundle.getOrderId());
            report.setProcessId(processId);
            report.setEmployeeId(employeeId);
            report.setQty(bundle.getQty());
            report.setPrice(op.getProcessPrice());
            report.setAmount(amount);
            report.setReportType(1);
            report.setStatus(1);
            report.setReportTime(java.time.LocalDateTime.now());
            report.setSupplementFlag(0);
            workReportMapper.insert(report);

            com.zhiyi.erp.module.base.entity.Process p = processMapper.selectById(processId);
            ReportSubmitResultVO.ReportItem item = new ReportSubmitResultVO.ReportItem();
            item.setProcessId(processId);
            item.setProcessName(p != null ? p.getName() : "");
            item.setQty(bundle.getQty());
            item.setPrice(op.getProcessPrice());
            item.setAmount(amount);
            items.add(item);

            totalAmount = totalAmount.add(amount);
            totalQty += bundle.getQty();
        }
    }

    if (items.isEmpty() && !skipped.isEmpty()) {
        throw new BizException("所选工序已全部报工，无需重复上报");
    }

    result.setTotalQty(totalQty);
    result.setTotalAmount(totalAmount);
    result.setItems(items);
    result.setSkippedItems(skipped.isEmpty() ? null : skipped);
    return result;
}
```

- [ ] **Step 4: 创建 MobileReportController**

`MobileReportController.java`:
```java
package com.zhiyi.erp.module.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.mobile.dto.ReportSubmitRequest;
import com.zhiyi.erp.module.mobile.dto.ScanResolveRequest;
import com.zhiyi.erp.module.mobile.service.MobileService;
import com.zhiyi.erp.module.mobile.vo.ReportSubmitResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "移动端报工", description = "扫码解析、确认报工、工资、报工记录")
@RestController
@RequestMapping("/api/mobile/report")
public class MobileReportController {

    @Autowired
    private MobileService mobileService;

    @Operation(summary = "扫码解析")
    @PostMapping("/scan/resolve")
    public R<Object> resolveScan(@Valid @RequestBody ScanResolveRequest req) {
        return R.ok(mobileService.resolveScan(req.getCode()));
    }

    @Operation(summary = "确认上报")
    @PostMapping("/submit")
    public R<ReportSubmitResultVO> submitReport(@Valid @RequestBody ReportSubmitRequest req) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(mobileService.submitReport(employeeId, req));
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `cd zhiyi-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/
git commit -m "feat(mobile): add scan resolve and report submit APIs"
```

---

## Task 3: 后端工资与报工记录接口

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/WageSummaryVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/WorkReportRecordVO.java`
- Modify: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/service/MobileService.java`
- Modify: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/controller/MobileReportController.java`

- [ ] **Step 1: 创建 VO**

`WageSummaryVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class WageSummaryVO {
    private BigDecimal todayAmount;
    private BigDecimal monthAmount;
    private List<DailyGroup> dailyGroups;

    @Data
    public static class DailyGroup {
        private String date;
        private BigDecimal amount;
        private List<WorkReportRecordVO> records;
    }
}
```

`WorkReportRecordVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WorkReportRecordVO {
    private Long id;
    private String reportTime;
    private String orderNo;
    private String styleNo;
    private String bundleNo;
    private String processName;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
}
```

- [ ] **Step 2: 在 MobileService 中新增工资和记录方法**

```java
public WageSummaryVO getWageSummary(Long employeeId) {
    java.time.LocalDate today = java.time.LocalDate.now();
    String yearMonth = today.toString().substring(0, 7);

    List<com.zhiyi.erp.module.report.entity.WorkReport> reports = workReportMapper.selectList(
            new LambdaQueryWrapper<com.zhiyi.erp.module.report.entity.WorkReport>()
                    .eq(com.zhiyi.erp.module.report.entity.WorkReport::getEmployeeId, employeeId)
                    .eq(com.zhiyi.erp.module.report.entity.WorkReport::getStatus, 1)
                    .apply("DATE_FORMAT(report_time, '%Y-%m') = {0}", yearMonth));

    java.math.BigDecimal todayAmount = java.math.BigDecimal.ZERO;
    java.math.BigDecimal monthAmount = java.math.BigDecimal.ZERO;
    String todayStr = today.toString();

    Map<String, List<com.zhiyi.erp.module.report.entity.WorkReport>> byDate = reports.stream()
            .collect(Collectors.groupingBy(r -> r.getReportTime().toLocalDate().toString()));

    List<WageSummaryVO.DailyGroup> dailyGroups = new ArrayList<>();
    for (Map.Entry<String, List<com.zhiyi.erp.module.report.entity.WorkReport>> entry : byDate.entrySet()) {
        WageSummaryVO.DailyGroup group = new WageSummaryVO.DailyGroup();
        group.setDate(entry.getKey());
        java.math.BigDecimal dayTotal = entry.getValue().stream()
                .map(com.zhiyi.erp.module.report.entity.WorkReport::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        group.setAmount(dayTotal);
        monthAmount = monthAmount.add(dayTotal);
        if (entry.getKey().equals(todayStr)) todayAmount = dayTotal;

        group.setRecords(entry.getValue().stream().map(r -> buildWorkReportRecordVO(r)).collect(Collectors.toList()));
        dailyGroups.add(group);
    }
    // 按日期倒序
    dailyGroups.sort((a, b) -> b.getDate().compareTo(a.getDate()));

    WageSummaryVO vo = new WageSummaryVO();
    vo.setTodayAmount(todayAmount);
    vo.setMonthAmount(monthAmount);
    vo.setDailyGroups(dailyGroups);
    return vo;
}

public List<WorkReportRecordVO> getReportRecords(Long employeeId, String startDate, String endDate) {
    LambdaQueryWrapper<com.zhiyi.erp.module.report.entity.WorkReport> wrapper =
            new LambdaQueryWrapper<com.zhiyi.erp.module.report.entity.WorkReport>()
                    .eq(com.zhiyi.erp.module.report.entity.WorkReport::getEmployeeId, employeeId)
                    .eq(com.zhiyi.erp.module.report.entity.WorkReport::getStatus, 1);
    if (startDate != null && !startDate.isEmpty()) {
        wrapper.apply("DATE(report_time) >= {0}", startDate);
    }
    if (endDate != null && !endDate.isEmpty()) {
        wrapper.apply("DATE(report_time) <= {0}", endDate);
    }
    wrapper.orderByDesc(com.zhiyi.erp.module.report.entity.WorkReport::getReportTime);
    List<com.zhiyi.erp.module.report.entity.WorkReport> reports = workReportMapper.selectList(wrapper);
    return reports.stream().map(this::buildWorkReportRecordVO).collect(Collectors.toList());
}

private WorkReportRecordVO buildWorkReportRecordVO(com.zhiyi.erp.module.report.entity.WorkReport r) {
    WorkReportRecordVO vo = new WorkReportRecordVO();
    vo.setId(r.getId());
    vo.setReportTime(r.getReportTime() != null ? r.getReportTime().toString() : "");
    vo.setQty(r.getQty());
    vo.setPrice(r.getPrice());
    vo.setAmount(r.getAmount());

    // 查订单号、款号
    com.zhiyi.erp.module.order.entity.Order order = orderMapper.selectById(r.getOrderId());
    vo.setOrderNo(order != null ? order.getOrderNo() : "");
    if (order != null && order.getStyleId() != null) {
        com.zhiyi.erp.module.base.entity.Style style = styleMapper.selectById(order.getStyleId());
        vo.setStyleNo(style != null ? style.getStyleCode() : "");
    }

    // 查扎号
    Bundle bundle = bundleMapper.selectById(r.getBundleId());
    vo.setBundleNo(bundle != null ? bundle.getBundleNo() : "");

    // 查工序名称
    com.zhiyi.erp.module.base.entity.Process process = processMapper.selectById(r.getProcessId());
    vo.setProcessName(process != null ? process.getName() : "");

    return vo;
}
```

- [ ] **Step 3: 在 MobileReportController 中新增工资和记录接口**

```java
@Operation(summary = "工资汇总")
@GetMapping("/wage-summary")
public R<WageSummaryVO> getWageSummary() {
    Long employeeId = StpUtil.getSession().getLong("employeeId");
    return R.ok(mobileService.getWageSummary(employeeId));
}

@Operation(summary = "报工记录")
@GetMapping("/records")
public R<java.util.List<WorkReportRecordVO>> getReportRecords(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    Long employeeId = StpUtil.getSession().getLong("employeeId");
    return R.ok(mobileService.getReportRecords(employeeId, startDate, endDate));
}
```

- [ ] **Step 4: 编译验证**

Run: `cd zhiyi-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/
git commit -m "feat(mobile): add wage summary and report records APIs"
```

---

## Task 4: 后端管理员接口

**Files:**
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/dto/AnomalyIgnoreRequest.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/dto/AnomalyClearRequest.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/dto/AnomalySupplementRequest.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/dto/TimedWageRequest.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/DashboardOrderVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/DashboardWorkerVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/AnomalyVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/TimedWageVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/BundleDetailVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/vo/OrderSearchVO.java`
- Create: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/controller/MobileAdminController.java`
- Modify: `zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/service/MobileService.java`

- [ ] **Step 1: 创建 DTO**

`AnomalyIgnoreRequest.java`:
```java
package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnomalyIgnoreRequest {
    @NotBlank(message = "原因不能为空")
    private String remark;
}
```

`AnomalyClearRequest.java`:
```java
package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnomalyClearRequest {
    @NotBlank(message = "原因不能为空")
    private String reason;
}
```

`AnomalySupplementRequest.java`:
```java
package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnomalySupplementRequest {
    @NotNull(message = "工人不能为空")
    private Long employeeId;
    @NotNull(message = "件数不能为空")
    private Integer qty;
    private String reportTime;
}
```

`TimedWageRequest.java`:
```java
package com.zhiyi.erp.module.mobile.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TimedWageRequest {
    @NotNull(message = "工人不能为空")
    private Long employeeId;
    private String workDate;
    @NotNull(message = "工时不能为空")
    private BigDecimal hours;
    @NotNull(message = "时薪不能为空")
    private BigDecimal hourlyRate;
    private String workContent;
}
```

- [ ] **Step 2: 创建 VO**

`DashboardOrderVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardOrderVO {
    private Long orderId;
    private String orderNo;
    private String styleNo;
    private Integer totalBundles;
    private BigDecimal completionRate;
    private String bottleneckProcess;
    private List<ProcessProgress> processProgress;

    @Data
    public static class ProcessProgress {
        private Long processId;
        private String processName;
        private Integer completed;
        private Integer total;
        private BigDecimal rate;
    }
}
```

`DashboardWorkerVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DashboardWorkerVO {
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private Integer totalQty;
    private BigDecimal totalAmount;
}
```

`AnomalyVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

@Data
public class AnomalyVO {
    private Long id;
    private String bundleNo;
    private String orderNo;
    private String description;
    private String frontProcessName;
    private String backProcessName;
    private String reportEmployeeName;
    private String reportTime;
    private Integer status;       // 0=待处理, 1=已忽略, 2=已清除, 3=已补录
    private Integer handleType;
    private String handleRemark;
}
```

`TimedWageVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TimedWageVO {
    private Long id;
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private LocalDate workDate;
    private BigDecimal hours;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
    private String workContent;
}
```

`BundleDetailVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.util.List;

@Data
public class BundleDetailVO {
    private Long id;
    private String globalBundleNo;
    private String bundleNo;
    private Integer qty;
    private String orderNo;
    private String styleNo;
    private String color;
    private String size;
    private String status;  // 生产中 / 已完成
    private List<ProcessReportStatusVO> processes;
}
```

`OrderSearchVO.java`:
```java
package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderSearchVO {
    private Long id;
    private String orderNo;
    private String customerName;
    private BigDecimal totalAmount;
    private LocalDate deliveryDate;
    private Integer status;
    private BigDecimal completionRate;
    private List<OrderItemVO> items;

    @Data
    public static class OrderItemVO {
        private String styleNo;
        private String color;
        private String size;
        private Integer qty;
        private Integer shippedQty;
        private BigDecimal progress;
    }
}
```

- [ ] **Step 3: 在 MobileService 中新增管理员方法**

```java
// 看板
public List<DashboardOrderVO> getDashboardOrders(String date, Long orderId) {
    // 复用 DashboardService 逻辑但输出结构化 VO
    List<Bundle> bundles;
    if (orderId != null) {
        bundles = bundleMapper.selectList(new LambdaQueryWrapper<Bundle>().eq(Bundle::getOrderId, orderId));
    } else {
        bundles = bundleMapper.selectList(new LambdaQueryWrapper<Bundle>().eq(Bundle::getFactoryId, com.zhiyi.erp.common.tenant.TenantContext.get()));
    }
    Map<Long, List<Bundle>> byOrder = bundles.stream().collect(Collectors.groupingBy(Bundle::getOrderId));

    List<DashboardOrderVO> result = new ArrayList<>();
    for (Map.Entry<Long, List<Bundle>> entry : byOrder.entrySet()) {
        com.zhiyi.erp.module.order.entity.Order order = orderMapper.selectById(entry.getKey());
        DashboardOrderVO vo = new DashboardOrderVO();
        vo.setOrderId(entry.getKey());
        vo.setOrderNo(order != null ? order.getOrderNo() : "");
        if (order != null && order.getStyleId() != null) {
            com.zhiyi.erp.module.base.entity.Style style = styleMapper.selectById(order.getStyleId());
            vo.setStyleNo(style != null ? style.getStyleCode() : "");
        }
        vo.setTotalBundles(entry.getValue().size());

        List<Long> bundleIds = entry.getValue().stream().map(Bundle::getId).collect(Collectors.toList());
        List<com.zhiyi.erp.module.report.entity.WorkReport> reports = workReportMapper.selectList(
                new LambdaQueryWrapper<com.zhiyi.erp.module.report.entity.WorkReport>()
                        .in(com.zhiyi.erp.module.report.entity.WorkReport::getBundleId, bundleIds)
                        .eq(com.zhiyi.erp.module.report.entity.WorkReport::getStatus, 1));

        List<com.zhiyi.erp.module.order.entity.OrderProcess> processes = orderProcessMapper.selectList(
                new LambdaQueryWrapper<com.zhiyi.erp.module.order.entity.OrderProcess>()
                        .eq(com.zhiyi.erp.module.order.entity.OrderProcess::getOrderId, entry.getKey()));

        java.math.BigDecimal minRate = java.math.BigDecimal.ONE;
        String bottleneck = "";
        List<DashboardOrderVO.ProcessProgress> ppList = new ArrayList<>();
        for (com.zhiyi.erp.module.order.entity.OrderProcess op : processes) {
            long completed = reports.stream()
                    .filter(r -> r.getProcessId().equals(op.getProcessId()))
                    .map(com.zhiyi.erp.module.report.entity.WorkReport::getBundleId)
                    .distinct()
                    .count();
            java.math.BigDecimal rate = java.math.BigDecimal.valueOf(completed)
                    .divide(java.math.BigDecimal.valueOf(entry.getValue().size()), 4, java.math.RoundingMode.HALF_UP);
            if (rate.compareTo(minRate) < 0) {
                minRate = rate;
                com.zhiyi.erp.module.base.entity.Process p = processMapper.selectById(op.getProcessId());
                bottleneck = p != null ? p.getName() : "";
            }
            DashboardOrderVO.ProcessProgress pp = new DashboardOrderVO.ProcessProgress();
            pp.setProcessId(op.getProcessId());
            com.zhiyi.erp.module.base.entity.Process p = processMapper.selectById(op.getProcessId());
            pp.setProcessName(p != null ? p.getName() : "");
            pp.setCompleted((int) completed);
            pp.setTotal(entry.getValue().size());
            pp.setRate(rate);
            ppList.add(pp);
        }
        vo.setProcessProgress(ppList);
        vo.setCompletionRate(minRate);
        vo.setBottleneckProcess(bottleneck);
        result.add(vo);
    }
    return result;
}

public List<DashboardWorkerVO> getDashboardWorkers(String date) {
    List<com.zhiyi.erp.module.report.entity.WorkReport> reports = workReportMapper.selectList(
            new LambdaQueryWrapper<com.zhiyi.erp.module.report.entity.WorkReport>()
                    .eq(com.zhiyi.erp.module.report.entity.WorkReport::getFactoryId, com.zhiyi.erp.common.tenant.TenantContext.get())
                    .eq(com.zhiyi.erp.module.report.entity.WorkReport::getStatus, 1)
                    .apply("DATE(report_time) = {0}", date));

    Map<Long, List<com.zhiyi.erp.module.report.entity.WorkReport>> byEmployee = reports.stream()
            .collect(Collectors.groupingBy(com.zhiyi.erp.module.report.entity.WorkReport::getEmployeeId));

    return byEmployee.entrySet().stream().map(e -> {
        DashboardWorkerVO vo = new DashboardWorkerVO();
        vo.setEmployeeId(e.getKey());
        Employee emp = employeeMapper.selectById(e.getKey());
        vo.setEmployeeNo(emp != null ? emp.getEmployeeNo() : "");
        vo.setEmployeeName(emp != null ? emp.getName() : "");
        vo.setTotalQty(e.getValue().stream().mapToInt(com.zhiyi.erp.module.report.entity.WorkReport::getQty).sum());
        vo.setTotalAmount(e.getValue().stream().map(com.zhiyi.erp.module.report.entity.WorkReport::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        return vo;
    }).collect(Collectors.toList());
}

// 异常
public List<AnomalyVO> getAnomalies(Integer status) {
    LambdaQueryWrapper<com.zhiyi.erp.module.admin.entity.AnomalyAlert> wrapper =
            new LambdaQueryWrapper<com.zhiyi.erp.module.admin.entity.AnomalyAlert>()
                    .eq(com.zhiyi.erp.module.admin.entity.AnomalyAlert::getFactoryId, com.zhiyi.erp.common.tenant.TenantContext.get());
    if (status != null) wrapper.eq(com.zhiyi.erp.module.admin.entity.AnomalyAlert::getStatus, status);
    wrapper.orderByDesc(com.zhiyi.erp.module.admin.entity.AnomalyAlert::getCreatedAt);
    List<com.zhiyi.erp.module.admin.entity.AnomalyAlert> alerts = anomalyAlertMapper.selectList(wrapper);

    return alerts.stream().map(a -> {
        AnomalyVO vo = new AnomalyVO();
        vo.setId(a.getId());
        Bundle bundle = bundleMapper.selectById(a.getBundleId());
        vo.setBundleNo(bundle != null ? bundle.getBundleNo() : "");
        com.zhiyi.erp.module.order.entity.Order order = orderMapper.selectById(a.getOrderId());
        vo.setOrderNo(order != null ? order.getOrderNo() : "");
        com.zhiyi.erp.module.base.entity.Process fp = processMapper.selectById(a.getFrontProcessId());
        com.zhiyi.erp.module.base.entity.Process bp = processMapper.selectById(a.getBackProcessId());
        vo.setFrontProcessName(fp != null ? fp.getName() : "");
        vo.setBackProcessName(bp != null ? bp.getName() : "");
        vo.setDescription((bp != null ? bp.getName() : "") + "已完成但" + (fp != null ? fp.getName() : "") + "未完成");
        if (a.getReportEmployeeId() != null) {
            Employee emp = employeeMapper.selectById(a.getReportEmployeeId());
            vo.setReportEmployeeName(emp != null ? emp.getName() : "");
        }
        vo.setReportTime(a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
        vo.setStatus(a.getStatus());
        vo.setHandleType(a.getHandleType());
        vo.setHandleRemark(a.getHandleRemark());
        return vo;
    }).collect(Collectors.toList());
}

public void ignoreAnomaly(Long id, String remark) {
    anomalyService.ignore(id, remark);
}

public void clearAnomaly(Long id, String reason) {
    anomalyService.clearReport(id, reason);
}

public void supplementAnomaly(Long id, Long employeeId, Integer qty) {
    anomalyService.supplement(id, employeeId, qty);
}

// 计时工资
public List<TimedWageVO> getTimedWages(String yearMonth) {
    LambdaQueryWrapper<com.zhiyi.erp.module.admin.entity.TimedWage> wrapper =
            new LambdaQueryWrapper<com.zhiyi.erp.module.admin.entity.TimedWage>()
                    .eq(com.zhiyi.erp.module.admin.entity.TimedWage::getFactoryId, com.zhiyi.erp.common.tenant.TenantContext.get());
    if (yearMonth != null) wrapper.apply("DATE_FORMAT(work_date, '%Y-%m') = {0}", yearMonth);
    wrapper.orderByDesc(com.zhiyi.erp.module.admin.entity.TimedWage::getWorkDate);
    List<com.zhiyi.erp.module.admin.entity.TimedWage> wages = timedWageMapper.selectList(wrapper);

    return wages.stream().map(w -> {
        TimedWageVO vo = new TimedWageVO();
        vo.setId(w.getId());
        vo.setEmployeeId(w.getEmployeeId());
        Employee emp = employeeMapper.selectById(w.getEmployeeId());
        vo.setEmployeeNo(emp != null ? emp.getEmployeeNo() : "");
        vo.setEmployeeName(emp != null ? emp.getName() : "");
        vo.setWorkDate(w.getWorkDate());
        vo.setHours(w.getHours());
        vo.setHourlyRate(w.getHourlyRate());
        vo.setAmount(w.getAmount());
        vo.setWorkContent(w.getWorkContent());
        return vo;
    }).collect(Collectors.toList());
}

public void createTimedWage(TimedWageRequest req) {
    com.zhiyi.erp.module.admin.entity.TimedWage wage = new com.zhiyi.erp.module.admin.entity.TimedWage();
    wage.setFactoryId(com.zhiyi.erp.common.tenant.TenantContext.get());
    wage.setEmployeeId(req.getEmployeeId());
    if (req.getWorkDate() != null && !req.getWorkDate().isEmpty()) {
        wage.setWorkDate(java.time.LocalDate.parse(req.getWorkDate()));
    } else {
        wage.setWorkDate(java.time.LocalDate.now());
    }
    wage.setHours(req.getHours());
    wage.setHourlyRate(req.getHourlyRate());
    wage.setAmount(req.getHours().multiply(req.getHourlyRate()));
    wage.setWorkContent(req.getWorkContent());
    wage.setCreatedBy(StpUtil.getSession().getLong("employeeId"));
    timedWageMapper.insert(wage);
}

public void updateTimedWage(Long id, TimedWageRequest req) {
    com.zhiyi.erp.module.admin.entity.TimedWage wage = timedWageMapper.selectById(id);
    if (wage == null) throw new BizException("记录不存在");
    wage.setEmployeeId(req.getEmployeeId());
    if (req.getWorkDate() != null && !req.getWorkDate().isEmpty()) {
        wage.setWorkDate(java.time.LocalDate.parse(req.getWorkDate()));
    }
    wage.setHours(req.getHours());
    wage.setHourlyRate(req.getHourlyRate());
    wage.setAmount(req.getHours().multiply(req.getHourlyRate()));
    wage.setWorkContent(req.getWorkContent());
    timedWageMapper.updateById(wage);
}

public void deleteTimedWage(Long id) {
    timedWageMapper.deleteById(id);
}

// 扎号查询
public BundleDetailVO searchBundle(String code) {
    Bundle bundle;
    if (code.startsWith("BUN")) {
        bundle = bundleMapper.selectOne(new LambdaQueryWrapper<Bundle>().eq(Bundle::getGlobalBundleNo, code));
    } else {
        bundle = bundleMapper.selectOne(new LambdaQueryWrapper<Bundle>().eq(Bundle::getBundleNo, code));
    }
    if (bundle == null) throw new BizException("扎号不存在");

    BundleDetailVO vo = new BundleDetailVO();
    vo.setId(bundle.getId());
    vo.setGlobalBundleNo(bundle.getGlobalBundleNo());
    vo.setBundleNo(bundle.getBundleNo());
    vo.setQty(bundle.getQty());
    vo.setColor(bundle.getColor());
    vo.setSize(bundle.getSize());

    com.zhiyi.erp.module.order.entity.Order order = orderMapper.selectById(bundle.getOrderId());
    vo.setOrderNo(order != null ? order.getOrderNo() : "");
    if (order != null && order.getStyleId() != null) {
        com.zhiyi.erp.module.base.entity.Style style = styleMapper.selectById(order.getStyleId());
        vo.setStyleNo(style != null ? style.getStyleCode() : "");
    }

    // 判断扎号状态
    List<ProcessReportStatusVO> processes = buildProcessStatusList(bundle.getOrderId(), bundle.getId());
    boolean allDone = processes.stream().allMatch(p -> "DONE".equals(p.getStatus()));
    vo.setStatus(allDone ? "已完成" : "生产中");
    vo.setProcesses(processes);
    return vo;
}

// 订单查询
public OrderSearchVO searchOrder(String orderNo) {
    com.zhiyi.erp.module.order.entity.Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<com.zhiyi.erp.module.order.entity.Order>()
                    .eq(com.zhiyi.erp.module.order.entity.Order::getOrderNo, orderNo));
    if (order == null) throw new BizException("订单不存在");

    OrderSearchVO vo = new OrderSearchVO();
    vo.setId(order.getId());
    vo.setOrderNo(order.getOrderNo());
    vo.setTotalAmount(order.getTotalAmount());
    vo.setDeliveryDate(order.getDeliveryDate());
    vo.setStatus(order.getStatus());

    // 客户名
    if (order.getCustomerId() != null) {
        com.zhiyi.erp.module.base.entity.Customer customer = customerMapper.selectById(order.getCustomerId());
        vo.setCustomerName(customer != null ? customer.getName() : "");
    }

    // 订单明细
    List<com.zhiyi.erp.module.order.entity.OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<com.zhiyi.erp.module.order.entity.OrderItem>()
                    .eq(com.zhiyi.erp.module.order.entity.OrderItem::getOrderId, order.getId()));

    List<Bundle> bundles = bundleMapper.selectList(
            new LambdaQueryWrapper<Bundle>().eq(Bundle::getOrderId, order.getId()));
    List<Long> bundleIds = bundles.stream().map(Bundle::getId).collect(Collectors.toList());

    List<com.zhiyi.erp.module.report.entity.WorkReport> reports = bundleIds.isEmpty() ? List.of() :
            workReportMapper.selectList(new LambdaQueryWrapper<com.zhiyi.erp.module.report.entity.WorkReport>()
                    .in(com.zhiyi.erp.module.report.entity.WorkReport::getBundleId, bundleIds)
                    .eq(com.zhiyi.erp.module.report.entity.WorkReport::getStatus, 1));

    List<OrderSearchVO.OrderItemVO> itemVOs = items.stream().map(item -> {
        OrderSearchVO.OrderItemVO ivo = new OrderSearchVO.OrderItemVO();
        if (item.getStyleId() != null) {
            com.zhiyi.erp.module.base.entity.Style style = styleMapper.selectById(item.getStyleId());
            ivo.setStyleNo(style != null ? style.getStyleCode() : "");
        }
        ivo.setColor(item.getColor());
        ivo.setSize(item.getSize());
        ivo.setQty(item.getQty());

        // 计算出货和进度
        long shippedCount = bundles.stream()
                .filter(b -> b.getColor().equals(item.getColor()) && b.getSize().equals(item.getSize()))
                .filter(b -> reports.stream().anyMatch(r -> r.getBundleId().equals(b.getId())))
                .count();
        long totalForItem = bundles.stream()
                .filter(b -> b.getColor().equals(item.getColor()) && b.getSize().equals(item.getSize()))
                .count();
        ivo.setShippedQty((int) shippedCount);
        if (totalForItem > 0) {
            ivo.setProgress(java.math.BigDecimal.valueOf(shippedCount)
                    .divide(java.math.BigDecimal.valueOf(totalForItem), 4, java.math.RoundingMode.HALF_UP));
        }
        return ivo;
    }).collect(Collectors.toList());
    vo.setItems(itemVOs);

    // 整体完成率
    if (!bundleIds.isEmpty()) {
        long completedBundles = reports.stream().map(com.zhiyi.erp.module.report.entity.WorkReport::getBundleId).distinct().count();
        vo.setCompletionRate(java.math.BigDecimal.valueOf(completedBundles)
                .divide(java.math.BigDecimal.valueOf(bundleIds.size()), 4, java.math.RoundingMode.HALF_UP));
    }
    return vo;
}
```

在 `MobileService` 中注入缺少的依赖：

```java
@Autowired
private com.zhiyi.erp.module.admin.mapper.AnomalyAlertMapper anomalyAlertMapper;
@Autowired
private com.zhiyi.erp.module.admin.service.AnomalyService anomalyService;
@Autowired
private com.zhiyi.erp.module.admin.mapper.TimedWageMapper timedWageMapper;
@Autowired
private com.zhiyi.erp.module.base.mapper.CustomerMapper customerMapper;
@Autowired
private com.zhiyi.erp.module.order.mapper.OrderItemMapper orderItemMapper;
```

- [ ] **Step 4: 创建 MobileAdminController**

`MobileAdminController.java`:
```java
package com.zhiyi.erp.module.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.mobile.dto.*;
import com.zhiyi.erp.module.mobile.service.MobileService;
import com.zhiyi.erp.module.mobile.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "移动端管理", description = "看板、异常、计时、查询")
@RestController
@RequestMapping("/api/mobile/admin")
public class MobileAdminController {

    @Autowired
    private MobileService mobileService;

    @Operation(summary = "订单维度看板")
    @GetMapping("/dashboard/orders")
    public R<List<DashboardOrderVO>> getDashboardOrders(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long orderId) {
        return R.ok(mobileService.getDashboardOrders(date, orderId));
    }

    @Operation(summary = "工人维度看板")
    @GetMapping("/dashboard/workers")
    public R<List<DashboardWorkerVO>> getDashboardWorkers(
            @RequestParam(required = false) String date) {
        if (date == null || date.isEmpty()) date = LocalDate.now().toString();
        return R.ok(mobileService.getDashboardWorkers(date));
    }

    @Operation(summary = "异常列表")
    @GetMapping("/anomalies")
    public R<List<AnomalyVO>> getAnomalies(@RequestParam(required = false) Integer status) {
        return R.ok(mobileService.getAnomalies(status));
    }

    @Operation(summary = "忽略异常")
    @PostMapping("/anomalies/{id}/ignore")
    public R<Void> ignoreAnomaly(@PathVariable Long id, @Valid @RequestBody AnomalyIgnoreRequest req) {
        mobileService.ignoreAnomaly(id, req.getRemark());
        return R.ok();
    }

    @Operation(summary = "清除异常")
    @PostMapping("/anomalies/{id}/clear")
    public R<Void> clearAnomaly(@PathVariable Long id, @Valid @RequestBody AnomalyClearRequest req) {
        mobileService.clearAnomaly(id, req.getReason());
        return R.ok();
    }

    @Operation(summary = "补录异常")
    @PostMapping("/anomalies/{id}/supplement")
    public R<Void> supplementAnomaly(@PathVariable Long id, @Valid @RequestBody AnomalySupplementRequest req) {
        mobileService.supplementAnomaly(id, req.getEmployeeId(), req.getQty());
        return R.ok();
    }

    @Operation(summary = "计时工资列表")
    @GetMapping("/timed-wages")
    public R<List<TimedWageVO>> getTimedWages(@RequestParam(required = false) String yearMonth) {
        return R.ok(mobileService.getTimedWages(yearMonth));
    }

    @Operation(summary = "新增计时工资")
    @PostMapping("/timed-wages")
    public R<Void> createTimedWage(@Valid @RequestBody TimedWageRequest req) {
        mobileService.createTimedWage(req);
        return R.ok();
    }

    @Operation(summary = "编辑计时工资")
    @PutMapping("/timed-wages/{id}")
    public R<Void> updateTimedWage(@PathVariable Long id, @Valid @RequestBody TimedWageRequest req) {
        mobileService.updateTimedWage(id, req);
        return R.ok();
    }

    @Operation(summary = "删除计时工资")
    @DeleteMapping("/timed-wages/{id}")
    public R<Void> deleteTimedWage(@PathVariable Long id) {
        mobileService.deleteTimedWage(id);
        return R.ok();
    }

    @Operation(summary = "扎号查询")
    @GetMapping("/bundles/search")
    public R<BundleDetailVO> searchBundle(@RequestParam String code) {
        return R.ok(mobileService.searchBundle(code));
    }

    @Operation(summary = "订单查询")
    @GetMapping("/orders/search")
    public R<OrderSearchVO> searchOrder(@RequestParam String orderNo) {
        return R.ok(mobileService.searchOrder(orderNo));
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `cd zhiyi-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add zhiyi-server/src/main/java/com/zhiyi/erp/module/mobile/
git commit -m "feat(mobile): add admin APIs for dashboard, anomaly, timed wage, and queries"
```

---

## Task 5: 前端 API 层与工具重构

**Files:**
- Modify: `zhiyi-mini/src/utils/request.ts`
- Create: `zhiyi-mini/src/api/types.ts`
- Create: `zhiyi-mini/src/api/mobileAuth.ts`
- Create: `zhiyi-mini/src/api/mobileReport.ts`
- Create: `zhiyi-mini/src/api/mobileAdmin.ts`
- Create: `zhiyi-mini/src/utils/auth.ts`
- Create: `zhiyi-mini/src/utils/format.ts`

- [ ] **Step 1: 重写 request.ts**

替换 `zhiyi-mini/src/utils/request.ts` 为：

```typescript
import Taro from '@tarojs/taro'

// 根据构建环境配置 API 地址
const BASE_URL = process.env.NODE_ENV === 'production'
  ? 'https://your-domain.com/api'
  : 'http://localhost:8080/api'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  params?: Record<string, any>
}

export function request<T = any>(options: RequestOptions): Promise<T> {
  const token = Taro.getStorageSync('satoken')
  const headers: Record<string, string> = {}
  if (token) {
    headers.satoken = token
  }

  let url = BASE_URL + options.url
  if (options.params) {
    const query = Object.entries(options.params)
      .filter(([, v]) => v !== undefined && v !== null)
      .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
      .join('&')
    if (query) url += '?' + query
  }

  return new Promise((resolve, reject) => {
    Taro.request({
      url,
      method: options.method || 'GET',
      data: options.data,
      header: headers,
      success(res) {
        if (res.data.code === 200) {
          resolve(res.data.data)
        } else if (res.data.code === 401) {
          clearAuth()
          Taro.redirectTo({ url: '/pages/login/index' })
          reject(res.data)
        } else {
          Taro.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail(err) {
        Taro.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        reject(err)
      },
    })
  })
}

export function get<T = any>(url: string, params?: Record<string, any>) {
  return request<T>({ url, method: 'GET', params })
}

export function post<T = any>(url: string, data?: any) {
  return request<T>({ url, method: 'POST', data })
}

export function put<T = any>(url: string, data?: any) {
  return request<T>({ url, method: 'PUT', data })
}

export function del<T = any>(url: string, params?: Record<string, any>) {
  return request<T>({ url, method: 'DELETE', params })
}

// 认证相关
export function clearAuth() {
  Taro.removeStorageSync('satoken')
  Taro.removeStorageSync('userInfo')
  Taro.removeStorageSync('menus')
  Taro.removeStorageSync('permissions')
  Taro.removeStorageSync('defaultHomeType')
}

export function saveAuth(data: {
  token: string
  employee: any
  role: any
  menus: any[]
  permissions: string[]
  defaultHomeType: string
}) {
  Taro.setStorageSync('satoken', data.token)
  Taro.setStorageSync('userInfo', JSON.stringify({
    employee: data.employee,
    role: data.role,
  }))
  Taro.setStorageSync('menus', JSON.stringify(data.menus))
  Taro.setStorageSync('permissions', JSON.stringify(data.permissions))
  Taro.setStorageSync('defaultHomeType', data.defaultHomeType)
}

export function getToken(): string {
  return Taro.getStorageSync('satoken') || ''
}

export function getUserInfo() {
  const raw = Taro.getStorageSync('userInfo')
  return raw ? JSON.parse(raw) : null
}

export function getMenus() {
  const raw = Taro.getStorageSync('menus')
  return raw ? JSON.parse(raw) : []
}

export function getPermissions(): string[] {
  const raw = Taro.getStorageSync('permissions')
  return raw ? JSON.parse(raw) : []
}

export function hasPermission(code: string): boolean {
  return getPermissions().includes(code)
}
```

- [ ] **Step 2: 创建 types.ts**

`zhiyi-mini/src/api/types.ts`:
```typescript
// 登录
export interface MobileLoginResult {
  token: string
  employee: { id: number; code: string; name: string }
  role: { id: number; name: string }
  menus: MobileMenu[]
  permissions: string[]
  defaultHomeType: 'employee' | 'admin'
}

export interface MobileMenu {
  code: string
  title: string
  path: string
  group: 'employee' | 'admin'
  icon?: string
  sort: number
}

export interface MobileProfile {
  employee: { id: number; code: string; name: string }
  role: { id: number; name: string }
  menus: MobileMenu[]
  permissions: string[]
  defaultHomeType: 'employee' | 'admin'
}

// 扫码
export interface BundleScanResult {
  scanType: 'BUNDLE'
  code: string
  bundle: {
    id: number
    globalBundleNo: string
    bundleNo: string
    qty: number
    orderNo: string
    styleNo: string
    color: string
    size: string
  }
  processes: ProcessReportStatus[]
}

export interface CuttingScanResult {
  scanType: 'CUTTING'
  code: string
  cutting: {
    id: number
    barcode: string
    bedNo: number
    totalBundleCount: number
    totalQty: number
    orderNo: string
    styleNo: string
    color: string
    size: string
  }
  bundles: Array<{
    id: number
    globalBundleNo: string
    bundleNo: string
    qty: number
    disabled?: boolean
    disabledReason?: string
  }>
  processes: ProcessReportStatus[]
}

export interface ProcessReportStatus {
  processId: number
  processName: string
  price: number
  status: 'AVAILABLE' | 'DONE' | 'PARTIAL_DONE' | 'UNAVAILABLE'
  doneBy?: string
  doneTime?: string
  hint?: string
}

export interface ReportSubmitResult {
  totalQty: number
  totalAmount: number
  items: Array<{
    processId: number
    processName: string
    qty: number
    price: number
    amount: number
  }>
  skippedItems?: Array<{
    bundleNo: string
    processName: string
    reason: string
  }>
}

// 工资
export interface WageSummary {
  todayAmount: number
  monthAmount: number
  dailyGroups: Array<{
    date: string
    amount: number
    records: WorkReportRecord[]
  }>
}

export interface WorkReportRecord {
  id: number
  reportTime: string
  orderNo: string
  styleNo: string
  bundleNo: string
  processName: string
  qty: number
  price: number
  amount: number
}

// 管理员 - 看板
export interface DashboardOrder {
  orderId: number
  orderNo: string
  styleNo: string
  totalBundles: number
  completionRate: number
  bottleneckProcess: string
  processProgress: Array<{
    processId: number
    processName: string
    completed: number
    total: number
    rate: number
  }>
}

export interface DashboardWorker {
  employeeId: number
  employeeNo: string
  employeeName: string
  totalQty: number
  totalAmount: number
}

// 管理员 - 异常
export interface AnomalyRecord {
  id: number
  bundleNo: string
  orderNo: string
  description: string
  frontProcessName: string
  backProcessName: string
  reportEmployeeName: string
  reportTime: string
  status: number
  handleType: number
  handleRemark: string
}

// 管理员 - 计时工资
export interface TimedWageRecord {
  id: number
  employeeId: number
  employeeNo: string
  employeeName: string
  workDate: string
  hours: number
  hourlyRate: number
  amount: number
  workContent: string
}

// 管理员 - 扎号查询
export interface BundleDetail {
  id: number
  globalBundleNo: string
  bundleNo: string
  qty: number
  orderNo: string
  styleNo: string
  color: string
  size: string
  status: string
  processes: ProcessReportStatus[]
}

// 管理员 - 订单查询
export interface OrderSearchResult {
  id: number
  orderNo: string
  customerName: string
  totalAmount: number
  deliveryDate: string
  status: number
  completionRate: number
  items: Array<{
    styleNo: string
    color: string
    size: string
    qty: number
    shippedQty: number
    progress: number
  }>
}
```

- [ ] **Step 3: 创建 mobileAuth.ts**

`zhiyi-mini/src/api/mobileAuth.ts`:
```typescript
import { get, post } from '../utils/request'
import type { MobileLoginResult, MobileProfile } from './types'

export function loginByCode(loginCode: string) {
  return post<MobileLoginResult>('/mobile/auth/login-by-code', { loginCode })
}

export function loginByAccount(account: string, password: string) {
  return post<MobileLoginResult>('/mobile/auth/login-by-account', { account, password })
}

export function logout() {
  return post<void>('/mobile/auth/logout')
}

export function getProfile() {
  return get<MobileProfile>('/mobile/auth/profile')
}
```

- [ ] **Step 4: 创建 mobileReport.ts**

`zhiyi-mini/src/api/mobileReport.ts`:
```typescript
import { get, post } from '../utils/request'
import type { BundleScanResult, CuttingScanResult, ReportSubmitResult, WageSummary, WorkReportRecord } from './types'

export function resolveScan(code: string) {
  return post<BundleScanResult | CuttingScanResult>('/mobile/report/scan/resolve', { code })
}

export function submitReport(data: {
  scanType: string
  code: string
  selectedBundleIds: number[]
  selectedProcessIds: number[]
}) {
  return post<ReportSubmitResult>('/mobile/report/submit', data)
}

export function getWageSummary() {
  return get<WageSummary>('/mobile/report/wage-summary')
}

export function getReportRecords(startDate?: string, endDate?: string) {
  return get<WorkReportRecord[]>('/mobile/report/records', { startDate, endDate })
}
```

- [ ] **Step 5: 创建 mobileAdmin.ts**

`zhiyi-mini/src/api/mobileAdmin.ts`:
```typescript
import { get, post, put, del } from '../utils/request'
import type { DashboardOrder, DashboardWorker, AnomalyRecord, TimedWageRecord, BundleDetail, OrderSearchResult } from './types'

// 看板
export function getDashboardOrders(date?: string, orderId?: number) {
  return get<DashboardOrder[]>('/mobile/admin/dashboard/orders', { date, orderId })
}

export function getDashboardWorkers(date?: string) {
  return get<DashboardWorker[]>('/mobile/admin/dashboard/workers', { date })
}

// 异常
export function getAnomalies(status?: number) {
  return get<AnomalyRecord[]>('/mobile/admin/anomalies', { status })
}

export function ignoreAnomaly(id: number, remark: string) {
  return post<void>(`/mobile/admin/anomalies/${id}/ignore`, { remark })
}

export function clearAnomaly(id: number, reason: string) {
  return post<void>(`/mobile/admin/anomalies/${id}/clear`, { reason })
}

export function supplementAnomaly(id: number, data: { employeeId: number; qty: number; reportTime?: string }) {
  return post<void>(`/mobile/admin/anomalies/${id}/supplement`, data)
}

// 计时工资
export function getTimedWages(yearMonth?: string) {
  return get<TimedWageRecord[]>('/mobile/admin/timed-wages', { yearMonth })
}

export function createTimedWage(data: {
  employeeId: number
  workDate?: string
  hours: number
  hourlyRate: number
  workContent?: string
}) {
  return post<void>('/mobile/admin/timed-wages', data)
}

export function updateTimedWage(id: number, data: {
  employeeId: number
  workDate?: string
  hours: number
  hourlyRate: number
  workContent?: string
}) {
  return put<void>(`/mobile/admin/timed-wages/${id}`, data)
}

export function deleteTimedWage(id: number) {
  return del<void>(`/mobile/admin/timed-wages/${id}`)
}

// 查询
export function searchBundle(code: string) {
  return get<BundleDetail>('/mobile/admin/bundles/search', { code })
}

export function searchOrder(orderNo: string) {
  return get<OrderSearchResult>('/mobile/admin/orders/search', { orderNo })
}
```

- [ ] **Step 6: 创建 auth.ts 和 format.ts**

`zhiyi-mini/src/utils/auth.ts`:
```typescript
import Taro from '@tarojs/taro'
import { clearAuth as clearAuthRequest, getToken, getMenus, getPermissions } from './request'

export { getToken, getMenus, getPermissions }

export function isLoggedIn(): boolean {
  return !!getToken()
}

export function hasMenu(code: string): boolean {
  const menus = getMenus()
  return menus.some((m: any) => m.code === code)
}

export function hasPermission(code: string): boolean {
  return getPermissions().includes(code)
}

export function getDefaultHomeType(): string {
  return Taro.getStorageSync('defaultHomeType') || 'employee'
}

export function clearAuth() {
  clearAuthRequest()
}

export function checkAuthAndRedirect() {
  if (!isLoggedIn()) {
    Taro.redirectTo({ url: '/pages/login/index' })
    return false
  }
  return true
}
```

`zhiyi-mini/src/utils/format.ts`:
```typescript
export function formatAmount(amount: number): string {
  return '¥' + (amount / 100).toFixed(2)
}

export function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

export function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  return dateStr.substring(0, 16).replace('T', ' ')
}

export function formatPercent(rate: number): string {
  return (rate * 100).toFixed(1) + '%'
}
```

- [ ] **Step 7: 构建验证**

Run: `cd zhiyi-mini && npx tsc --noEmit 2>&1 | head -20`
Expected: 无类型错误

- [ ] **Step 8: Commit**

```bash
git add zhiyi-mini/src/utils/ zhiyi-mini/src/api/
git commit -m "feat(mobile): rewrite frontend API layer, types, and auth utilities"
```

---

## Task 6: 前端通用组件

**Files:**
- Create: `zhiyi-mini/src/components/AppCard.vue`
- Create: `zhiyi-mini/src/components/StatusTag.vue`
- Create: `zhiyi-mini/src/components/EmptyState.vue`
- Create: `zhiyi-mini/src/components/ConfirmPanel.vue`
- Create: `zhiyi-mini/src/components/ProcessCheckList.vue`

- [ ] **Step 1: 创建 AppCard.vue**

```vue
<template>
  <View class="app-card" @tap="$emit('tap')">
    <View class="app-card-icon" v-if="icon">
      <Text class="icon-text">{{ icon }}</Text>
    </View>
    <View class="app-card-body">
      <Text class="app-card-title">{{ title }}</Text>
      <Text class="app-card-desc" v-if="desc">{{ desc }}</Text>
    </View>
    <View class="app-card-arrow" v-if="arrow">
      <Text class="arrow-text">›</Text>
    </View>
  </View>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string
  title: string
  desc?: string
  arrow?: boolean
}>()

defineEmits(['tap'])
</script>

<style lang="scss">
.app-card {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.app-card-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24px;
  flex-shrink: 0;
}
.icon-text {
  font-size: 36px;
  color: #fff;
}
.app-card-body {
  flex: 1;
  min-width: 0;
}
.app-card-title {
  display: block;
  font-size: 30px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 6px;
}
.app-card-desc {
  display: block;
  font-size: 24px;
  color: #9ca3af;
}
.app-card-arrow {
  flex-shrink: 0;
  padding-left: 16px;
}
.arrow-text {
  font-size: 36px;
  color: #d1d5db;
}
</style>
```

- [ ] **Step 2: 创建 StatusTag.vue**

```vue
<template>
  <View class="status-tag" :class="typeClass">
    <Text class="status-tag-text">{{ text }}</Text>
  </View>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text: string
  type?: 'success' | 'warning' | 'danger' | 'info' | 'default'
}>()

const typeClass = computed(() => `status-tag--${props.type || 'default'}`)
</script>

<style lang="scss">
.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 16px;
  border-radius: 8px;
  font-size: 22px;
}
.status-tag-text {
  font-size: 22px;
}
.status-tag--success { background: #d1fae5; }
.status-tag--success .status-tag-text { color: #065f46; }
.status-tag--warning { background: #fef3c7; }
.status-tag--warning .status-tag-text { color: #92400e; }
.status-tag--danger { background: #fee2e2; }
.status-tag--danger .status-tag-text { color: #991b1b; }
.status-tag--info { background: #dbeafe; }
.status-tag--info .status-tag-text { color: #1e40af; }
.status-tag--default { background: #f3f4f6; }
.status-tag--default .status-tag-text { color: #6b7280; }
</style>
```

- [ ] **Step 3: 创建 EmptyState.vue**

```vue
<template>
  <View class="empty-state">
    <Text class="empty-icon">📋</Text>
    <Text class="empty-text">{{ text || '暂无数据' }}</Text>
  </View>
</template>

<script setup lang="ts">
defineProps<{ text?: string }>()
</script>

<style lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}
.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}
.empty-text {
  font-size: 28px;
  color: #9ca3af;
}
</style>
```

- [ ] **Step 4: 创建 ConfirmPanel.vue**

```vue
<template>
  <View class="confirm-panel" v-if="visible">
    <View class="confirm-mask" @tap="$emit('cancel')" />
    <View class="confirm-body">
      <Text class="confirm-title">{{ title }}</Text>
      <View class="confirm-content">
        <slot />
      </View>
      <View class="confirm-actions">
        <View class="confirm-btn cancel" @tap="$emit('cancel')">
          <Text class="btn-text">取消</Text>
        </View>
        <View class="confirm-btn submit" :class="{ loading }" @tap="handleSubmit">
          <Text class="btn-text">{{ confirmText || '确认' }}</Text>
        </View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  title: string
  confirmText?: string
  loading?: boolean
}>()

const emit = defineEmits(['confirm', 'cancel'])

function handleSubmit() {
  emit('confirm')
}
</script>

<style lang="scss">
.confirm-panel {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.confirm-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
}
.confirm-body {
  position: relative;
  width: 80%;
  max-width: 600px;
  background: #fff;
  border-radius: 20px;
  padding: 40px 32px 32px;
  z-index: 1;
}
.confirm-title {
  display: block;
  font-size: 34px;
  font-weight: 600;
  color: #1a1a2e;
  text-align: center;
  margin-bottom: 24px;
}
.confirm-content {
  margin-bottom: 32px;
}
.confirm-actions {
  display: flex;
  gap: 24px;
}
.confirm-btn {
  flex: 1;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}
.confirm-btn.cancel {
  background: #f3f4f6;
}
.confirm-btn.cancel .btn-text { color: #6b7280; }
.confirm-btn.submit {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.confirm-btn.submit .btn-text { color: #fff; }
.confirm-btn.loading { opacity: 0.6; }
.btn-text {
  font-size: 30px;
  font-weight: 500;
}
</style>
```

- [ ] **Step 5: 创建 ProcessCheckList.vue**

```vue
<template>
  <View class="process-list">
    <View
      class="process-item"
      v-for="p in processes"
      :key="p.processId"
      :class="{ done: p.status === 'DONE', selected: isSelected(p.processId) }"
      @tap="toggleProcess(p)"
    >
      <View class="process-check">
        <View class="check-box" v-if="p.status === 'DONE'">
          <Text class="check-icon">✓</Text>
        </View>
        <View class="check-box unchecked" v-else>
          <View class="check-inner" v-if="isSelected(p.processId)" />
        </View>
      </View>
      <View class="process-info">
        <Text class="process-name">{{ p.processName }}</Text>
        <Text class="process-price" v-if="p.status !== 'DONE'">¥{{ p.price }}/件</Text>
        <Text class="process-done-info" v-if="p.status === 'DONE' && p.doneBy">
          已完成（{{ p.doneBy }}）
        </Text>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ProcessReportStatus } from '../api/types'

const props = defineProps<{
  processes: ProcessReportStatus[]
}>()

const selectedIds = ref<number[]>([])

const emit = defineEmits<{
  (e: 'change', ids: number[]): void
}>()

watch(() => props.processes, () => {
  selectedIds.value = []
  emit('change', [])
}, { immediate: true })

function isSelected(id: number) {
  return selectedIds.value.includes(id)
}

function toggleProcess(p: ProcessReportStatus) {
  if (p.status === 'DONE') return
  const idx = selectedIds.value.indexOf(p.processId)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(p.processId)
  }
  emit('change', [...selectedIds.value])
}
</script>

<style lang="scss">
.process-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.process-item {
  display: flex;
  align-items: center;
  padding: 20px 16px;
  background: #f9fafb;
  border-radius: 12px;
  border: 2px solid transparent;
}
.process-item.selected {
  border-color: #667eea;
  background: #f0f0ff;
}
.process-item.done {
  opacity: 0.5;
  background: #f3f4f6;
}
.process-check {
  margin-right: 16px;
  flex-shrink: 0;
}
.check-box {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #10b981;
  display: flex;
  align-items: center;
  justify-content: center;
}
.check-icon {
  color: #fff;
  font-size: 24px;
}
.check-box.unchecked {
  background: #fff;
  border: 2px solid #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
}
.check-inner {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: #667eea;
}
.process-info {
  flex: 1;
}
.process-name {
  display: block;
  font-size: 28px;
  font-weight: 500;
  color: #1a1a2e;
}
.process-price {
  display: block;
  font-size: 24px;
  color: #9ca3af;
  margin-top: 4px;
}
.process-done-info {
  display: block;
  font-size: 24px;
  color: #6b7280;
  margin-top: 4px;
}
</style>
```

- [ ] **Step 6: Commit**

```bash
git add zhiyi-mini/src/components/
git commit -m "feat(mobile): add common UI components for mini program"
```

---

## Task 7: 前端登录页重写

**Files:**
- Modify: `zhiyi-mini/src/pages/login/index.vue`

- [ ] **Step 1: 重写登录页**

替换 `zhiyi-mini/src/pages/login/index.vue` 为：

```vue
<template>
  <View class="login-page">
    <View class="login-header">
      <Text class="login-logo">智衣ERP</Text>
      <Text class="login-subtitle">制衣厂生产管理系统</Text>
    </View>

    <View class="login-tabs">
      <View
        class="tab-item"
        :class="{ active: activeTab === 'employee' }"
        @tap="activeTab = 'employee'"
      >
        <Text class="tab-text">员工登录</Text>
      </View>
      <View
        class="tab-item"
        :class="{ active: activeTab === 'admin' }"
        @tap="activeTab = 'admin'"
      >
        <Text class="tab-text">管理员登录</Text>
      </View>
    </View>

    <View class="login-card">
      <!-- 员工登录 -->
      <View v-if="activeTab === 'employee'" class="login-form">
        <View class="input-group">
          <Text class="input-label">工号 / 登录码</Text>
          <Input
            class="login-input"
            type="text"
            placeholder="请输入工号"
            :value="loginCode"
            @input="loginCode = $event.detail.value"
            @confirm="handleLogin"
          />
        </View>
        <Button class="login-btn" :loading="loading" @tap="handleLogin">
          登 录
        </Button>
      </View>

      <!-- 管理员登录 -->
      <View v-if="activeTab === 'admin'" class="login-form">
        <View class="input-group">
          <Text class="input-label">账号</Text>
          <Input
            class="login-input"
            type="text"
            placeholder="请输入账号"
            :value="account"
            @input="account = $event.detail.value"
          />
        </View>
        <View class="input-group">
          <Text class="input-label">密码</Text>
          <Input
            class="login-input"
            type="password"
            placeholder="请输入密码"
            :value="password"
            @input="password = $event.detail.value"
            @confirm="handleLogin"
          />
        </View>
        <Button class="login-btn" :loading="loading" @tap="handleLogin">
          登 录
        </Button>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Taro from '@tarojs/taro'
import { loginByCode, loginByAccount } from '../../api/mobileAuth'
import { saveAuth } from '../../utils/request'

definePageConfig({
  navigationBarTitleText: '登录',
})

const activeTab = ref<'employee' | 'admin'>('employee')
const loginCode = ref('')
const account = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (activeTab.value === 'employee') {
    const code = loginCode.value.trim()
    if (!code) {
      Taro.showToast({ title: '请输入工号', icon: 'none' })
      return
    }
    loading.value = true
    try {
      const res = await loginByCode(code)
      saveAuth(res)
      Taro.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        Taro.switchTab({ url: '/pages/index/index' })
      }, 800)
    } catch {
      // error handled by request util
    } finally {
      loading.value = false
    }
  } else {
    const acc = account.value.trim()
    const pwd = password.value.trim()
    if (!acc || !pwd) {
      Taro.showToast({ title: '请输入账号和密码', icon: 'none' })
      return
    }
    loading.value = true
    try {
      const res = await loginByAccount(acc, pwd)
      saveAuth(res)
      Taro.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        Taro.switchTab({ url: '/pages/index/index' })
      }, 800)
    } catch {
      // error handled by request util
    } finally {
      loading.value = false
    }
  }
}
</script>

<style lang="scss">
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0 40px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
.login-header {
  padding-top: 120px;
  margin-bottom: 48px;
  text-align: center;
}
.login-logo {
  display: block;
  font-size: 56px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 12px;
}
.login-subtitle {
  display: block;
  font-size: 28px;
  color: rgba(255, 255, 255, 0.8);
}
.login-tabs {
  display: flex;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 24px 0;
}
.tab-item.active {
  background: #ffffff;
}
.tab-text {
  font-size: 30px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
}
.tab-item.active .tab-text {
  color: #667eea;
}
.login-card {
  background: #ffffff;
  border-radius: 0 0 16px 16px;
  padding: 40px 32px;
}
.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.input-label {
  font-size: 26px;
  color: #6b7280;
  font-weight: 500;
}
.login-input {
  width: 100%;
  height: 88px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 0 24px;
  font-size: 30px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.login-input:focus {
  border-color: #667eea;
}
.login-btn {
  width: 100%;
  height: 88px;
  line-height: 88px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-size: 32px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  margin-top: 16px;
}
</style>
```

- [ ] **Step 2: 构建验证**

Run: `cd zhiyi-mini && npm run build:weapp 2>&1 | tail -5`
Expected: 构建成功

- [ ] **Step 3: Commit**

```bash
git add zhiyi-mini/src/pages/login/index.vue
git commit -m "feat(mobile): rewrite login page with dual tab (employee/admin)"
```

---

## Task 8: 前端首页重写

**Files:**
- Modify: `zhiyi-mini/src/pages/index/index.vue`

- [ ] **Step 1: 重写首页**

替换 `zhiyi-mini/src/pages/index/index.vue` 为：

```vue
<template>
  <View class="home-page">
    <View class="home-header">
      <View class="header-info">
        <Text class="header-name">{{ userInfo?.employee?.name || '用户' }}</Text>
        <Text class="header-code">{{ userInfo?.employee?.code || '' }}</Text>
      </View>
      <View class="header-logout" @tap="handleLogout">
        <Text class="logout-text">退出</Text>
      </View>
    </View>

    <!-- 员工入口 -->
    <View class="section" v-if="employeeMenus.length">
      <Text class="section-title">我的工作</Text>
      <View class="menu-grid">
        <View
          class="menu-item"
          v-for="menu in employeeMenus"
          :key="menu.code"
          @tap="navigateTo(menu.path)"
        >
          <View class="menu-icon" :class="menu.code">
            <Text class="menu-icon-text">{{ getIcon(menu.code) }}</Text>
          </View>
          <Text class="menu-title">{{ menu.title }}</Text>
        </View>
      </View>
    </View>

    <!-- 管理员入口 -->
    <View class="section" v-if="adminMenus.length">
      <Text class="section-title">管理工具</Text>
      <View class="menu-grid">
        <View
          class="menu-item"
          v-for="menu in adminMenus"
          :key="menu.code"
          @tap="navigateTo(menu.path)"
        >
          <View class="menu-icon admin" :class="menu.code">
            <Text class="menu-icon-text">{{ getIcon(menu.code) }}</Text>
          </View>
          <Text class="menu-title">{{ menu.title }}</Text>
        </View>
      </View>
    </View>
  </View>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Taro, { useDidShow } from '@tarojs/taro'
import { getUserInfo, getMenus, clearAuth, isLoggedIn } from '../../utils/auth'
import { logout } from '../../api/mobileAuth'
import { getProfile } from '../../api/mobileAuth'
import { saveAuth } from '../../utils/request'
import type { MobileMenu } from '../../api/types'

definePageConfig({
  navigationBarTitleText: '首页',
})

const userInfo = ref<any>(null)
const menus = ref<MobileMenu[]>([])

const employeeMenus = computed(() => menus.value.filter(m => m.group === 'employee'))
const adminMenus = computed(() => menus.value.filter(m => m.group === 'admin'))

useDidShow(async () => {
  if (!isLoggedIn()) {
    Taro.redirectTo({ url: '/pages/login/index' })
    return
  }
  // 从本地缓存读取
  userInfo.value = getUserInfo()
  menus.value = getMenus()

  // 异步刷新 profile
  try {
    const profile = await getProfile()
    saveAuth({
      token: Taro.getStorageSync('satoken'),
      employee: profile.employee,
      role: profile.role,
      menus: profile.menus,
      permissions: profile.permissions,
      defaultHomeType: profile.defaultHomeType,
    })
    userInfo.value = getUserInfo()
    menus.value = getMenus()
  } catch {
    // token 失效会在 request 层处理
  }
})

function getIcon(code: string): string {
  const icons: Record<string, string> = {
    'mobile:scan': '📷',
    'mobile:wage': '💰',
    'mobile:report-records': '📋',
    'mobile:dashboard': '📊',
    'mobile:anomaly': '⚠️',
    'mobile:timed-wage': '⏰',
    'mobile:bundle-query': '🔍',
    'mobile:order-query': '📦',
  }
  return icons[code] || '📌'
}

function navigateTo(path: string) {
  if (path.startsWith('/pages/scan') || path.startsWith('/pages/index') || path.startsWith('/pages/wage')) {
    Taro.switchTab({ url: path })
  } else {
    Taro.navigateTo({ url: path })
  }
}

async function handleLogout() {
  try {
    await logout()
  } catch {}
  clearAuth()
  Taro.redirectTo({ url: '/pages/login/index' })
}
</script>

<style lang="scss">
.home-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 120px;
}
.home-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 48px 32px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-info {
  flex: 1;
}
.header-name {
  display: block;
  font-size: 40px;
  font-weight: 700;
  color: #ffffff;
}
.header-code {
  display: block;
  font-size: 26px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4px;
}
.header-logout {
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
}
.logout-text {
  font-size: 26px;
  color: #ffffff;
}
.section {
  padding: 24px 24px 0;
}
.section-title {
  display: block;
  font-size: 30px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 20px;
}
.menu-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}
.menu-icon {
  width: 96px;
  height: 96px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.menu-icon.admin {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}
.menu-icon-text {
  font-size: 40px;
}
.menu-title {
  font-size: 24px;
  color: #374151;
  font-weight: 500;
}
</style>
```

- [ ] **Step 2: 更新 app.config.ts 路由**

替换 `zhiyi-mini/src/app.config.ts` 为：

```typescript
export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/login/index',
    'pages/scan/index',
    'pages/wage/index',
    'pages/report-records/index',
    'pages/admin/dashboard/index',
    'pages/admin/anomaly/index',
    'pages/admin/anomaly-detail/index',
    'pages/admin/timed-wage/index',
    'pages/admin/bundle-query/index',
    'pages/admin/order-query/index',
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#667eea',
    navigationBarTitleText: '智衣ERP',
    navigationBarTextStyle: 'white',
  },
  tabBar: {
    color: '#9ca3af',
    selectedColor: '#667eea',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '首页',
      },
      {
        pagePath: 'pages/scan/index',
        text: '扫码',
      },
      {
        pagePath: 'pages/wage/index',
        text: '我的',
      },
    ],
  },
})
```

- [ ] **Step 3: 构建验证**

Run: `cd zhiyi-mini && npm run build:weapp 2>&1 | tail -5`
Expected: 构建成功

- [ ] **Step 4: Commit**

```bash
git add zhiyi-mini/src/pages/index/index.vue zhiyi-mini/src/app.config.ts
git commit -m "feat(mobile): rewrite home page with dynamic menu and update routes"
```

---

## Task 9: 前端扫码报工页重写

**Files:**
- Modify: `zhiyi-mini/src/pages/scan/index.vue`

- [ ] **Step 1: 重写扫码页为三步流程**

替换 `zhiyi-mini/src/pages/scan/index.vue` 为完整三步扫码报工流程页面。该页面包含：
- 第一步：扫码/手动输入按钮
- 第二步：展示解析结果、扎号选择、工序选择
- 第三步：确认上报并展示汇总

（完整代码较长，实施时按设计文档中描述的三步流程编写，使用 `resolveScan` 和 `submitReport` API，引用 `ProcessCheckList` 组件展示工序列表。）

- [ ] **Step 2: 构建验证**

Run: `cd zhiyi-mini && npm run build:weapp 2>&1 | tail -5`
Expected: 构建成功

- [ ] **Step 3: Commit**

```bash
git add zhiyi-mini/src/pages/scan/index.vue
git commit -m "feat(mobile): rewrite scan page with three-step flow"
```

---

## Task 10: 前端工资页与报工记录页

**Files:**
- Modify: `zhiyi-mini/src/pages/wage/index.vue`
- Create: `zhiyi-mini/src/pages/report-records/index.vue`

- [ ] **Step 1: 重写工资页**

替换 `zhiyi-mini/src/pages/wage/index.vue`，使用 `getWageSummary` API，展示今日已赚、本月累计、按日期分组的计件明细。只展示计件工资，不展示计时工资。

- [ ] **Step 2: 创建报工记录页**

创建 `zhiyi-mini/src/pages/report-records/index.vue`，使用 `getReportRecords` API，支持近 7 天/本月/自定义日期筛选，倒序展示报工记录列表。

- [ ] **Step 3: 构建验证**

Run: `cd zhiyi-mini && npm run build:weapp 2>&1 | tail -5`
Expected: 构建成功

- [ ] **Step 4: Commit**

```bash
git add zhiyi-mini/src/pages/wage/index.vue zhiyi-mini/src/pages/report-records/
git commit -m "feat(mobile): rewrite wage page and add report records page"
```

---

## Task 11: 前端管理员端页面

**Files:**
- Modify: `zhiyi-mini/src/pages/admin/dashboard/index.vue`
- Modify: `zhiyi-mini/src/pages/admin/anomaly/index.vue`
- Create: `zhiyi-mini/src/pages/admin/anomaly-detail/index.vue`
- Modify: `zhiyi-mini/src/pages/admin/timed-wage/index.vue`
- Create: `zhiyi-mini/src/pages/admin/bundle-query/index.vue`
- Create: `zhiyi-mini/src/pages/admin/order-query/index.vue`

- [ ] **Step 1: 重写生产看板页**

替换 `dashboard/index.vue`，使用 `getDashboardOrders` 和 `getDashboardWorkers` API，展示订单维度（工序完成进度、瓶颈工序）和工人维度（今日产出、今日工资），支持日期筛选和下拉刷新。

- [ ] **Step 2: 重写异常预警页**

替换 `anomaly/index.vue`，使用 `getAnomalies` API，展示异常列表，支持状态筛选，点击进入异常详情页。

- [ ] **Step 3: 创建异常详情页**

创建 `anomaly-detail/index.vue`，展示异常详情，提供忽略（填写原因）、清除（填写原因）、补录（选择工序/工人/件数/时间）三种操作。

- [ ] **Step 4: 重写计时录入页**

替换 `timed-wage/index.vue`，使用计时工资 CRUD API，展示记录列表，支持新增/编辑弹窗（工人、日期、工时、时薪、工作内容）和删除。

- [ ] **Step 5: 创建扎号查询页**

创建 `bundle-query/index.vue`，支持扫码或输入扎号搜索，使用 `searchBundle` API，展示扎号详情与工序完成状态，有权限用户可清除/补录。

- [ ] **Step 6: 创建订单查询页**

创建 `order-query/index.vue`，输入订单号搜索，使用 `searchOrder` API，展示订单基础信息、明细和整体完成率。

- [ ] **Step 7: 构建验证**

Run: `cd zhiyi-mini && npm run build:weapp 2>&1 | tail -5`
Expected: 构建成功

- [ ] **Step 8: Commit**

```bash
git add zhiyi-mini/src/pages/admin/
git commit -m "feat(mobile): implement all admin pages"
```

---

## Task 12: 删除旧文件与清理

**Files:**
- Delete: `zhiyi-mini/src/pages/admin/query/index.vue`（已拆分为 bundle-query 和 order-query）
- Delete: `zhiyi-mini/src/api/auth.ts`
- Delete: `zhiyi-mini/src/api/report.ts`
- Delete: `zhiyi-mini/src/api/admin.ts`

- [ ] **Step 1: 删除旧文件**

```bash
rm zhiyi-mini/src/pages/admin/query/index.vue
rm zhiyi-mini/src/api/auth.ts
rm zhiyi-mini/src/api/report.ts
rm zhiyi-mini/src/api/admin.ts
```

- [ ] **Step 2: 构建验证**

Run: `cd zhiyi-mini && npm run build:weapp 2>&1 | tail -5`
Expected: 构建成功，无缺失模块错误

- [ ] **Step 3: 后端编译验证**

Run: `cd zhiyi-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore(mobile): remove old mini program API and query pages"
```

---

## Task 13: 数据库迁移 - 移动端菜单初始化

**Files:**
- Modify: `sql/init.sql` — 新增移动端菜单记录

- [ ] **Step 1: 新增移动端菜单 SQL**

在 `sql/init.sql` 末尾追加移动端菜单和权限数据（`platform = 'mobile'`）：

```sql
-- 移动端菜单
INSERT IGNORE INTO menu (id, parent_id, name, code, path, component, icon, type, platform, permission, sort, status) VALUES
(100, 0, '移动端', 'mobile', '', '', '', 1, 'mobile', '', 0, 1),
(101, 100, '扫码报工', 'mobile:scan', '/pages/scan/index', '', '📷', 2, 'mobile', 'mobile:scan', 1, 1),
(102, 100, '我的工资', 'mobile:wage', '/pages/wage/index', '', '💰', 2, 'mobile', 'mobile:wage', 2, 1),
(103, 100, '报工记录', 'mobile:report-records', '/pages/report-records/index', '', '📋', 2, 'mobile', 'mobile:report-records', 3, 1),
(104, 100, '生产看板', 'mobile:dashboard', '/pages/admin/dashboard/index', '', '📊', 2, 'mobile', 'mobile:dashboard', 4, 1),
(105, 100, '异常预警', 'mobile:anomaly', '/pages/admin/anomaly/index', '', '⚠️', 2, 'mobile', 'mobile:anomaly', 5, 1),
(106, 100, '计时录入', 'mobile:timed-wage', '/pages/admin/timed-wage/index', '', '⏰', 2, 'mobile', 'mobile:timed-wage', 6, 1),
(107, 100, '扎号查询', 'mobile:bundle-query', '/pages/admin/bundle-query/index', '', '🔍', 2, 'mobile', 'mobile:bundle-query', 7, 1),
(108, 100, '订单查询', 'mobile:order-query', '/pages/admin/order-query/index', '', '📦', 2, 'mobile', 'mobile:order-query', 8, 1),
(109, 100, '报工清除', 'mobile:report-clear', '', '', '', 3, 'mobile', 'mobile:report-clear', 9, 1),
(110, 100, '报工补录', 'mobile:report-supplement', '', '', '', 3, 'mobile', 'mobile:report-supplement', 10, 1);
```

- [ ] **Step 2: Commit**

```bash
git add sql/init.sql
git commit -m "feat(mobile): add mobile menu and permission seed data"
```

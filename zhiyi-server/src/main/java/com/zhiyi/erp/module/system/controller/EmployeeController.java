package com.zhiyi.erp.module.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "员工管理", description = "员工档案CRUD、状态管理")
@RestController
@RequestMapping("/api/system/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Operation(summary = "分页查询员工列表")
    @GetMapping
    public R<IPage<Map<String, Object>>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return R.ok(employeeService.pageWithRoleName(new Page<>(page, size), keyword));
    }

    @Operation(summary = "根据ID查询员工详情")
    @GetMapping("/{id}")
    public R<Employee> getById(@Parameter(description = "员工ID") @PathVariable Long id) {
        return R.ok(employeeService.getById(id));
    }

    @Operation(summary = "创建员工")
    @PostMapping
    public R<Employee> create(@RequestBody Employee employee) {
        return R.ok(employeeService.create(employee));
    }

    @Operation(summary = "更新员工信息")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "员工ID") @PathVariable Long id, @RequestBody Employee employee) {
        employee.setId(id);
        employeeService.updateById(employee);
        return R.ok();
    }

    @Operation(summary = "更新员工状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@Parameter(description = "员工ID") @PathVariable Long id, @Parameter(description = "状态值") @RequestParam Integer status) {
        employeeService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "重置员工登录码")
    @PostMapping("/{id}/reset-login-code")
    public R<String> resetLoginCode(@Parameter(description = "员工ID") @PathVariable Long id) {
        employeeService.resetLoginCode(id);
        Employee emp = employeeService.getById(id);
        return R.ok(emp.getLoginCode());
    }

    @Operation(summary = "重置员工密码")
    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@Parameter(description = "员工ID") @PathVariable Long id, @Parameter(description = "新密码") @RequestParam(defaultValue = "123456") String newPassword) {
        employeeService.resetPassword(id, newPassword);
        return R.ok();
    }
}

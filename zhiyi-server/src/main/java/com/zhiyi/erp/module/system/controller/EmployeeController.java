package com.zhiyi.erp.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.service.EmployeeService;
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

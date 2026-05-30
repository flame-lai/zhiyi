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

package com.zhiyi.erp.module.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.entity.Factory;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import com.zhiyi.erp.module.system.mapper.FactoryMapper;
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

    @Autowired
    private FactoryMapper factoryMapper;

    public Map<String, Object> loginByAccount(String account, String password) {
        Employee employee = employeeMapper.selectByLoginAccount(account);
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

    private Map<String, Object> doLogin(Employee employee) {
        StpUtil.login(employee.getId());

        // 将factoryId存入Session
        StpUtil.getSession().set("factoryId", employee.getFactoryId());
        StpUtil.getSession().set("employeeId", employee.getId());
        StpUtil.getSession().set("roleId", employee.getRoleId());

        Role role = roleMapper.selectById(employee.getRoleId());
        Factory factory = factoryMapper.selectById(employee.getFactoryId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", employee.getId());
        result.put("employeeNo", employee.getEmployeeNo());
        result.put("userName", employee.getName());
        result.put("roleName", role != null ? role.getName() : "");
        result.put("factoryId", employee.getFactoryId());
        result.put("factoryName", factory != null ? factory.getName() : "");

        return result;
    }
}

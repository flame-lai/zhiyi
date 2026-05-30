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
        Employee maxEmployee = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getFactoryId, factoryId)
                        .orderByDesc(Employee::getEmployeeNo)
                        .last("LIMIT 1")
        );

        if (maxEmployee == null || maxEmployee.getEmployeeNo() == null || maxEmployee.getEmployeeNo().isEmpty()) {
            return "A001";
        }

        String maxNo = maxEmployee.getEmployeeNo();
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

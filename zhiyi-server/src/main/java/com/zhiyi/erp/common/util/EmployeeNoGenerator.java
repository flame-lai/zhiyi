package com.zhiyi.erp.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmployeeNoGenerator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String generate(Long factoryId) {
        // 用 JdbcTemplate 绕过 MyBatis-Plus @TableLogic，查到含已删除的最大工号
        String maxNo = jdbcTemplate.queryForObject(
                "SELECT MAX(employee_no) FROM employee WHERE factory_id = ?",
                String.class, factoryId
        );

        if (maxNo == null || maxNo.isEmpty()) {
            return "A001";
        }

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

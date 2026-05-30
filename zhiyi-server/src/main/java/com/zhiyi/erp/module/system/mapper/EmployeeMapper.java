package com.zhiyi.erp.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.system.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}

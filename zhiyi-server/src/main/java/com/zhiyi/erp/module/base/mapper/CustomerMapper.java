package com.zhiyi.erp.module.base.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.base.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT customer_code FROM customer WHERE factory_id = #{factoryId} AND customer_code LIKE 'C%' ORDER BY customer_code DESC LIMIT 1")
    String selectMaxCode(@Param("factoryId") Long factoryId);
}

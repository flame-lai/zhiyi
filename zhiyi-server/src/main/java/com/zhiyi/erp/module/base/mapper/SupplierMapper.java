package com.zhiyi.erp.module.base.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.base.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT supplier_code FROM supplier WHERE factory_id = #{factoryId} AND supplier_code LIKE 'S%' ORDER BY supplier_code DESC LIMIT 1")
    String selectMaxCode(@Param("factoryId") Long factoryId);
}

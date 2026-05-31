package com.zhiyi.erp.module.base.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyi.erp.module.base.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MaterialMapper extends BaseMapper<Material> {

    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT material_code FROM material WHERE factory_id = #{factoryId} AND material_code LIKE 'M%' ORDER BY material_code DESC LIMIT 1")
    String selectMaxCode(@Param("factoryId") Long factoryId);
}

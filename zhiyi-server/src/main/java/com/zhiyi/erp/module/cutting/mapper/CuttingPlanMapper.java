package com.zhiyi.erp.module.cutting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.module.cutting.dto.CuttingPlanDetailDTO;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CuttingPlanMapper extends BaseMapper<CuttingPlan> {

    /**
     * 分页查询裁床计划详情（联表查询订单、款号信息）
     */
    List<CuttingPlanDetailDTO> selectCuttingPlanDetailsPage(
            Page<CuttingPlanDetailDTO> page,
            @Param("factoryId") Long factoryId,
            @Param("orderNo") String orderNo,
            @Param("styleCode") String styleCode,
            @Param("color") String color,
            @Param("size") String size
    );

    /**
     * 查询单个裁床计划详情
     */
    CuttingPlanDetailDTO selectCuttingPlanDetailById(@Param("planId") Long planId);
}

package com.zhiyi.erp.module.cutting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.module.cutting.dto.BundleDetailDTO;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BundleMapper extends BaseMapper<Bundle> {

    /**
     * 分页查询扎号详情（联表查询订单、款号、裁床计划信息）
     */
    List<BundleDetailDTO> selectBundleDetailsPage(
            Page<BundleDetailDTO> page,
            @Param("factoryId") Long factoryId,
            @Param("orderNo") String orderNo,
            @Param("styleCode") String styleCode,
            @Param("color") String color,
            @Param("size") String size,
            @Param("status") Integer status
    );

    /**
     * 查询单个扎号详情
     */
    BundleDetailDTO selectBundleDetailById(@Param("bundleId") Long bundleId);
}

package com.zhiyi.erp.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.module.order.dto.OrderDetailDTO;
import com.zhiyi.erp.module.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 分页查询订单详情（联表查询客户、款号、颜色尺码汇总）
     */
    List<OrderDetailDTO> selectOrderDetailsPage(
            Page<OrderDetailDTO> page,
            @Param("factoryId") Long factoryId,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 查询单个订单详情
     */
    OrderDetailDTO selectOrderDetailById(@Param("orderId") Long orderId);
}

package com.zhiyi.erp.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.common.util.OrderNoGenerator;
import com.zhiyi.erp.module.base.entity.StyleMaterial;
import com.zhiyi.erp.module.base.entity.StyleProcess;
import com.zhiyi.erp.module.base.service.StyleService;
import com.zhiyi.erp.module.order.dto.OrderDetailDTO;
import com.zhiyi.erp.module.order.entity.*;
import com.zhiyi.erp.module.order.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderProcessMapper orderProcessMapper;
    @Autowired
    private OrderMaterialMapper orderMaterialMapper;
    @Autowired
    private OrderColorSizeMapper orderColorSizeMapper;
    @Autowired
    private StyleService styleService;
    @Autowired
    private OrderMapper orderMapper;

    public OrderDetailDTO getOrderWithDetails(Long orderId) {
        OrderDetailDTO detail = orderMapper.selectOrderDetailById(orderId);
        if (detail == null) {
            throw new BizException("订单不存在");
        }
        return detail;
    }

    public Page<OrderDetailDTO> pageWithDetails(Page<OrderDetailDTO> page, String keyword, Integer status, String startDate, String endDate) {
        List<OrderDetailDTO> records = orderMapper.selectOrderDetailsPage(
                page, TenantContext.get(), keyword, status, startDate, endDate);
        page.setRecords(records);
        return page;
    }

    @Transactional
    public Long createOrder(Order order, List<OrderColorSize> colorSizes) {
        order.setOrderNo(OrderNoGenerator.generate());
        order.setFactoryId(TenantContext.get());
        order.setStatus(0);
        save(order);

        // 保存颜色尺码明细
        int totalQty = 0;
        if (colorSizes != null) {
            for (OrderColorSize cs : colorSizes) {
                cs.setOrderId(order.getId());
                orderColorSizeMapper.insert(cs);
                totalQty += cs.getQty() != null ? cs.getQty() : 0;
            }
        }

        // 从款号模板带出工序
        if (order.getStyleId() != null) {
            List<StyleProcess> styleProcesses = styleService.getStyleProcesses(order.getStyleId());
            for (StyleProcess sp : styleProcesses) {
                OrderProcess op = new OrderProcess();
                op.setOrderId(order.getId());
                op.setProcessId(sp.getProcessId());
                op.setProcessPrice(sp.getRefPrice());
                op.setSort(sp.getSort());
                op.setBatchReport(sp.getBatchReport() != null ? sp.getBatchReport() : 1);
                orderProcessMapper.insert(op);
            }

            // 从款号模板带出物料清单，总用量 = 单件用量 × 总数量
            List<StyleMaterial> styleMaterials = styleService.getStyleMaterials(order.getStyleId());
            for (StyleMaterial sm : styleMaterials) {
                OrderMaterial om = new OrderMaterial();
                om.setOrderId(order.getId());
                om.setMaterialName(sm.getMaterialName());
                om.setType(sm.getType());
                om.setUnitQty(sm.getUnitQty());
                om.setUnit(sm.getUnit());
                if (sm.getUnitQty() != null && totalQty > 0) {
                    om.setTotalQty(sm.getUnitQty().multiply(new java.math.BigDecimal(totalQty)));
                }
                orderMaterialMapper.insert(om);
            }
        }

        return order.getId();
    }

    public List<OrderColorSize> getOrderColorSizes(Long orderId) {
        return orderColorSizeMapper.selectList(
                new LambdaQueryWrapper<OrderColorSize>().eq(OrderColorSize::getOrderId, orderId));
    }

    @Transactional
    public void saveOrderColorSizes(Long orderId, List<OrderColorSize> colorSizes) {
        orderColorSizeMapper.delete(
                new LambdaQueryWrapper<OrderColorSize>().eq(OrderColorSize::getOrderId, orderId));
        if (colorSizes != null) {
            for (OrderColorSize cs : colorSizes) {
                cs.setOrderId(orderId);
                orderColorSizeMapper.insert(cs);
            }
        }
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    public List<OrderProcess> getOrderProcesses(Long orderId) {
        return orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>()
                        .eq(OrderProcess::getOrderId, orderId)
                        .orderByAsc(OrderProcess::getSort));
    }

    @Transactional
    public void saveOrderProcesses(Long orderId, List<OrderProcess> processes) {
        orderProcessMapper.delete(
                new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getOrderId, orderId));
        for (int i = 0; i < processes.size(); i++) {
            OrderProcess op = processes.get(i);
            op.setOrderId(orderId);
            op.setSort(i + 1);
            orderProcessMapper.insert(op);
        }
    }

    public List<OrderMaterial> getOrderMaterials(Long orderId) {
        return orderMaterialMapper.selectList(
                new LambdaQueryWrapper<OrderMaterial>().eq(OrderMaterial::getOrderId, orderId));
    }

    @Transactional
    public void saveOrderMaterials(Long orderId, List<OrderMaterial> materials) {
        orderMaterialMapper.delete(
                new LambdaQueryWrapper<OrderMaterial>().eq(OrderMaterial::getOrderId, orderId));
        for (OrderMaterial om : materials) {
            om.setOrderId(orderId);
            orderMaterialMapper.insert(om);
        }
    }

    public void confirmOrder(Long id) {
        Order order = getById(id);
        if (order == null) throw new BizException("订单不存在");
        if (order.getStatus() != 0) throw new BizException("当前状态不可确认");
        order.setStatus(1);
        updateById(order);
    }

    public void settleOrder(Long id) {
        Order order = getById(id);
        if (order == null) throw new BizException("订单不存在");
        order.setStatus(5);
        updateById(order);
    }
}

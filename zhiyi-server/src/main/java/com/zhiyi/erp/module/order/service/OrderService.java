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
import com.zhiyi.erp.module.order.entity.*;
import com.zhiyi.erp.module.order.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderProcessMapper orderProcessMapper;
    @Autowired
    private OrderMaterialMapper orderMaterialMapper;
    @Autowired
    private StyleService styleService;

    public Page<Order> page(Page<Order> page, String keyword, Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Order::getOrderNo, keyword);
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Order::getCreatedAt, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Order::getCreatedAt, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return page(page, wrapper);
    }

    @Transactional
    public Long createOrder(Order order, List<OrderItem> items) {
        order.setOrderNo(OrderNoGenerator.generate());
        order.setFactoryId(TenantContext.get());
        order.setStatus(0);
        save(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);

            // 从款号模板带出工序
            List<StyleProcess> styleProcesses = styleService.getStyleProcesses(item.getStyleId());
            for (StyleProcess sp : styleProcesses) {
                OrderProcess op = new OrderProcess();
                op.setOrderId(order.getId());
                op.setOrderItemId(item.getId());
                op.setProcessId(sp.getProcessId());
                op.setProcessPrice(sp.getRefPrice());
                op.setSort(sp.getSort());
                orderProcessMapper.insert(op);
            }

            // 从款号模板带出物料清单
            List<StyleMaterial> styleMaterials = styleService.getStyleMaterials(item.getStyleId());
            for (StyleMaterial sm : styleMaterials) {
                OrderMaterial om = new OrderMaterial();
                om.setOrderId(order.getId());
                om.setMaterialName(sm.getMaterialName());
                om.setType(sm.getType());
                om.setUnitQty(sm.getUnitQty());
                om.setUnit(sm.getUnit());
                // 总用量 = 单件用量 × 数量
                if (sm.getUnitQty() != null && item.getQty() != null) {
                    om.setTotalQty(sm.getUnitQty().multiply(new java.math.BigDecimal(item.getQty())));
                }
                orderMaterialMapper.insert(om);
            }
        }

        return order.getId();
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

package com.zhiyi.erp.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.common.util.OrderNoGenerator;
import com.zhiyi.erp.module.base.entity.Customer;
import com.zhiyi.erp.module.base.entity.Style;
import com.zhiyi.erp.module.base.entity.StyleMaterial;
import com.zhiyi.erp.module.base.entity.StyleProcess;
import com.zhiyi.erp.module.base.mapper.CustomerMapper;
import com.zhiyi.erp.module.base.mapper.StyleMapper;
import com.zhiyi.erp.module.base.service.StyleService;
import com.zhiyi.erp.module.order.entity.*;
import com.zhiyi.erp.module.order.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private CustomerMapper customerMapper;
    @Autowired
    private StyleMapper styleMapper;

    public Page<Map<String, Object>> pageWithDetails(Page<Order> page, String keyword, Integer status, String startDate, String endDate) {
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

        Page<Order> orderPage = page(page, wrapper);

        // 转换为包含详细信息的Map
        List<Map<String, Object>> records = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("customerId", order.getCustomerId());
            map.put("styleId", order.getStyleId());
            map.put("totalAmount", order.getTotalAmount());
            map.put("deliveryDate", order.getDeliveryDate());
            map.put("status", order.getStatus());
            map.put("remark", order.getRemark());
            map.put("createdAt", order.getCreatedAt());

            // 获取客户名称
            Customer customer = customerMapper.selectById(order.getCustomerId());
            map.put("customerName", customer != null ? customer.getName() : "");

            // 获取款号
            Style style = styleMapper.selectById(order.getStyleId());
            map.put("styleCode", style != null ? style.getStyleCode() : "");
            map.put("styleName", style != null ? style.getStyleName() : "");

            // 获取颜色尺码汇总数量
            List<OrderColorSize> colorSizes = orderColorSizeMapper.selectList(
                    new LambdaQueryWrapper<OrderColorSize>().eq(OrderColorSize::getOrderId, order.getId()));
            int totalQty = 0;
            for (OrderColorSize cs : colorSizes) {
                totalQty += cs.getQty() != null ? cs.getQty() : 0;
            }
            map.put("totalQty", totalQty);

            records.add(map);
        }

        Page<Map<String, Object>> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(records);
        return result;
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

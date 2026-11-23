package com.zhiyi.erp.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.entity.Shipment;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import com.zhiyi.erp.module.order.mapper.ShipmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentService extends ServiceImpl<ShipmentMapper, Shipment> {

    @Autowired
    private OrderMapper orderMapper;

    public List<Shipment> listByOrder(Long orderId) {
        return list(new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getOrderId, orderId)
                .orderByDesc(Shipment::getShipmentDate));
    }

    public Page<Shipment> pageShipments(Page<Shipment> page, Long orderId) {
        LambdaQueryWrapper<Shipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shipment::getFactoryId, TenantContext.get());
        if (orderId != null) {
            wrapper.eq(Shipment::getOrderId, orderId);
        }
        wrapper.orderByDesc(Shipment::getShipmentDate).orderByDesc(Shipment::getId);
        return page(page, wrapper);
    }

    public void createShipment(Shipment shipment) {
        shipment.setFactoryId(TenantContext.get());
        save(shipment);

        // 更新订单状态
        Order order = orderMapper.selectById(shipment.getOrderId());
        if (order != null && order.getStatus() < 2) {
            order.setStatus(2); // 生产中
            orderMapper.updateById(order);
        }
    }
}

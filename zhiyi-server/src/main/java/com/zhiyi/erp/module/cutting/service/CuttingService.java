package com.zhiyi.erp.module.cutting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanMapper;
import com.zhiyi.erp.module.order.entity.OrderItem;
import com.zhiyi.erp.module.order.mapper.OrderItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CuttingService extends ServiceImpl<CuttingPlanMapper, CuttingPlan> {

    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;

    public Page<CuttingPlan> page(Page<CuttingPlan> page, Long orderItemId) {
        LambdaQueryWrapper<CuttingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CuttingPlan::getFactoryId, TenantContext.get());
        if (orderItemId != null) {
            wrapper.eq(CuttingPlan::getOrderItemId, orderItemId);
        }
        wrapper.orderByDesc(CuttingPlan::getCreatedAt);
        return page(page, wrapper);
    }

    public void inputActualQty(Long id, Integer actualQty) {
        CuttingPlan plan = getById(id);
        if (plan == null) throw new BizException("裁床计划不存在");
        plan.setActualQty(actualQty);
        updateById(plan);
    }

    @Transactional
    public List<Bundle> openBundle(Long id) {
        CuttingPlan plan = getById(id);
        if (plan == null) throw new BizException("裁床计划不存在");
        if (plan.getActualQty() == null || plan.getActualQty() <= 0) {
            throw new BizException("请先录入实际件数");
        }
        if (plan.getStatus() >= 2) throw new BizException("已开菲");

        int actualQty = plan.getActualQty();
        int qtyPerBundle = plan.getQtyPerBundle();
        int bundleCount = (int) Math.ceil((double) actualQty / qtyPerBundle);
        plan.setBundleCount(bundleCount);
        plan.setStatus(2);
        updateById(plan);

        // 获取订单ID
        OrderItem orderItem = orderItemMapper.selectById(plan.getOrderItemId());
        Long orderId = orderItem != null ? orderItem.getOrderId() : null;

        List<Bundle> bundles = new ArrayList<>();
        for (int i = 1; i <= bundleCount; i++) {
            int qty = (i < bundleCount) ? qtyPerBundle : (actualQty - (bundleCount - 1) * qtyPerBundle);
            Bundle bundle = new Bundle();
            bundle.setFactoryId(TenantContext.get());
            bundle.setCuttingPlanId(id);
            bundle.setOrderId(orderId);
            bundle.setOrderItemId(plan.getOrderItemId());
            bundle.setBundleNo(plan.getBedNo() + "-" + String.format("%03d", i));
            bundle.setGlobalBundleNo(plan.getBarcode() + "-" + plan.getBedNo() + "-" + String.format("%03d", i));
            bundle.setQty(qty);
            bundle.setStatus(0);
            bundleMapper.insert(bundle);
            bundles.add(bundle);
        }

        return bundles;
    }

    public List<Bundle> getBundles(Long cuttingPlanId) {
        return bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getCuttingPlanId, cuttingPlanId)
                        .orderByAsc(Bundle::getBundleNo));
    }

    public Bundle getBundleById(Long id) {
        return bundleMapper.selectById(id);
    }

    public List<Bundle> getBundlesByOrderId(Long orderId) {
        return bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getOrderId, orderId)
                        .orderByAsc(Bundle::getBundleNo));
    }
}

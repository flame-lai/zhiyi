package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Supplier;
import com.zhiyi.erp.module.base.mapper.SupplierMapper;
import org.springframework.stereotype.Service;

@Service
public class SupplierService extends ServiceImpl<SupplierMapper, Supplier> {
    public Page<Supplier> page(Page<Supplier> page, String keyword) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Supplier::getName, keyword).or().like(Supplier::getContact, keyword));
        }
        wrapper.orderByDesc(Supplier::getCreatedAt);
        return page(page, wrapper);
    }
}

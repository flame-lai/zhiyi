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

    @Override
    public boolean save(Supplier entity) {
        // 自动生成供应商编号
        if (entity.getSupplierCode() == null || entity.getSupplierCode().isEmpty()) {
            entity.setSupplierCode(generateCode());
        }
        // 设置工厂ID
        entity.setFactoryId(TenantContext.get());
        return super.save(entity);
    }

    private String generateCode() {
        // 查询所有记录（包括已删除）的最大编号，避免编号冲突
        // 使用@InterceptorIgnore忽略租户过滤和逻辑删除
        String maxCode = baseMapper.selectMaxCode(TenantContext.get());
        int nextNum = 1;
        if (maxCode != null && maxCode.startsWith("S")) {
            try {
                nextNum = Integer.parseInt(maxCode.substring(1)) + 1;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return String.format("S%03d", nextNum);
    }
}

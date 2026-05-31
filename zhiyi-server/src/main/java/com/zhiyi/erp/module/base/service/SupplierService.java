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
        // 查询当前最大编号（租户拦截器会自动添加factory_id条件）
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Supplier::getSupplierCode, "S");
        wrapper.orderByDesc(Supplier::getSupplierCode);
        wrapper.last("LIMIT 1");
        Supplier last = getOne(wrapper);

        int nextNum = 1;
        if (last != null && last.getSupplierCode() != null) {
            String lastCode = last.getSupplierCode();
            if (lastCode.startsWith("S")) {
                try {
                    nextNum = Integer.parseInt(lastCode.substring(1)) + 1;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return String.format("S%03d", nextNum);
    }
}

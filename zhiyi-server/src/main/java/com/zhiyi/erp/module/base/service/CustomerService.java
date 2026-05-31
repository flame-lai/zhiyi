package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Customer;
import com.zhiyi.erp.module.base.mapper.CustomerMapper;
import org.springframework.stereotype.Service;

@Service
public class CustomerService extends ServiceImpl<CustomerMapper, Customer> {
    public Page<Customer> page(Page<Customer> page, String keyword) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getFactoryId, TenantContext.get());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Customer::getName, keyword).or().like(Customer::getContact, keyword));
        }
        wrapper.orderByDesc(Customer::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public boolean save(Customer entity) {
        // 自动生成客户编号
        if (entity.getCustomerCode() == null || entity.getCustomerCode().isEmpty()) {
            entity.setCustomerCode(generateCode());
        }
        // 设置工厂ID
        entity.setFactoryId(TenantContext.get());
        return super.save(entity);
    }

    private String generateCode() {
        // 查询所有记录（包括已删除）的最大编号，避免编号冲突
        String maxCode = baseMapper.selectMaxCode(TenantContext.get());
        int nextNum = 1;
        if (maxCode != null && maxCode.startsWith("C")) {
            try {
                nextNum = Integer.parseInt(maxCode.substring(1)) + 1;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return String.format("C%03d", nextNum);
    }
}

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
        // 查询当前最大编号（租户拦截器会自动添加factory_id条件）
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Customer::getCustomerCode, "C");
        wrapper.orderByDesc(Customer::getCustomerCode);
        wrapper.last("LIMIT 1");
        Customer last = getOne(wrapper);

        int nextNum = 1;
        if (last != null && last.getCustomerCode() != null) {
            String lastCode = last.getCustomerCode();
            if (lastCode.startsWith("C")) {
                try {
                    nextNum = Integer.parseInt(lastCode.substring(1)) + 1;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return String.format("C%03d", nextNum);
    }
}

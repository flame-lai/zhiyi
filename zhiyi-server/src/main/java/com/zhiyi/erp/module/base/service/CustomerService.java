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
}

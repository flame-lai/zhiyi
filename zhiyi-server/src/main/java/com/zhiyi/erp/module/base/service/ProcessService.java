package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Process;
import com.zhiyi.erp.module.base.mapper.ProcessMapper;
import org.springframework.stereotype.Service;

@Service
public class ProcessService extends ServiceImpl<ProcessMapper, Process> {
    public Page<Process> page(Page<Process> page, String category) {
        LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Process::getFactoryId, TenantContext.get());
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Process::getCategory, category);
        }
        wrapper.orderByAsc(Process::getSort);
        return page(page, wrapper);
    }
}

package com.zhiyi.erp.module.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.base.entity.Process;
import com.zhiyi.erp.module.base.mapper.ProcessMapper;
import com.zhiyi.erp.module.order.entity.OrderProcess;
import com.zhiyi.erp.module.order.mapper.OrderProcessMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProcessService extends ServiceImpl<ProcessMapper, Process> {
    @Autowired
    private OrderProcessMapper orderProcessMapper;

    public Page<Process> page(Page<Process> page, String category) {
        LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Process::getFactoryId, TenantContext.get());
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Process::getCategory, category);
        }
        wrapper.orderByAsc(Process::getSort);
        return page(page, wrapper);
    }

    public List<String> getCategories() {
        LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Process::getFactoryId, TenantContext.get());
        wrapper.select(Process::getCategory);
        wrapper.groupBy(Process::getCategory);
        wrapper.orderByAsc(Process::getCategory);
        return list(wrapper).stream()
                .map(Process::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        // 检查工序是否被订单使用
        Long count = orderProcessMapper.selectCount(
                new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getProcessId, id));
        if (count > 0) {
            throw new BizException("该工序已被订单使用，无法删除");
        }
        return super.removeById(id);
    }
}

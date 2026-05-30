package com.zhiyi.erp.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.entity.TimedWage;
import com.zhiyi.erp.module.admin.mapper.TimedWageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimedWageService extends ServiceImpl<TimedWageMapper, TimedWage> {

    public Page<TimedWage> page(Page<TimedWage> page, Long employeeId, String yearMonth) {
        LambdaQueryWrapper<TimedWage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimedWage::getFactoryId, TenantContext.get());
        if (employeeId != null) {
            wrapper.eq(TimedWage::getEmployeeId, employeeId);
        }
        if (yearMonth != null) {
            wrapper.apply("DATE_FORMAT(work_date, '%Y-%m') = {0}", yearMonth);
        }
        wrapper.orderByDesc(TimedWage::getWorkDate);
        return page(page, wrapper);
    }

    public void createBatch(List<TimedWage> wages) {
        for (TimedWage wage : wages) {
            wage.setFactoryId(TenantContext.get());
            save(wage);
        }
    }
}

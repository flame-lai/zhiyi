package com.zhiyi.erp.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.entity.WageOther;
import com.zhiyi.erp.module.admin.entity.WageSummary;
import com.zhiyi.erp.module.admin.mapper.WageOtherMapper;
import com.zhiyi.erp.module.admin.mapper.WageSummaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WageManageService extends ServiceImpl<WageSummaryMapper, WageSummary> {

    @Autowired
    private WageOtherMapper wageOtherMapper;

    public Page<WageSummary> page(Page<WageSummary> page, String yearMonth, Integer status) {
        LambdaQueryWrapper<WageSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WageSummary::getFactoryId, TenantContext.get());
        if (yearMonth != null) {
            wrapper.eq(WageSummary::getYearMonth, yearMonth);
        }
        if (status != null) {
            wrapper.eq(WageSummary::getStatus, status);
        }
        return page(page, wrapper);
    }

    public void audit(Long id) {
        WageSummary summary = getById(id);
        summary.setStatus(1);
        updateById(summary);
    }

    public void markPaid(Long id) {
        WageSummary summary = getById(id);
        summary.setStatus(2);
        updateById(summary);
    }

    public List<WageOther> getOtherItems(Long employeeId, String yearMonth) {
        return wageOtherMapper.selectList(
                new LambdaQueryWrapper<WageOther>()
                        .eq(WageOther::getEmployeeId, employeeId)
                        .eq(WageOther::getYearMonth, yearMonth));
    }

    public void addOtherItem(WageOther item) {
        item.setFactoryId(TenantContext.get());
        wageOtherMapper.insert(item);
    }
}

package com.zhiyi.erp.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.entity.AnomalyAlert;
import com.zhiyi.erp.module.admin.mapper.AnomalyAlertMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnomalyService extends ServiceImpl<AnomalyAlertMapper, AnomalyAlert> {

    @Autowired
    private WorkReportMapper workReportMapper;

    public Page<AnomalyAlert> page(Page<AnomalyAlert> page, Integer status) {
        LambdaQueryWrapper<AnomalyAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyAlert::getFactoryId, TenantContext.get());
        if (status != null) {
            wrapper.eq(AnomalyAlert::getStatus, status);
        }
        wrapper.orderByDesc(AnomalyAlert::getCreatedAt);
        return page(page, wrapper);
    }

    @Transactional
    public void ignore(Long id, String remark) {
        AnomalyAlert alert = getById(id);
        alert.setStatus(1);
        alert.setHandleType(1);
        alert.setHandleRemark(remark);
        alert.setHandleTime(LocalDateTime.now());
        updateById(alert);
    }

    @Transactional
    public void clearReport(Long id, String reason) {
        AnomalyAlert alert = getById(id);
        WorkReport report = workReportMapper.selectOne(
                new LambdaQueryWrapper<WorkReport>()
                        .eq(WorkReport::getBundleId, alert.getBundleId())
                        .eq(WorkReport::getProcessId, alert.getBackProcessId())
                        .eq(WorkReport::getStatus, 1));
        if (report != null) {
            report.setStatus(0);
            report.setClearReason(reason);
            workReportMapper.updateById(report);
        }
        alert.setStatus(2);
        alert.setHandleType(2);
        alert.setHandleTime(LocalDateTime.now());
        updateById(alert);
    }

    @Transactional
    public void supplement(Long id, Long employeeId, Integer qty) {
        AnomalyAlert alert = getById(id);
        WorkReport report = new WorkReport();
        report.setFactoryId(TenantContext.get());
        report.setBundleId(alert.getBundleId());
        report.setOrderId(alert.getOrderId());
        report.setProcessId(alert.getFrontProcessId());
        report.setEmployeeId(employeeId);
        report.setQty(qty);
        report.setReportType(3);
        report.setStatus(1);
        report.setReportTime(LocalDateTime.now());
        report.setSupplementFlag(1);
        workReportMapper.insert(report);

        alert.setStatus(3);
        alert.setHandleType(3);
        alert.setHandleTime(LocalDateTime.now());
        updateById(alert);
    }
}

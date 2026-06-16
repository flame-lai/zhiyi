package com.zhiyi.erp.module.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanMapper;
import com.zhiyi.erp.module.order.entity.OrderProcess;
import com.zhiyi.erp.module.order.mapper.OrderProcessMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService extends ServiceImpl<WorkReportMapper, WorkReport> {

    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private CuttingPlanMapper cuttingPlanMapper;
    @Autowired
    private OrderProcessMapper orderProcessMapper;

    /**
     * 扫扎号QR码报工
     */
    @Transactional
    public Map<String, Object> reportByBundle(Long employeeId, Long bundleId, List<Long> processIds) {
        Bundle bundle = bundleMapper.selectById(bundleId);
        if (bundle == null) throw new BizException("扎号不存在");

        List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getOrderId, bundle.getOrderId()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;

        for (Long processId : processIds) {
            // 检查是否已报工
            long count = count(new LambdaQueryWrapper<WorkReport>()
                    .eq(WorkReport::getBundleId, bundleId)
                    .eq(WorkReport::getProcessId, processId)
                    .eq(WorkReport::getStatus, 1));
            if (count > 0) continue;

            OrderProcess op = orderProcesses.stream()
                    .filter(p -> p.getProcessId().equals(processId))
                    .findFirst()
                    .orElseThrow(() -> new BizException("工序不存在"));

            WorkReport report = new WorkReport();
            report.setFactoryId(TenantContext.get());
            report.setBundleId(bundleId);
            report.setOrderId(bundle.getOrderId());
            report.setProcessId(processId);
            report.setEmployeeId(employeeId);
            report.setQty(bundle.getQty());
            report.setPrice(op.getProcessPrice());
            report.setAmount(op.getProcessPrice().multiply(BigDecimal.valueOf(bundle.getQty())));
            report.setReportType(1);
            report.setStatus(1);
            report.setReportTime(LocalDateTime.now());
            report.setSupplementFlag(0);
            save(report);

            totalAmount = totalAmount.add(report.getAmount());
            totalQty += bundle.getQty();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("qty", totalQty);
        result.put("amount", totalAmount);
        return result;
    }

    /**
     * 扫裁床条形码报工（整床裁剪）
     */
    @Transactional
    public Map<String, Object> reportByCuttingBarcode(Long employeeId, String barcode) {
        CuttingPlan plan = cuttingPlanMapper.selectOne(
                new LambdaQueryWrapper<CuttingPlan>()
                        .eq(CuttingPlan::getBarcode, barcode)
                        .eq(CuttingPlan::getFactoryId, TenantContext.get()));
        if (plan == null) throw new BizException("裁床码无效");
        if (plan.getStatus() < 2) throw new BizException("请先开菲");

        List<Bundle> bundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>().eq(Bundle::getCuttingPlanId, plan.getId()));
        if (bundles.isEmpty()) throw new BizException("无扎号数据");

        // 获取裁剪工序
        List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getOrderId, bundles.get(0).getOrderId()));
        OrderProcess cuttingProcess = orderProcesses.isEmpty() ? null : orderProcesses.get(0);
        if (cuttingProcess == null) throw new BizException("无裁剪工序");

        long alreadyReported = count(new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getBundleId, bundles.get(0).getId())
                .eq(WorkReport::getProcessId, cuttingProcess.getProcessId())
                .eq(WorkReport::getStatus, 1));
        if (alreadyReported > 0) throw new BizException("该床已报工");

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;

        for (Bundle bundle : bundles) {
            WorkReport report = new WorkReport();
            report.setFactoryId(TenantContext.get());
            report.setBundleId(bundle.getId());
            report.setOrderId(bundle.getOrderId());
            report.setProcessId(cuttingProcess.getProcessId());
            report.setEmployeeId(employeeId);
            report.setQty(bundle.getQty());
            report.setPrice(cuttingProcess.getProcessPrice());
            report.setAmount(cuttingProcess.getProcessPrice().multiply(BigDecimal.valueOf(bundle.getQty())));
            report.setReportType(2);
            report.setStatus(1);
            report.setReportTime(LocalDateTime.now());
            report.setSupplementFlag(0);
            save(report);

            totalAmount = totalAmount.add(report.getAmount());
            totalQty += bundle.getQty();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("qty", totalQty);
        result.put("amount", totalAmount);
        return result;
    }

    public List<Map<String, Object>> getEmployeeWageSummary(Long employeeId, String yearMonth) {
        List<WorkReport> reports = list(new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getEmployeeId, employeeId)
                .eq(WorkReport::getStatus, 1)
                .apply("DATE_FORMAT(report_time, '%Y-%m') = {0}", yearMonth));

        BigDecimal totalPieceWage = reports.stream()
                .map(WorkReport::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<WorkReport>> byDate = reports.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getReportTime().toLocalDate().toString()));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPieceWage", totalPieceWage);
        summary.put("dailyDetails", byDate);
        return List.of(summary);
    }
}

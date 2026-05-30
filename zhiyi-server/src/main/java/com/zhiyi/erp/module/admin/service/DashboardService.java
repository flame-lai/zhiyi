package com.zhiyi.erp.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.order.entity.OrderProcess;
import com.zhiyi.erp.module.order.mapper.OrderProcessMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private WorkReportMapper workReportMapper;
    @Autowired
    private OrderProcessMapper orderProcessMapper;

    public List<Map<String, Object>> getOrderDashboard(Long orderId) {
        List<Bundle> bundles;
        if (orderId != null) {
            bundles = bundleMapper.selectList(
                    new LambdaQueryWrapper<Bundle>().eq(Bundle::getOrderId, orderId));
        } else {
            bundles = bundleMapper.selectList(
                    new LambdaQueryWrapper<Bundle>().eq(Bundle::getFactoryId, TenantContext.get()));
        }

        Map<Long, List<Bundle>> byOrder = bundles.stream()
                .collect(Collectors.groupingBy(Bundle::getOrderId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Bundle>> entry : byOrder.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("orderId", entry.getKey());
            item.put("totalBundles", entry.getValue().size());

            List<Long> bundleIds = entry.getValue().stream().map(Bundle::getId).toList();
            List<WorkReport> reports = workReportMapper.selectList(
                    new LambdaQueryWrapper<WorkReport>()
                            .in(WorkReport::getBundleId, bundleIds)
                            .eq(WorkReport::getStatus, 1));

            List<OrderProcess> processes = orderProcessMapper.selectList(
                    new LambdaQueryWrapper<OrderProcess>().eq(OrderProcess::getOrderId, entry.getKey()));

            List<Map<String, Object>> processProgress = new ArrayList<>();
            for (OrderProcess op : processes) {
                long completed = reports.stream()
                        .filter(r -> r.getProcessId().equals(op.getProcessId()))
                        .map(WorkReport::getBundleId)
                        .distinct()
                        .count();
                Map<String, Object> pp = new HashMap<>();
                pp.put("processId", op.getProcessId());
                pp.put("completed", completed);
                pp.put("total", entry.getValue().size());
                processProgress.add(pp);
            }
            item.put("processProgress", processProgress);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getWorkerDashboard(String date) {
        List<WorkReport> reports = workReportMapper.selectList(
                new LambdaQueryWrapper<WorkReport>()
                        .eq(WorkReport::getFactoryId, TenantContext.get())
                        .eq(WorkReport::getStatus, 1)
                        .apply("DATE(report_time) = {0}", date));

        Map<Long, List<WorkReport>> byEmployee = reports.stream()
                .collect(Collectors.groupingBy(WorkReport::getEmployeeId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<WorkReport>> entry : byEmployee.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", entry.getKey());
            item.put("totalQty", entry.getValue().stream().mapToInt(WorkReport::getQty).sum());
            item.put("totalAmount", entry.getValue().stream()
                    .map(WorkReport::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            result.add(item);
        }
        return result;
    }
}

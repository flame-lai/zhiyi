package com.zhiyi.erp.module.admin.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.entity.AnomalyAlert;
import com.zhiyi.erp.module.admin.mapper.AnomalyAlertMapper;
import com.zhiyi.erp.module.admin.vo.AnomalyAlertVO;
import com.zhiyi.erp.module.base.entity.Process;
import com.zhiyi.erp.module.base.entity.Style;
import com.zhiyi.erp.module.base.mapper.ProcessMapper;
import com.zhiyi.erp.module.base.mapper.StyleMapper;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.entity.OrderProcess;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import com.zhiyi.erp.module.order.mapper.OrderProcessMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnomalyService extends ServiceImpl<AnomalyAlertMapper, AnomalyAlert> {

    @Autowired
    private WorkReportMapper workReportMapper;
    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderProcessMapper orderProcessMapper;
    @Autowired
    private ProcessMapper processMapper;
    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private StyleMapper styleMapper;

    public Page<AnomalyAlertVO> pageVO(Page<AnomalyAlert> page, Integer status) {
        LambdaQueryWrapper<AnomalyAlert> wrapper = new LambdaQueryWrapper<AnomalyAlert>()
                .eq(AnomalyAlert::getFactoryId, TenantContext.get());
        if (status != null) {
            if (status == 9) {
                wrapper.ne(AnomalyAlert::getStatus, 0);
            } else {
                wrapper.eq(AnomalyAlert::getStatus, status);
            }
        }
        wrapper.orderByAsc(AnomalyAlert::getStatus).orderByDesc(AnomalyAlert::getCreatedAt);

        Page<AnomalyAlert> alertPage = page(page, wrapper);
        Page<AnomalyAlertVO> result = new Page<>(alertPage.getCurrent(), alertPage.getSize(), alertPage.getTotal());
        result.setRecords(buildVOList(alertPage.getRecords()));
        return result;
    }

    public Page<AnomalyAlert> page(Page<AnomalyAlert> page, Integer status) {
        LambdaQueryWrapper<AnomalyAlert> wrapper = new LambdaQueryWrapper<AnomalyAlert>()
                .eq(AnomalyAlert::getFactoryId, TenantContext.get());
        if (status != null) {
            wrapper.eq(AnomalyAlert::getStatus, status);
        }
        wrapper.orderByAsc(AnomalyAlert::getStatus).orderByDesc(AnomalyAlert::getCreatedAt);
        return page(page, wrapper);
    }

    public List<AnomalyAlertVO> buildVOList(List<AnomalyAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return List.of();
        }

        Set<Long> bundleIds = new HashSet<>();
        Set<Long> orderIds = new HashSet<>();
        Set<Long> processIds = new HashSet<>();
        Set<Long> employeeIds = new HashSet<>();
        for (AnomalyAlert alert : alerts) {
            addIfNotNull(bundleIds, alert.getBundleId());
            addIfNotNull(orderIds, alert.getOrderId());
            addIfNotNull(processIds, alert.getFrontProcessId());
            addIfNotNull(processIds, alert.getBackProcessId());
            addIfNotNull(employeeIds, alert.getReportEmployeeId());
            addIfNotNull(employeeIds, alert.getHandleBy());
        }

        Map<Long, Bundle> bundleMap = selectByIds(bundleMapper, bundleIds).stream()
                .collect(Collectors.toMap(Bundle::getId, Function.identity(), (a, b) -> a));
        Map<Long, Order> orderMap = selectByIds(orderMapper, orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity(), (a, b) -> a));
        Map<Long, Process> processMap = selectByIds(processMapper, processIds).stream()
                .collect(Collectors.toMap(Process::getId, Function.identity(), (a, b) -> a));
        Map<Long, Employee> employeeMap = selectByIds(employeeMapper, employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity(), (a, b) -> a));
        Map<Long, Style> styleMap = selectByIds(styleMapper, orderMap.values().stream()
                        .map(Order::getStyleId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Style::getId, Function.identity(), (a, b) -> a));
        Map<String, WorkReport> reportMap = selectBackReports(alerts);

        List<AnomalyAlertVO> result = new ArrayList<>();
        for (AnomalyAlert alert : alerts) {
            Bundle bundle = bundleMap.get(alert.getBundleId());
            Order order = orderMap.get(alert.getOrderId());
            Style style = order == null ? null : styleMap.get(order.getStyleId());
            Process frontProcess = processMap.get(alert.getFrontProcessId());
            Process backProcess = processMap.get(alert.getBackProcessId());
            Employee reporter = employeeMap.get(alert.getReportEmployeeId());
            Employee handler = employeeMap.get(alert.getHandleBy());
            WorkReport backReport = reportMap.get(reportKey(alert.getBundleId(), alert.getBackProcessId()));

            AnomalyAlertVO vo = new AnomalyAlertVO();
            vo.setId(alert.getId());
            vo.setBundleId(alert.getBundleId());
            vo.setBundleNo(bundle != null ? bundle.getBundleNo() : null);
            vo.setGlobalBundleNo(bundle != null ? bundle.getGlobalBundleNo() : null);
            vo.setOrderId(alert.getOrderId());
            vo.setOrderNo(order != null ? order.getOrderNo() : null);
            vo.setStyleCode(style != null ? style.getStyleCode() : null);
            vo.setStyleName(style != null ? style.getStyleName() : null);
            vo.setColor(bundle != null ? bundle.getColor() : null);
            vo.setSize(bundle != null ? bundle.getSize() : null);
            vo.setFrontProcessId(alert.getFrontProcessId());
            vo.setFrontProcessName(frontProcess != null ? frontProcess.getName() : null);
            vo.setBackProcessId(alert.getBackProcessId());
            vo.setBackProcessName(backProcess != null ? backProcess.getName() : null);
            vo.setReportEmployeeId(alert.getReportEmployeeId());
            vo.setReportEmployeeNo(reporter != null ? reporter.getEmployeeNo() : null);
            vo.setReportEmployeeName(reporter != null ? reporter.getName() : null);
            vo.setReportQty(backReport != null ? backReport.getQty() : null);
            vo.setReportTime(backReport != null ? backReport.getReportTime() : alert.getCreatedAt());
            vo.setDescription(buildDescription(frontProcess, backProcess));
            vo.setStatus(alert.getStatus());
            vo.setHandleType(alert.getHandleType());
            vo.setHandleBy(alert.getHandleBy());
            vo.setHandleByName(handler != null ? handler.getName() : null);
            vo.setHandleTime(alert.getHandleTime());
            vo.setHandleRemark(alert.getHandleRemark());
            vo.setCreatedAt(alert.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Transactional
    public void ignore(Long id, String remark) {
        if (remark == null || remark.isBlank()) {
            throw new BizException("请输入忽略原因");
        }
        AnomalyAlert alert = getPendingAlert(id);
        alert.setStatus(1);
        alert.setHandleType(1);
        alert.setHandleBy(StpUtil.getLoginIdAsLong());
        alert.setHandleRemark(remark.trim());
        alert.setHandleTime(LocalDateTime.now());
        updateById(alert);
    }

    @Transactional
    public void clearReport(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BizException("请输入清除原因");
        }
        AnomalyAlert alert = getPendingAlert(id);
        WorkReport report = findActiveBackReport(alert);
        if (report == null) {
            throw new BizException("后置工序报工记录不存在或已清除");
        }

        report.setStatus(0);
        report.setClearReason(reason.trim());
        workReportMapper.updateById(report);

        alert.setStatus(2);
        alert.setHandleType(2);
        alert.setHandleBy(StpUtil.getLoginIdAsLong());
        alert.setHandleRemark(reason.trim());
        alert.setHandleTime(LocalDateTime.now());
        updateById(alert);
    }

    @Transactional
    public void supplement(Long id, Long employeeId, Integer qty) {
        if (employeeId == null) {
            throw new BizException("请选择补录工人");
        }
        if (qty == null || qty <= 0) {
            throw new BizException("补录数量必须大于0");
        }
        Long factoryId = TenantContext.get();
        AnomalyAlert alert = getPendingAlert(id);

        Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getFactoryId, factoryId)
                .eq(Employee::getId, employeeId)
                .eq(Employee::getStatus, 1));
        if (employee == null) {
            throw new BizException("补录工人不存在或已停用");
        }

        Bundle bundle = bundleMapper.selectOne(new LambdaQueryWrapper<Bundle>()
                .eq(Bundle::getFactoryId, factoryId)
                .eq(Bundle::getId, alert.getBundleId()));
        if (bundle == null) {
            throw new BizException("扎号不存在");
        }
        if (bundle.getQty() != null && qty > bundle.getQty()) {
            throw new BizException("补录数量不能超过扎号件数（" + bundle.getQty() + "）");
        }

        OrderProcess orderProcess = orderProcessMapper.selectOne(new LambdaQueryWrapper<OrderProcess>()
                .eq(OrderProcess::getOrderId, alert.getOrderId())
                .eq(OrderProcess::getProcessId, alert.getFrontProcessId()));
        if (orderProcess == null) {
            throw new BizException("前置工序不属于该订单");
        }

        List<Bundle> samePhysicalBundles = selectSamePhysicalBundles(bundle);
        List<Long> sameBundleIds = samePhysicalBundles.stream().map(Bundle::getId).toList();
        Long existingCount = workReportMapper.selectCount(new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, factoryId)
                .in(WorkReport::getBundleId, sameBundleIds)
                .eq(WorkReport::getProcessId, alert.getFrontProcessId())
                .eq(WorkReport::getStatus, 1));
        if (existingCount != null && existingCount > 0) {
            throw new BizException("该扎号前置工序已报工，无需重复补录");
        }

        BigDecimal price = orderProcess.getProcessPrice() != null ? orderProcess.getProcessPrice() : BigDecimal.ZERO;
        WorkReport report = new WorkReport();
        report.setFactoryId(factoryId);
        report.setBundleId(alert.getBundleId());
        report.setOrderId(alert.getOrderId());
        report.setProcessId(alert.getFrontProcessId());
        report.setEmployeeId(employeeId);
        report.setQty(qty);
        report.setPrice(price);
        report.setAmount(price.multiply(BigDecimal.valueOf(qty)));
        report.setReportType(3);
        report.setStatus(1);
        report.setReportTime(LocalDateTime.now());
        report.setSupplementFlag(1);
        report.setCreatedBy(StpUtil.getLoginIdAsLong());
        workReportMapper.insert(report);

        List<AnomalyAlert> pendingAlerts = list(new LambdaQueryWrapper<AnomalyAlert>()
                .eq(AnomalyAlert::getFactoryId, factoryId)
                .eq(AnomalyAlert::getOrderId, alert.getOrderId())
                .in(AnomalyAlert::getBundleId, sameBundleIds)
                .eq(AnomalyAlert::getFrontProcessId, alert.getFrontProcessId())
                .eq(AnomalyAlert::getStatus, 0));
        for (AnomalyAlert pendingAlert : pendingAlerts) {
            pendingAlert.setStatus(3);
            pendingAlert.setHandleType(3);
            pendingAlert.setHandleBy(StpUtil.getLoginIdAsLong());
            pendingAlert.setHandleTime(LocalDateTime.now());
            pendingAlert.setHandleRemark("补录前置工序");
            updateById(pendingAlert);
        }
    }

    private AnomalyAlert getPendingAlert(Long id) {
        AnomalyAlert alert = getOne(new LambdaQueryWrapper<AnomalyAlert>()
                .eq(AnomalyAlert::getFactoryId, TenantContext.get())
                .eq(AnomalyAlert::getId, id));
        if (alert == null) {
            throw new BizException("预警记录不存在");
        }
        if (alert.getStatus() == null || alert.getStatus() != 0) {
            throw new BizException("该预警已处理，不可重复操作");
        }
        return alert;
    }

    private WorkReport findActiveBackReport(AnomalyAlert alert) {
        return workReportMapper.selectOne(new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, TenantContext.get())
                .eq(WorkReport::getBundleId, alert.getBundleId())
                .eq(WorkReport::getOrderId, alert.getOrderId())
                .eq(WorkReport::getProcessId, alert.getBackProcessId())
                .eq(WorkReport::getStatus, 1)
                .orderByDesc(WorkReport::getReportTime)
                .last("LIMIT 1"));
    }

    private List<Bundle> selectSamePhysicalBundles(Bundle bundle) {
        List<Bundle> bundles = bundleMapper.selectList(new LambdaQueryWrapper<Bundle>()
                .eq(Bundle::getFactoryId, TenantContext.get())
                .eq(Bundle::getOrderId, bundle.getOrderId())
                .eq(Bundle::getBundleNo, bundle.getBundleNo()));
        return bundles.isEmpty() ? List.of(bundle) : bundles;
    }

    private Map<String, WorkReport> selectBackReports(List<AnomalyAlert> alerts) {
        Set<Long> bundleIds = alerts.stream().map(AnomalyAlert::getBundleId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> processIds = alerts.stream().map(AnomalyAlert::getBackProcessId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (bundleIds.isEmpty() || processIds.isEmpty()) {
            return Map.of();
        }

        List<WorkReport> reports = workReportMapper.selectList(new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, TenantContext.get())
                .in(WorkReport::getBundleId, bundleIds)
                .in(WorkReport::getProcessId, processIds)
                .orderByDesc(WorkReport::getReportTime));
        Map<String, WorkReport> result = new LinkedHashMap<>();
        for (WorkReport report : reports) {
            result.putIfAbsent(reportKey(report.getBundleId(), report.getProcessId()), report);
        }
        return result;
    }

    private String buildDescription(Process frontProcess, Process backProcess) {
        String frontName = frontProcess != null ? frontProcess.getName() : "前置工序";
        String backName = backProcess != null ? backProcess.getName() : "后置工序";
        return "「" + frontName + "」未完成，但「" + backName + "」已报工";
    }

    private String reportKey(Long bundleId, Long processId) {
        return bundleId + ":" + processId;
    }

    private void addIfNotNull(Set<Long> set, Long value) {
        if (value != null) {
            set.add(value);
        }
    }

    private <T> List<T> selectByIds(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids);
    }
}

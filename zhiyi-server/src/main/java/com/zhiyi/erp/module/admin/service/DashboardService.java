package com.zhiyi.erp.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.vo.DashboardOrderPageVO;
import com.zhiyi.erp.module.admin.vo.DashboardOrderProgressVO;
import com.zhiyi.erp.module.admin.vo.DashboardOverviewVO;
import com.zhiyi.erp.module.admin.vo.DashboardWorkerCapacityVO;
import com.zhiyi.erp.module.admin.vo.DashboardWorkerPageVO;
import com.zhiyi.erp.module.base.entity.Customer;
import com.zhiyi.erp.module.base.entity.Process;
import com.zhiyi.erp.module.base.entity.Style;
import com.zhiyi.erp.module.base.mapper.CustomerMapper;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private WorkReportMapper workReportMapper;
    @Autowired
    private OrderProcessMapper orderProcessMapper;
    @Autowired
    private StyleMapper styleMapper;
    @Autowired
    private CustomerMapper customerMapper;
    @Autowired
    private ProcessMapper processMapper;
    @Autowired
    private EmployeeMapper employeeMapper;

    public DashboardOverviewVO getOverview(String date) {
        LocalDate dashboardDate = parseDate(date, LocalDate.now());
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getFactoryId, TenantContext.get())
                .in(Order::getStatus, List.of(1, 2, 3, 4))
                .orderByDesc(Order::getCreatedAt));

        List<DashboardOrderProgressVO> orderProgress = buildOrderProgress(orders);
        List<WorkReport> todayReports = selectReports(dashboardDate.atStartOfDay(), dashboardDate.plusDays(1).atStartOfDay(), null);

        DashboardOverviewVO overview = new DashboardOverviewVO();
        overview.setOrderCount((long) orders.size());
        overview.setBundleCount(orderProgress.stream().mapToInt(v -> nullToZero(v.getTotalBundles())).sum());
        overview.setOrderQty(orderProgress.stream().mapToInt(v -> nullToZero(v.getTotalQty())).sum());
        overview.setAverageCompletionRate(orderProgress.isEmpty()
                ? BigDecimal.ZERO
                : orderProgress.stream()
                .map(DashboardOrderProgressVO::getCompletionRate)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(orderProgress.size()), 4, RoundingMode.HALF_UP));
        overview.setTodayReportQty(todayReports.stream().mapToInt(r -> nullToZero(r.getQty())).sum());
        overview.setTodayReportAmount(todayReports.stream()
                .map(WorkReport::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return overview;
    }

    public DashboardOrderPageVO getOrders(String keyword, Integer status, Integer page, Integer size) {
        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), buildOrderWrapper(keyword, status));
        List<DashboardOrderProgressVO> records = buildOrderProgress(orderPage.getRecords());

        DashboardOrderPageVO result = new DashboardOrderPageVO();
        result.setRecords(records);
        result.setTotal(orderPage.getTotal());
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    public List<DashboardOrderProgressVO> getOrderById(Long orderId) {
        if (orderId == null) {
            return buildOrderProgress(orderMapper.selectList(buildOrderWrapper(null, null)));
        }
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getFactoryId, TenantContext.get())
                .eq(Order::getId, orderId));
        return order == null ? List.of() : buildOrderProgress(List.of(order));
    }

    public DashboardWorkerPageVO getWorkers(String startDate, String endDate, String keyword, Integer page, Integer size) {
        LocalDate start = parseDate(startDate, LocalDate.now());
        LocalDate end = parseDate(endDate, start);
        List<WorkReport> reports = selectReports(start.atStartOfDay(), end.plusDays(1).atStartOfDay(), null);

        Map<Long, List<WorkReport>> byEmployee = reports.stream()
                .filter(r -> r.getEmployeeId() != null)
                .collect(Collectors.groupingBy(WorkReport::getEmployeeId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Employee> employeeMap = selectByIds(employeeMapper, byEmployee.keySet()).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity(), (a, b) -> a));

        List<DashboardWorkerCapacityVO> all = new ArrayList<>();
        for (Map.Entry<Long, List<WorkReport>> entry : byEmployee.entrySet()) {
            Employee employee = employeeMap.get(entry.getKey());
            DashboardWorkerCapacityVO vo = new DashboardWorkerCapacityVO();
            vo.setEmployeeId(entry.getKey());
            vo.setEmployeeNo(employee != null ? employee.getEmployeeNo() : null);
            vo.setEmployeeName(employee != null ? employee.getName() : null);
            vo.setReportCount(entry.getValue().size());
            vo.setTotalQty(entry.getValue().stream().mapToInt(r -> nullToZero(r.getQty())).sum());
            vo.setTotalAmount(entry.getValue().stream()
                    .map(WorkReport::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            vo.setAvgPrice(vo.getTotalQty() > 0
                    ? vo.getTotalAmount().divide(BigDecimal.valueOf(vo.getTotalQty()), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            all.add(vo);
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            all = all.stream()
                    .filter(item -> contains(item.getEmployeeNo(), kw) || contains(item.getEmployeeName(), kw))
                    .collect(Collectors.toList());
        }
        all.sort(Comparator.comparing(DashboardWorkerCapacityVO::getTotalQty, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(DashboardWorkerCapacityVO::getTotalAmount, Comparator.nullsLast(BigDecimal::compareTo))
                .reversed());

        long total = all.size();
        int from = Math.min((page - 1) * size, all.size());
        int to = Math.min(from + size, all.size());

        DashboardWorkerPageVO result = new DashboardWorkerPageVO();
        result.setRecords(all.subList(from, to));
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalQty(all.stream().mapToInt(v -> nullToZero(v.getTotalQty())).sum());
        result.setTotalAmount(all.stream()
                .map(DashboardWorkerCapacityVO::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return result;
    }

    private LambdaQueryWrapper<Order> buildOrderWrapper(String keyword, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getFactoryId, TenantContext.get());
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            List<Long> styleIds = styleMapper.selectList(new LambdaQueryWrapper<Style>()
                            .eq(Style::getFactoryId, TenantContext.get())
                            .and(w -> w.like(Style::getStyleCode, kw).or().like(Style::getStyleName, kw)))
                    .stream()
                    .map(Style::getId)
                    .toList();
            wrapper.and(w -> {
                w.like(Order::getOrderNo, kw);
                if (!styleIds.isEmpty()) {
                    w.or().in(Order::getStyleId, styleIds);
                }
            });
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return wrapper;
    }

    private List<DashboardOrderProgressVO> buildOrderProgress(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, Style> styleMap = selectByIds(styleMapper, orders.stream().map(Order::getStyleId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Style::getId, Function.identity(), (a, b) -> a));
        Map<Long, Customer> customerMap = selectByIds(customerMapper, orders.stream().map(Order::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Customer::getId, Function.identity(), (a, b) -> a));
        List<Bundle> bundles = bundleMapper.selectList(new LambdaQueryWrapper<Bundle>()
                .eq(Bundle::getFactoryId, TenantContext.get())
                .in(Bundle::getOrderId, orderIds));
        List<OrderProcess> orderProcesses = orderProcessMapper.selectList(new LambdaQueryWrapper<OrderProcess>()
                .in(OrderProcess::getOrderId, orderIds)
                .orderByAsc(OrderProcess::getSort));
        Map<Long, Process> processMap = selectByIds(processMapper, orderProcesses.stream().map(OrderProcess::getProcessId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Process::getId, Function.identity(), (a, b) -> a));
        List<WorkReport> reports = workReportMapper.selectList(new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, TenantContext.get())
                .eq(WorkReport::getStatus, 1)
                .in(WorkReport::getOrderId, orderIds));

        Map<Long, List<Bundle>> bundlesByOrder = bundles.stream().collect(Collectors.groupingBy(Bundle::getOrderId));
        Map<Long, List<OrderProcess>> processesByOrder = orderProcesses.stream().collect(Collectors.groupingBy(OrderProcess::getOrderId));
        Map<Long, Map<Long, Integer>> reportQtyByOrderProcess = new HashMap<>();
        for (WorkReport report : reports) {
            if (report.getOrderId() == null || report.getProcessId() == null) {
                continue;
            }
            reportQtyByOrderProcess
                    .computeIfAbsent(report.getOrderId(), k -> new HashMap<>())
                    .merge(report.getProcessId(), nullToZero(report.getQty()), Integer::sum);
        }

        List<DashboardOrderProgressVO> result = new ArrayList<>();
        for (Order order : orders) {
            List<Bundle> orderBundles = bundlesByOrder.getOrDefault(order.getId(), List.of());
            List<OrderProcess> processes = processesByOrder.getOrDefault(order.getId(), List.of());
            Integer totalQty = orderBundles.stream().mapToInt(b -> nullToZero(b.getQty())).sum();
            Map<Long, Integer> processQtyMap = reportQtyByOrderProcess.getOrDefault(order.getId(), Map.of());

            List<DashboardOrderProgressVO.ProcessProgress> processProgress = new ArrayList<>();
            BigDecimal lowestRate = null;
            String bottleneckProcess = null;
            for (OrderProcess orderProcess : processes) {
                int completedQty = processQtyMap.getOrDefault(orderProcess.getProcessId(), 0);
                BigDecimal rate = ratio(completedQty, totalQty);
                Process process = processMap.get(orderProcess.getProcessId());

                DashboardOrderProgressVO.ProcessProgress progress = new DashboardOrderProgressVO.ProcessProgress();
                progress.setProcessId(orderProcess.getProcessId());
                progress.setProcessName(process != null ? process.getName() : "未知工序");
                progress.setCompletedQty(completedQty);
                progress.setPlannedQty(totalQty);
                progress.setCompletionRate(rate);
                processProgress.add(progress);

                if (lowestRate == null || rate.compareTo(lowestRate) < 0) {
                    lowestRate = rate;
                    bottleneckProcess = progress.getProcessName();
                }
            }

            BigDecimal completionRate = processProgress.isEmpty()
                    ? BigDecimal.ZERO
                    : processProgress.stream()
                    .map(DashboardOrderProgressVO.ProcessProgress::getCompletionRate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(processProgress.size()), 4, RoundingMode.HALF_UP);

            Style style = styleMap.get(order.getStyleId());
            Customer customer = customerMap.get(order.getCustomerId());
            DashboardOrderProgressVO vo = new DashboardOrderProgressVO();
            vo.setOrderId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setCustomerName(customer != null ? customer.getName() : null);
            vo.setStyleCode(style != null ? style.getStyleCode() : null);
            vo.setStyleName(style != null ? style.getStyleName() : null);
            vo.setColorSummary(summarizeBundleField(orderBundles, Bundle::getColor));
            vo.setSizeSummary(summarizeBundleField(orderBundles, Bundle::getSize));
            vo.setDeliveryDate(order.getDeliveryDate());
            vo.setStatus(order.getStatus());
            vo.setTotalBundles(orderBundles.size());
            vo.setTotalQty(totalQty);
            vo.setCompletedQty(processProgress.isEmpty()
                    ? 0
                    : processProgress.stream().mapToInt(DashboardOrderProgressVO.ProcessProgress::getCompletedQty).min().orElse(0));
            vo.setCompletionRate(completionRate);
            vo.setBottleneckProcess(bottleneckProcess);
            vo.setProcessProgress(processProgress);
            result.add(vo);
        }
        return result;
    }

    private List<WorkReport> selectReports(LocalDateTime start, LocalDateTime end, Long employeeId) {
        LambdaQueryWrapper<WorkReport> wrapper = new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, TenantContext.get())
                .eq(WorkReport::getStatus, 1)
                .ge(WorkReport::getReportTime, start)
                .lt(WorkReport::getReportTime, end);
        if (employeeId != null) {
            wrapper.eq(WorkReport::getEmployeeId, employeeId);
        }
        return workReportMapper.selectList(wrapper);
    }

    private <T> List<T> selectByIds(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids);
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        return value == null || value.isBlank() ? defaultValue : LocalDate.parse(value);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String summarizeBundleField(List<Bundle> bundles, Function<Bundle, String> getter) {
        String value = bundles.stream()
                .map(getter)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .collect(Collectors.joining(" / "));
        return value.isBlank() ? null : value;
    }

    private Integer nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal ratio(Integer numerator, Integer denominator) {
        if (denominator == null || denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
    }
}

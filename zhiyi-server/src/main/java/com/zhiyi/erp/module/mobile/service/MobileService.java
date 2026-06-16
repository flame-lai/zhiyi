package com.zhiyi.erp.module.mobile.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.admin.entity.AnomalyAlert;
import com.zhiyi.erp.module.admin.entity.TimedWage;
import com.zhiyi.erp.module.admin.mapper.AnomalyAlertMapper;
import com.zhiyi.erp.module.admin.mapper.TimedWageMapper;
import com.zhiyi.erp.module.admin.service.AnomalyService;
import com.zhiyi.erp.module.base.entity.Customer;
import com.zhiyi.erp.module.base.entity.Process;
import com.zhiyi.erp.module.base.entity.Style;
import com.zhiyi.erp.module.base.mapper.CustomerMapper;
import com.zhiyi.erp.module.base.mapper.ProcessMapper;
import com.zhiyi.erp.module.base.mapper.StyleMapper;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanMapper;
import com.zhiyi.erp.module.mobile.dto.TimedWageRequest;
import com.zhiyi.erp.module.mobile.dto.ReportSubmitRequest;
import com.zhiyi.erp.module.mobile.vo.*;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.entity.OrderItem;
import com.zhiyi.erp.module.order.entity.OrderProcess;
import com.zhiyi.erp.module.order.mapper.OrderItemMapper;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import com.zhiyi.erp.module.order.mapper.OrderProcessMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.entity.Menu;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import com.zhiyi.erp.module.system.mapper.RoleMapper;
import com.zhiyi.erp.module.system.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MobileService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private MenuService menuService;

    @Autowired
    private BundleMapper bundleMapper;

    @Autowired
    private CuttingPlanMapper cuttingPlanMapper;

    @Autowired
    private OrderProcessMapper orderProcessMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StyleMapper styleMapper;

    @Autowired
    private ProcessMapper processMapper;

    @Autowired
    private WorkReportMapper workReportMapper;

    @Autowired
    private AnomalyAlertMapper anomalyAlertMapper;

    @Autowired
    private AnomalyService anomalyService;

    @Autowired
    private TimedWageMapper timedWageMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    // ========== Auth methods (from Task 1) ==========

    public MobileLoginVO loginByCode(String loginCode) {
        Employee employee = employeeMapper.selectByLoginCode(loginCode);
        if (employee == null) {
            throw new BizException("登录码无效");
        }
        if (employee.getStatus() != 1) {
            throw new BizException("账号已停用，请联系管理员");
        }
        return buildLoginVO(employee);
    }

    public MobileLoginVO loginByAccount(String account, String password) {
        Employee employee = employeeMapper.selectByLoginAccount(account);
        if (employee == null) {
            throw new BizException("账号不存在");
        }
        if (employee.getStatus() != 1) {
            throw new BizException("账号已停用，请联系管理员");
        }
        if (!BCrypt.checkpw(password, employee.getLoginPassword())) {
            throw new BizException("密码错误");
        }
        return buildLoginVO(employee);
    }

    public MobileProfileVO getProfile(Long employeeId) {
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BizException("员工不存在");
        }

        Role role = employee.getRoleId() != null ? roleMapper.selectById(employee.getRoleId()) : null;

        MobileProfileVO vo = new MobileProfileVO();

        MobileProfileVO.EmployeeInfo empInfo = new MobileProfileVO.EmployeeInfo();
        empInfo.setId(employee.getId());
        empInfo.setCode(employee.getEmployeeNo());
        empInfo.setName(employee.getName());
        vo.setEmployee(empInfo);

        MobileProfileVO.RoleInfo roleInfo = new MobileProfileVO.RoleInfo();
        if (role != null) {
            roleInfo.setId(role.getId());
            roleInfo.setName(role.getName());
        }
        vo.setRole(roleInfo);

        vo.setMenus(getMobileMenus(employee.getRoleId(), role));
        vo.setPermissions(getMobilePermissions(employee.getRoleId(), role));
        vo.setDefaultHomeType(determineDefaultHomeType(employee.getRoleId(), role));

        return vo;
    }

    private MobileLoginVO buildLoginVO(Employee employee) {
        StpUtil.login(employee.getId());

        StpUtil.getSession().set("factoryId", employee.getFactoryId());
        StpUtil.getSession().set("employeeId", employee.getId());
        StpUtil.getSession().set("roleId", employee.getRoleId());

        Role role = employee.getRoleId() != null ? roleMapper.selectById(employee.getRoleId()) : null;

        MobileLoginVO vo = new MobileLoginVO();
        vo.setToken(StpUtil.getTokenValue());

        MobileLoginVO.EmployeeInfo empInfo = new MobileLoginVO.EmployeeInfo();
        empInfo.setId(employee.getId());
        empInfo.setCode(employee.getEmployeeNo());
        empInfo.setName(employee.getName());
        vo.setEmployee(empInfo);

        MobileLoginVO.RoleInfo roleInfo = new MobileLoginVO.RoleInfo();
        if (role != null) {
            roleInfo.setId(role.getId());
            roleInfo.setName(role.getName());
        }
        vo.setRole(roleInfo);

        vo.setMenus(getMobileMenus(employee.getRoleId(), role));
        vo.setPermissions(getMobilePermissions(employee.getRoleId(), role));
        vo.setDefaultHomeType(determineDefaultHomeType(employee.getRoleId(), role));

        return vo;
    }

    public List<MobileMenuVO> getMobileMenus(Long roleId, Role role) {
        List<Menu> menus;
        if (role != null && "super_admin".equals(role.getCode())) {
            menus = menuService.listAll();
        } else {
            menus = menuService.getMenusByRoleId(roleId);
        }

        return menus.stream()
                .filter(m -> "mobile".equals(m.getPlatform()) && m.getStatus() != null && m.getStatus() == 1)
                .sorted(Comparator.comparingInt(m -> m.getSort() != null ? m.getSort() : 0))
                .map(m -> {
                    MobileMenuVO menuVO = new MobileMenuVO();
                    menuVO.setCode(m.getCode());
                    menuVO.setTitle(m.getName());
                    menuVO.setPath(m.getPath());
                    menuVO.setGroup(m.getCode() != null && m.getCode().startsWith("mobile:admin") ? "admin" : "employee");
                    menuVO.setIcon(m.getIcon());
                    menuVO.setSort(m.getSort());
                    return menuVO;
                })
                .collect(Collectors.toList());
    }

    public List<String> getMobilePermissions(Long roleId, Role role) {
        List<Menu> menus;
        if (role != null && "super_admin".equals(role.getCode())) {
            menus = menuService.listAll();
        } else {
            menus = menuService.getMenusByRoleId(roleId);
        }

        return menus.stream()
                .filter(m -> "mobile".equals(m.getPlatform()) && m.getStatus() != null && m.getStatus() == 1)
                .map(Menu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String determineDefaultHomeType(Long roleId, Role role) {
        if (role != null && "super_admin".equals(role.getCode())) {
            return "admin";
        }
        List<MobileMenuVO> mobileMenus = getMobileMenus(roleId, role);
        boolean hasAdminMenu = mobileMenus.stream().anyMatch(m -> "admin".equals(m.getGroup()));
        return hasAdminMenu ? "admin" : "employee";
    }

    // ========== Scan & Report methods (Task 2) ==========

    public Object resolveScan(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BizException("扫码内容不能为空");
        }
        String trimmed = code.trim().toUpperCase();
        if (trimmed.startsWith("BUN")) {
            return resolveBundleScan(code.trim());
        } else if (trimmed.startsWith("CUT")) {
            return resolveCuttingScan(code.trim());
        } else {
            throw new BizException("无法识别的二维码，请扫描扎号或裁床条码");
        }
    }

    private BundleScanVO resolveBundleScan(String globalBundleNo) {
        Bundle bundle = bundleMapper.selectOne(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getGlobalBundleNo, globalBundleNo)
        );
        if (bundle == null) {
            throw new BizException("扎号不存在：" + globalBundleNo);
        }

        Order order = bundle.getOrderId() != null ? orderMapper.selectById(bundle.getOrderId()) : null;
        Style style = (order != null && order.getStyleId() != null) ? styleMapper.selectById(order.getStyleId()) : null;

        BundleScanVO vo = new BundleScanVO();
        vo.setCode(globalBundleNo);

        BundleScanVO.BundleInfo bundleInfo = new BundleScanVO.BundleInfo();
        bundleInfo.setId(bundle.getId());
        bundleInfo.setGlobalBundleNo(bundle.getGlobalBundleNo());
        bundleInfo.setBundleNo(bundle.getBundleNo());
        bundleInfo.setQty(bundle.getQty());
        bundleInfo.setOrderNo(order != null ? order.getOrderNo() : null);
        bundleInfo.setStyleNo(style != null ? style.getStyleCode() : null);
        bundleInfo.setColor(bundle.getColor());
        bundleInfo.setSize(bundle.getSize());
        vo.setBundle(bundleInfo);

        vo.setProcesses(buildProcessStatusList(bundle.getOrderId(), bundle.getId()));

        return vo;
    }

    private CuttingScanVO resolveCuttingScan(String barcode) {
        CuttingPlan plan = cuttingPlanMapper.selectOne(
                new LambdaQueryWrapper<CuttingPlan>()
                        .eq(CuttingPlan::getBarcode, barcode)
        );
        if (plan == null) {
            throw new BizException("裁床计划不存在：" + barcode);
        }
        if (plan.getStatus() < 2) {
            throw new BizException("该裁床计划尚未开菲，无法报工");
        }

        Order order = plan.getOrderId() != null ? orderMapper.selectById(plan.getOrderId()) : null;
        Style style = (order != null && order.getStyleId() != null) ? styleMapper.selectById(order.getStyleId()) : null;

        List<Bundle> bundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getCuttingPlanId, plan.getId())
                        .orderByAsc(Bundle::getBundleNo)
        );

        // Get a sample bundle for process status lookup
        Bundle sampleBundle = bundles.isEmpty() ? null : bundles.get(0);

        CuttingScanVO vo = new CuttingScanVO();
        vo.setCode(barcode);

        CuttingScanVO.CuttingInfo cuttingInfo = new CuttingScanVO.CuttingInfo();
        cuttingInfo.setId(plan.getId());
        cuttingInfo.setBarcode(plan.getBarcode());
        cuttingInfo.setBedNo(plan.getBedNo());
        cuttingInfo.setTotalBundleCount(plan.getBundleCount());
        cuttingInfo.setTotalQty(plan.getActualQty() != null ? plan.getActualQty() : plan.getPlanQty());
        cuttingInfo.setOrderNo(order != null ? order.getOrderNo() : null);
        cuttingInfo.setStyleNo(style != null ? style.getStyleCode() : null);
        // For cutting scan, color/size may vary across bundles; leave null at cutting level
        vo.setCutting(cuttingInfo);

        // Build bundle items
        List<CuttingScanVO.BundleItem> bundleItems = new ArrayList<>();
        for (Bundle b : bundles) {
            CuttingScanVO.BundleItem item = new CuttingScanVO.BundleItem();
            item.setId(b.getId());
            item.setGlobalBundleNo(b.getGlobalBundleNo());
            item.setBundleNo(b.getBundleNo());
            item.setQty(b.getQty());
            // Check if all processes for this bundle are already done
            item.setDisabled(false);
            item.setDisabledReason(null);
            bundleItems.add(item);
        }
        vo.setBundles(bundleItems);

        vo.setProcesses(buildProcessStatusList(plan.getOrderId(), sampleBundle != null ? sampleBundle.getId() : null));

        return vo;
    }

    private List<ProcessReportStatusVO> buildProcessStatusList(Long orderId, Long sampleBundleId) {
        if (orderId == null) {
            return Collections.emptyList();
        }

        List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>()
                        .eq(OrderProcess::getOrderId, orderId)
                        .orderByAsc(OrderProcess::getSort)
        );

        if (orderProcesses.isEmpty()) {
            return Collections.emptyList();
        }

        // Get existing work reports for the sample bundle
        Map<Long, WorkReport> existingReports = new HashMap<>();
        if (sampleBundleId != null) {
            List<WorkReport> reports = workReportMapper.selectList(
                    new LambdaQueryWrapper<WorkReport>()
                            .eq(WorkReport::getBundleId, sampleBundleId)
                            .eq(WorkReport::getStatus, 1)
            );
            for (WorkReport r : reports) {
                existingReports.put(r.getProcessId(), r);
            }
        }

        // Load process details
        Set<Long> processIds = orderProcesses.stream()
                .map(OrderProcess::getProcessId)
                .collect(Collectors.toSet());
        Map<Long, Process> processMap = new HashMap<>();
        if (!processIds.isEmpty()) {
            List<Process> processList = processMapper.selectBatchIds(processIds);
            for (Process p : processList) {
                processMap.put(p.getId(), p);
            }
        }

        List<ProcessReportStatusVO> result = new ArrayList<>();
        for (OrderProcess op : orderProcesses) {
            ProcessReportStatusVO statusVO = new ProcessReportStatusVO();
            statusVO.setProcessId(op.getProcessId());

            Process proc = processMap.get(op.getProcessId());
            statusVO.setProcessName(proc != null ? proc.getName() : "未知工序");
            statusVO.setPrice(op.getProcessPrice());

            WorkReport existingReport = existingReports.get(op.getProcessId());
            if (existingReport != null) {
                statusVO.setStatus("DONE");
                // Look up the employee who did the report
                if (existingReport.getEmployeeId() != null) {
                    Employee reporter = employeeMapper.selectById(existingReport.getEmployeeId());
                    statusVO.setDoneBy(reporter != null ? reporter.getName() : null);
                }
                if (existingReport.getReportTime() != null) {
                    statusVO.setDoneTime(existingReport.getReportTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
                statusVO.setHint("已报工");
            } else {
                statusVO.setStatus("AVAILABLE");
                statusVO.setHint(null);
            }

            result.add(statusVO);
        }

        return result;
    }

    @Transactional
    public ReportSubmitResultVO submitReport(Long employeeId, ReportSubmitRequest req) {
        String scanType = req.getScanType();
        String code = req.getCode();

        // Resolve bundles based on scan type to validate ownership
        List<Bundle> validBundles;
        Long orderId;

        if ("BUNDLE".equals(scanType)) {
            Bundle bundle = bundleMapper.selectOne(
                    new LambdaQueryWrapper<Bundle>()
                            .eq(Bundle::getGlobalBundleNo, code)
            );
            if (bundle == null) {
                throw new BizException("扎号不存在：" + code);
            }
            validBundles = Collections.singletonList(bundle);
            orderId = bundle.getOrderId();
        } else if ("CUTTING".equals(scanType)) {
            CuttingPlan plan = cuttingPlanMapper.selectOne(
                    new LambdaQueryWrapper<CuttingPlan>()
                            .eq(CuttingPlan::getBarcode, code)
            );
            if (plan == null) {
                throw new BizException("裁床计划不存在：" + code);
            }
            if (plan.getStatus() < 2) {
                throw new BizException("该裁床计划尚未开菲，无法报工");
            }
            validBundles = bundleMapper.selectList(
                    new LambdaQueryWrapper<Bundle>()
                            .eq(Bundle::getCuttingPlanId, plan.getId())
            );
            orderId = plan.getOrderId();
        } else {
            throw new BizException("无效的扫码类型：" + scanType);
        }

        // Validate selected bundle IDs belong to the scan result
        Set<Long> validBundleIds = validBundles.stream()
                .map(Bundle::getId)
                .collect(Collectors.toSet());
        for (Long bid : req.getSelectedBundleIds()) {
            if (!validBundleIds.contains(bid)) {
                throw new BizException("所选扎号不属于当前扫码结果");
            }
        }

        // Load selected processes
        List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>()
                        .eq(OrderProcess::getOrderId, orderId)
                        .in(OrderProcess::getProcessId, req.getSelectedProcessIds())
        );
        Map<Long, OrderProcess> opMap = orderProcesses.stream()
                .collect(Collectors.toMap(OrderProcess::getProcessId, op -> op, (a, b) -> a));

        // Load process names
        Map<Long, Process> processMap = new HashMap<>();
        if (!req.getSelectedProcessIds().isEmpty()) {
            List<Process> processList = processMapper.selectBatchIds(req.getSelectedProcessIds());
            for (Process p : processList) {
                processMap.put(p.getId(), p);
            }
        }

        Long factoryId = TenantContext.get();

        List<ReportSubmitResultVO.ReportItem> reportItems = new ArrayList<>();
        List<ReportSubmitResultVO.SkippedItem> skippedItems = new ArrayList<>();

        int totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Create a bundle id -> bundle map for lookup
        Map<Long, Bundle> bundleMap = validBundles.stream()
                .collect(Collectors.toMap(Bundle::getId, b -> b, (a, b) -> a));

        for (Long bundleId : req.getSelectedBundleIds()) {
            Bundle bundle = bundleMap.get(bundleId);

            for (Long processId : req.getSelectedProcessIds()) {
                // Check if already reported
                Long existingCount = workReportMapper.selectCount(
                        new LambdaQueryWrapper<WorkReport>()
                                .eq(WorkReport::getBundleId, bundleId)
                                .eq(WorkReport::getProcessId, processId)
                                .eq(WorkReport::getStatus, 1)
                );

                if (existingCount > 0) {
                    // Skip this bundle+process combo
                    ReportSubmitResultVO.SkippedItem skip = new ReportSubmitResultVO.SkippedItem();
                    skip.setBundleNo(bundle.getBundleNo());
                    Process proc = processMap.get(processId);
                    skip.setProcessName(proc != null ? proc.getName() : "未知工序");
                    skip.setReason("该扎号此工序已报工");
                    skippedItems.add(skip);
                    continue;
                }

                OrderProcess op = opMap.get(processId);
                BigDecimal price = op != null ? op.getProcessPrice() : BigDecimal.ZERO;
                int qty = bundle.getQty() != null ? bundle.getQty() : 0;
                BigDecimal amount = price.multiply(BigDecimal.valueOf(qty));

                // Create WorkReport
                WorkReport report = new WorkReport();
                report.setFactoryId(factoryId);
                report.setBundleId(bundleId);
                report.setOrderId(orderId);
                report.setProcessId(processId);
                report.setEmployeeId(employeeId);
                report.setQty(qty);
                report.setPrice(price);
                report.setAmount(amount);
                report.setReportType(1);
                report.setStatus(1);
                report.setReportTime(LocalDateTime.now());
                report.setSupplementFlag(0);
                report.setCreatedBy(employeeId);
                workReportMapper.insert(report);

                // Build report item
                ReportSubmitResultVO.ReportItem item = new ReportSubmitResultVO.ReportItem();
                item.setProcessId(processId);
                Process proc = processMap.get(processId);
                item.setProcessName(proc != null ? proc.getName() : "未知工序");
                item.setQty(qty);
                item.setPrice(price);
                item.setAmount(amount);
                reportItems.add(item);

                totalQty += qty;
                totalAmount = totalAmount.add(amount);
            }
        }

        if (reportItems.isEmpty()) {
            throw new BizException("所有选择项均已报工，无需重复提交");
        }

        ReportSubmitResultVO result = new ReportSubmitResultVO();
        result.setTotalQty(totalQty);
        result.setTotalAmount(totalAmount);
        result.setItems(reportItems);
        result.setSkippedItems(skippedItems);

        return result;
    }

    // ========== Wage & Records methods (Task 3) ==========

    public WageSummaryVO getWageSummary(Long employeeId) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String today = LocalDate.now().toString();

        List<WorkReport> reports = workReportMapper.selectList(
                new LambdaQueryWrapper<WorkReport>()
                        .eq(WorkReport::getEmployeeId, employeeId)
                        .eq(WorkReport::getStatus, 1)
                        .apply("DATE_FORMAT(report_time, '%Y-%m') = {0}", yearMonth)
        );

        BigDecimal todayAmount = BigDecimal.ZERO;
        BigDecimal monthAmount = BigDecimal.ZERO;

        // Group by date
        Map<String, List<WorkReport>> grouped = reports.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getReportTime().toLocalDate().toString(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<WageSummaryVO.DailyGroup> dailyGroups = new ArrayList<>();
        for (Map.Entry<String, List<WorkReport>> entry : grouped.entrySet()) {
            String date = entry.getKey();
            List<WorkReport> dayReports = entry.getValue();

            BigDecimal dayAmount = dayReports.stream()
                    .map(WorkReport::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (date.equals(today)) {
                todayAmount = dayAmount;
            }
            monthAmount = monthAmount.add(dayAmount);

            WageSummaryVO.DailyGroup dg = new WageSummaryVO.DailyGroup();
            dg.setDate(date);
            dg.setAmount(dayAmount);
            dg.setRecords(dayReports.stream()
                    .map(this::buildWorkReportRecordVO)
                    .collect(Collectors.toList()));
            dailyGroups.add(dg);
        }

        // Sort by date descending
        dailyGroups.sort(Comparator.comparing(WageSummaryVO.DailyGroup::getDate).reversed());

        WageSummaryVO vo = new WageSummaryVO();
        vo.setTodayAmount(todayAmount);
        vo.setMonthAmount(monthAmount);
        vo.setDailyGroups(dailyGroups);
        return vo;
    }

    public List<WorkReportRecordVO> getReportRecords(Long employeeId, String startDate, String endDate) {
        LambdaQueryWrapper<WorkReport> wrapper = new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getEmployeeId, employeeId)
                .eq(WorkReport::getStatus, 1);

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.apply("DATE(report_time) >= {0}", startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.apply("DATE(report_time) <= {0}", endDate);
        }

        wrapper.orderByDesc(WorkReport::getReportTime);

        List<WorkReport> reports = workReportMapper.selectList(wrapper);
        return reports.stream()
                .map(this::buildWorkReportRecordVO)
                .collect(Collectors.toList());
    }

    private WorkReportRecordVO buildWorkReportRecordVO(WorkReport r) {
        WorkReportRecordVO vo = new WorkReportRecordVO();
        vo.setId(r.getId());
        vo.setReportTime(r.getReportTime() != null ? r.getReportTime().toString() : null);
        vo.setQty(r.getQty());
        vo.setPrice(r.getPrice());
        vo.setAmount(r.getAmount());

        if (r.getOrderId() != null) {
            Order order = orderMapper.selectById(r.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                if (order.getStyleId() != null) {
                    Style style = styleMapper.selectById(order.getStyleId());
                    vo.setStyleNo(style != null ? style.getStyleCode() : null);
                }
            }
        }

        if (r.getBundleId() != null) {
            Bundle bundle = bundleMapper.selectById(r.getBundleId());
            vo.setBundleNo(bundle != null ? bundle.getBundleNo() : null);
        }

        if (r.getProcessId() != null) {
            Process process = processMapper.selectById(r.getProcessId());
            vo.setProcessName(process != null ? process.getName() : null);
        }

        return vo;
    }

    // ========== Admin methods (Task 4) ==========

    public List<DashboardOrderVO> getDashboardOrders(String date, Long orderId) {
        Long factoryId = TenantContext.get();

        // Load bundles, optionally filtered by orderId
        LambdaQueryWrapper<Bundle> bundleWrapper = new LambdaQueryWrapper<Bundle>()
                .eq(Bundle::getFactoryId, factoryId);
        if (orderId != null) {
            bundleWrapper.eq(Bundle::getOrderId, orderId);
        }
        List<Bundle> bundles = bundleMapper.selectList(bundleWrapper);

        // Group bundles by orderId
        Map<Long, List<Bundle>> bundlesByOrder = bundles.stream()
                .filter(b -> b.getOrderId() != null)
                .collect(Collectors.groupingBy(Bundle::getOrderId));

        List<DashboardOrderVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Bundle>> entry : bundlesByOrder.entrySet()) {
            Long oid = entry.getKey();
            List<Bundle> orderBundles = entry.getValue();

            Order order = orderMapper.selectById(oid);
            if (order == null) continue;

            Style style = order.getStyleId() != null ? styleMapper.selectById(order.getStyleId()) : null;

            // Get order processes
            List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                    new LambdaQueryWrapper<OrderProcess>()
                            .eq(OrderProcess::getOrderId, oid)
                            .orderByAsc(OrderProcess::getSort)
            );

            // Count total bundles for this order
            int totalBundles = orderBundles.size();

            // Get all work reports for bundles of this order
            Set<Long> bundleIds = orderBundles.stream().map(Bundle::getId).collect(Collectors.toSet());

            // Build per-process progress
            List<DashboardOrderVO.ProcessProgress> processProgressList = new ArrayList<>();
            BigDecimal lowestRate = BigDecimal.ONE;
            String bottleneckProcess = null;

            for (OrderProcess op : orderProcesses) {
                // Count bundles that have a work report for this process
                int completed = 0;
                if (!bundleIds.isEmpty()) {
                    Long completedCount = workReportMapper.selectCount(
                            new LambdaQueryWrapper<WorkReport>()
                                    .in(WorkReport::getBundleId, bundleIds)
                                    .eq(WorkReport::getProcessId, op.getProcessId())
                                    .eq(WorkReport::getStatus, 1)
                    );
                    completed = completedCount != null ? completedCount.intValue() : 0;
                }
                int total = totalBundles;
                BigDecimal rate = total > 0
                        ? BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(total), 4, BigDecimal.ROUND_HALF_UP)
                        : BigDecimal.ZERO;

                Process proc = processMapper.selectById(op.getProcessId());

                DashboardOrderVO.ProcessProgress pp = new DashboardOrderVO.ProcessProgress();
                pp.setProcessId(op.getProcessId());
                pp.setProcessName(proc != null ? proc.getName() : "未知工序");
                pp.setCompleted(completed);
                pp.setTotal(total);
                pp.setRate(rate);
                processProgressList.add(pp);

                if (rate.compareTo(lowestRate) < 0) {
                    lowestRate = rate;
                    bottleneckProcess = proc != null ? proc.getName() : "未知工序";
                }
            }

            // Overall completion rate = average of process rates, or 0 if no processes
            BigDecimal completionRate = BigDecimal.ZERO;
            if (!processProgressList.isEmpty()) {
                BigDecimal sum = processProgressList.stream()
                        .map(DashboardOrderVO.ProcessProgress::getRate)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                completionRate = sum.divide(BigDecimal.valueOf(processProgressList.size()), 4, BigDecimal.ROUND_HALF_UP);
            }

            DashboardOrderVO vo = new DashboardOrderVO();
            vo.setOrderId(oid);
            vo.setOrderNo(order.getOrderNo());
            vo.setStyleNo(style != null ? style.getStyleCode() : null);
            vo.setTotalBundles(totalBundles);
            vo.setCompletionRate(completionRate);
            vo.setBottleneckProcess(bottleneckProcess);
            vo.setProcessProgress(processProgressList);
            result.add(vo);
        }

        return result;
    }

    public List<DashboardWorkerVO> getDashboardWorkers(String date) {
        Long factoryId = TenantContext.get();

        LambdaQueryWrapper<WorkReport> wrapper = new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, factoryId)
                .eq(WorkReport::getStatus, 1)
                .apply("DATE(report_time) = {0}", date);

        List<WorkReport> reports = workReportMapper.selectList(wrapper);

        // Group by employee
        Map<Long, List<WorkReport>> byEmployee = reports.stream()
                .filter(r -> r.getEmployeeId() != null)
                .collect(Collectors.groupingBy(WorkReport::getEmployeeId, LinkedHashMap::new, Collectors.toList()));

        List<DashboardWorkerVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<WorkReport>> entry : byEmployee.entrySet()) {
            Long empId = entry.getKey();
            List<WorkReport> empReports = entry.getValue();

            Employee emp = employeeMapper.selectById(empId);

            DashboardWorkerVO vo = new DashboardWorkerVO();
            vo.setEmployeeId(empId);
            vo.setEmployeeNo(emp != null ? emp.getEmployeeNo() : null);
            vo.setEmployeeName(emp != null ? emp.getName() : null);
            vo.setTotalQty(empReports.stream().mapToInt(r -> r.getQty() != null ? r.getQty() : 0).sum());
            vo.setTotalAmount(empReports.stream()
                    .map(WorkReport::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(vo);
        }

        return result;
    }

    public List<AnomalyVO> getAnomalies(Integer status) {
        Long factoryId = TenantContext.get();

        LambdaQueryWrapper<AnomalyAlert> wrapper = new LambdaQueryWrapper<AnomalyAlert>()
                .eq(AnomalyAlert::getFactoryId, factoryId);
        if (status != null) {
            wrapper.eq(AnomalyAlert::getStatus, status);
        }
        wrapper.orderByDesc(AnomalyAlert::getCreatedAt);

        List<AnomalyAlert> alerts = anomalyAlertMapper.selectList(wrapper);

        return alerts.stream().map(alert -> {
            AnomalyVO vo = new AnomalyVO();
            vo.setId(alert.getId());
            vo.setStatus(alert.getStatus());
            vo.setHandleType(alert.getHandleType());
            vo.setHandleRemark(alert.getHandleRemark());

            // Bundle info
            if (alert.getBundleId() != null) {
                Bundle bundle = bundleMapper.selectById(alert.getBundleId());
                vo.setBundleNo(bundle != null ? bundle.getBundleNo() : null);
            }

            // Order info
            if (alert.getOrderId() != null) {
                Order order = orderMapper.selectById(alert.getOrderId());
                vo.setOrderNo(order != null ? order.getOrderNo() : null);
            }

            // Process names
            if (alert.getFrontProcessId() != null) {
                Process frontProc = processMapper.selectById(alert.getFrontProcessId());
                vo.setFrontProcessName(frontProc != null ? frontProc.getName() : null);
            }
            if (alert.getBackProcessId() != null) {
                Process backProc = processMapper.selectById(alert.getBackProcessId());
                vo.setBackProcessName(backProc != null ? backProc.getName() : null);
            }

            // Reporter info
            if (alert.getReportEmployeeId() != null) {
                Employee reporter = employeeMapper.selectById(alert.getReportEmployeeId());
                vo.setReportEmployeeName(reporter != null ? reporter.getName() : null);
            }

            // Report time
            vo.setReportTime(alert.getCreatedAt() != null
                    ? alert.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : null);

            // Description: front process worker != back process worker
            vo.setDescription("工序报工人员异常：前工序与后工序报工人员不一致");

            return vo;
        }).collect(Collectors.toList());
    }

    public void ignoreAnomaly(Long id, String remark) {
        anomalyService.ignore(id, remark);
    }

    public void clearAnomaly(Long id, String reason) {
        anomalyService.clearReport(id, reason);
    }

    public void supplementAnomaly(Long id, Long employeeId, Integer qty) {
        anomalyService.supplement(id, employeeId, qty);
    }

    public List<TimedWageVO> getTimedWages(String yearMonth) {
        Long factoryId = TenantContext.get();

        LambdaQueryWrapper<TimedWage> wrapper = new LambdaQueryWrapper<TimedWage>()
                .eq(TimedWage::getFactoryId, factoryId);
        if (yearMonth != null && !yearMonth.isEmpty()) {
            wrapper.apply("DATE_FORMAT(work_date, '%Y-%m') = {0}", yearMonth);
        }
        wrapper.orderByDesc(TimedWage::getWorkDate);

        List<TimedWage> wages = timedWageMapper.selectList(wrapper);

        return wages.stream().map(w -> {
            TimedWageVO vo = new TimedWageVO();
            vo.setId(w.getId());
            vo.setEmployeeId(w.getEmployeeId());
            vo.setWorkDate(w.getWorkDate());
            vo.setHours(w.getHours());
            vo.setHourlyRate(w.getHourlyRate());
            vo.setAmount(w.getAmount());
            vo.setWorkContent(w.getWorkContent());

            if (w.getEmployeeId() != null) {
                Employee emp = employeeMapper.selectById(w.getEmployeeId());
                if (emp != null) {
                    vo.setEmployeeNo(emp.getEmployeeNo());
                    vo.setEmployeeName(emp.getName());
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void createTimedWage(TimedWageRequest req) {
        Long factoryId = TenantContext.get();
        Long currentUserId = StpUtil.getLoginIdAsLong();

        TimedWage wage = new TimedWage();
        wage.setFactoryId(factoryId);
        wage.setEmployeeId(req.getEmployeeId());
        if (req.getWorkDate() != null && !req.getWorkDate().isEmpty()) {
            wage.setWorkDate(LocalDate.parse(req.getWorkDate()));
        } else {
            wage.setWorkDate(LocalDate.now());
        }
        wage.setHours(req.getHours());
        wage.setHourlyRate(req.getHourlyRate());
        wage.setAmount(req.getHours().multiply(req.getHourlyRate()));
        wage.setWorkContent(req.getWorkContent());
        wage.setCreatedBy(currentUserId);
        timedWageMapper.insert(wage);
    }

    @Transactional
    public void updateTimedWage(Long id, TimedWageRequest req) {
        TimedWage wage = timedWageMapper.selectById(id);
        if (wage == null) {
            throw new BizException("计时工资记录不存在");
        }
        wage.setEmployeeId(req.getEmployeeId());
        if (req.getWorkDate() != null && !req.getWorkDate().isEmpty()) {
            wage.setWorkDate(LocalDate.parse(req.getWorkDate()));
        }
        wage.setHours(req.getHours());
        wage.setHourlyRate(req.getHourlyRate());
        wage.setAmount(req.getHours().multiply(req.getHourlyRate()));
        wage.setWorkContent(req.getWorkContent());
        timedWageMapper.updateById(wage);
    }

    @Transactional
    public void deleteTimedWage(Long id) {
        timedWageMapper.deleteById(id);
    }

    public BundleDetailVO searchBundle(String code) {
        Bundle bundle;
        String upperCode = code.trim().toUpperCase();
        if (upperCode.startsWith("BUN")) {
            bundle = bundleMapper.selectOne(
                    new LambdaQueryWrapper<Bundle>()
                            .eq(Bundle::getGlobalBundleNo, code.trim())
            );
        } else {
            bundle = bundleMapper.selectOne(
                    new LambdaQueryWrapper<Bundle>()
                            .eq(Bundle::getBundleNo, code.trim())
            );
        }

        if (bundle == null) {
            throw new BizException("扎号不存在：" + code);
        }

        Order order = bundle.getOrderId() != null ? orderMapper.selectById(bundle.getOrderId()) : null;
        Style style = (order != null && order.getStyleId() != null) ? styleMapper.selectById(order.getStyleId()) : null;

        BundleDetailVO vo = new BundleDetailVO();
        vo.setId(bundle.getId());
        vo.setGlobalBundleNo(bundle.getGlobalBundleNo());
        vo.setBundleNo(bundle.getBundleNo());
        vo.setQty(bundle.getQty());
        vo.setOrderNo(order != null ? order.getOrderNo() : null);
        vo.setStyleNo(style != null ? style.getStyleCode() : null);
        vo.setColor(bundle.getColor());
        vo.setSize(bundle.getSize());

        // Build process status list
        List<ProcessReportStatusVO> processes = buildProcessStatusList(bundle.getOrderId(), bundle.getId());
        vo.setProcesses(processes);

        // Determine status: 已完成 if all processes are DONE, else 生产中
        boolean allDone = !processes.isEmpty() && processes.stream().allMatch(p -> "DONE".equals(p.getStatus()));
        vo.setStatus(allDone ? "已完成" : "生产中");

        return vo;
    }

    public OrderSearchVO searchOrder(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BizException("订单不存在：" + orderNo);
        }

        OrderSearchVO vo = new OrderSearchVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDeliveryDate(order.getDeliveryDate());
        vo.setStatus(order.getStatus());

        // Customer name
        if (order.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            vo.setCustomerName(customer != null ? customer.getName() : null);
        }

        // Order items
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
        );

        // Get all bundles for this order to calculate progress
        List<Bundle> orderBundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getOrderId, order.getId())
        );

        // Get order processes for completion calculation
        List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                new LambdaQueryWrapper<OrderProcess>()
                        .eq(OrderProcess::getOrderId, order.getId())
                        .orderByAsc(OrderProcess::getSort)
        );

        // Calculate overall completion rate
        BigDecimal completionRate = BigDecimal.ZERO;
        if (!orderBundles.isEmpty() && !orderProcesses.isEmpty()) {
            Set<Long> bundleIds = orderBundles.stream().map(Bundle::getId).collect(Collectors.toSet());
            int totalExpected = orderBundles.size() * orderProcesses.size();
            int completedCount = 0;
            if (!bundleIds.isEmpty()) {
                Long completedCountLong = workReportMapper.selectCount(
                        new LambdaQueryWrapper<WorkReport>()
                                .in(WorkReport::getBundleId, bundleIds)
                                .eq(WorkReport::getStatus, 1)
                );
                completedCount = completedCountLong != null ? completedCountLong.intValue() : 0;
            }
            if (totalExpected > 0) {
                completionRate = BigDecimal.valueOf(completedCount)
                        .divide(BigDecimal.valueOf(totalExpected), 4, BigDecimal.ROUND_HALF_UP);
            }
        }
        vo.setCompletionRate(completionRate);

        // Build order items
        List<OrderSearchVO.OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderSearchVO.OrderItemVO itemVO = new OrderSearchVO.OrderItemVO();
            if (item.getStyleId() != null) {
                Style s = styleMapper.selectById(item.getStyleId());
                itemVO.setStyleNo(s != null ? s.getStyleCode() : null);
            }
            itemVO.setColor(item.getColor());
            itemVO.setSize(item.getSize());
            itemVO.setQty(item.getQty());
            itemVO.setShippedQty(0); // No shipment tracking yet
            itemVO.setProgress(BigDecimal.ZERO);
            return itemVO;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }
}

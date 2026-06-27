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
import com.zhiyi.erp.module.base.entity.StyleProcess;
import com.zhiyi.erp.module.base.mapper.CustomerMapper;
import com.zhiyi.erp.module.base.mapper.ProcessMapper;
import com.zhiyi.erp.module.base.mapper.StyleMapper;
import com.zhiyi.erp.module.base.mapper.StyleProcessMapper;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.mapper.BundleMapper;
import com.zhiyi.erp.module.cutting.mapper.CuttingPlanMapper;
import com.zhiyi.erp.module.mobile.dto.EmployeeRequest;
import com.zhiyi.erp.module.mobile.dto.TimedWageRequest;
import com.zhiyi.erp.module.mobile.dto.ReportSubmitRequest;
import com.zhiyi.erp.module.mobile.vo.*;
import com.zhiyi.erp.module.report.dto.ReportRecordDTO;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.entity.OrderItem;
import com.zhiyi.erp.module.order.entity.OrderProcess;
import com.zhiyi.erp.module.order.mapper.OrderItemMapper;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import com.zhiyi.erp.module.order.mapper.OrderProcessMapper;
import com.zhiyi.erp.module.report.dto.WageReportDTO;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.entity.Menu;
import com.zhiyi.erp.module.system.entity.Role;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import com.zhiyi.erp.module.system.mapper.RoleMapper;
import com.zhiyi.erp.module.system.service.MenuService;
import com.zhiyi.erp.common.util.EmployeeNoGenerator;
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
    private EmployeeNoGenerator employeeNoGenerator;

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
    private StyleProcessMapper styleProcessMapper;

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

    public void changePassword(Long employeeId, String oldPassword, String newPassword) {
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        if (!BCrypt.checkpw(oldPassword, employee.getLoginPassword())) {
            throw new BizException("旧密码错误");
        }
        employee.setLoginPassword(BCrypt.hashpw(newPassword));
        employeeMapper.updateById(employee);
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

        vo.setShowMobilePrice(style != null && style.getShowMobilePrice() != null ? style.getShowMobilePrice() : 1);
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
                        .orderByAsc(Bundle::getColor)
                        .orderByAsc(Bundle::getSize)
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
        vo.setCutting(cuttingInfo);

        // Build bundle items
        List<CuttingScanVO.BundleItem> bundleItems = new ArrayList<>();
        for (Bundle b : bundles) {
            CuttingScanVO.BundleItem item = new CuttingScanVO.BundleItem();
            item.setId(b.getId());
            item.setGlobalBundleNo(b.getGlobalBundleNo());
            item.setBundleNo(b.getBundleNo());
            item.setQty(b.getQty());
            item.setDisabled(false);
            item.setDisabledReason(null);
            bundleItems.add(item);
        }
        vo.setBundles(bundleItems);

        // Build color/size matrix (按物理扎去重，同一扎号只计算一次)
        Map<String, CuttingScanVO.ColorSizeItem> matrixMap = new LinkedHashMap<>();
        Set<String> countedBundles = new HashSet<>();
        for (Bundle b : bundles) {
            String csKey = (b.getColor() != null ? b.getColor() : "") + "|" + (b.getSize() != null ? b.getSize() : "");
            String bundleKey = b.getBundleNo() + "|" + csKey;
            CuttingScanVO.ColorSizeItem csItem = matrixMap.get(csKey);
            if (csItem == null) {
                csItem = new CuttingScanVO.ColorSizeItem();
                csItem.setColor(b.getColor());
                csItem.setSize(b.getSize());
                csItem.setQty(0);
                csItem.setBundleIds(new ArrayList<>());
                matrixMap.put(csKey, csItem);
            }
            // 同一物理扎只计算一次数量
            if (!countedBundles.contains(bundleKey)) {
                csItem.setQty(csItem.getQty() + (b.getQty() != null ? b.getQty() : 0));
                countedBundles.add(bundleKey);
            }
            csItem.getBundleIds().add(b.getId());
        }
        vo.setColorSizeMatrix(new ArrayList<>(matrixMap.values()));

        vo.setShowMobilePrice(style != null && style.getShowMobilePrice() != null ? style.getShowMobilePrice() : 1);
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

        // 获取订单对应的款号工序（用于读取 batchReport）
        Order order = orderMapper.selectById(orderId);
        Long styleId = order != null ? order.getStyleId() : null;
        Map<Long, Integer> styleBatchReportMap = new HashMap<>();
        if (styleId != null) {
            List<StyleProcess> styleProcesses = styleProcessMapper.selectList(
                    new LambdaQueryWrapper<StyleProcess>()
                            .eq(StyleProcess::getStyleId, styleId)
            );
            for (StyleProcess sp : styleProcesses) {
                styleBatchReportMap.put(sp.getProcessId(), sp.getBatchReport() != null ? sp.getBatchReport() : 1);
            }
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
            // 优先从款号工序读取 batchReport，实时生效
            statusVO.setBatchReport(styleBatchReportMap.getOrDefault(op.getProcessId(), 1));

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

    public WageSummaryVO getWageSummary(Long employeeId, String startDate, String endDate) {
        // 默认本月
        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().withDayOfMonth(1).toString();
        }
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }

        // 单条 SQL 查询，JOIN 关联表
        List<WageReportDTO> reports = workReportMapper.selectWageReports(employeeId, startDate, endDate);

        if (reports.isEmpty()) {
            WageSummaryVO empty = new WageSummaryVO();
            empty.setTotalQty(0);
            empty.setTotalAmount(BigDecimal.ZERO);
            empty.setVisibleAmount(BigDecimal.ZERO);
            empty.setStyleSummaries(new ArrayList<>());
            empty.setDailyGroups(new ArrayList<>());
            return empty;
        }

        // 按款号分组
        Map<String, List<WageReportDTO>> byStyle = new LinkedHashMap<>();
        for (WageReportDTO r : reports) {
            String styleNo = r.getStyleCode() != null ? r.getStyleCode() : "未知款号";
            byStyle.computeIfAbsent(styleNo, k -> new ArrayList<>()).add(r);
        }

        // 构建款号汇总
        int totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal visibleAmount = BigDecimal.ZERO;
        List<WageSummaryVO.StyleSummary> styleSummaries = new ArrayList<>();

        for (Map.Entry<String, List<WageReportDTO>> entry : byStyle.entrySet()) {
            String styleNo = entry.getKey();
            List<WageReportDTO> styleReports = entry.getValue();
            int showPrice = styleReports.get(0).getShowMobilePrice() != null ? styleReports.get(0).getShowMobilePrice() : 1;

            int qty = styleReports.stream().mapToInt(r -> r.getQty() != null ? r.getQty() : 0).sum();
            BigDecimal amount = styleReports.stream()
                    .map(WageReportDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalQty += qty;
            totalAmount = totalAmount.add(amount);
            if (showPrice == 1) {
                visibleAmount = visibleAmount.add(amount);
            }

            // 按工序分组
            Map<Long, List<WageReportDTO>> byProcess = new LinkedHashMap<>();
            for (WageReportDTO r : styleReports) {
                byProcess.computeIfAbsent(r.getProcessId(), k -> new ArrayList<>()).add(r);
            }
            List<WageSummaryVO.ProcessSummary> processSummaries = new ArrayList<>();
            for (Map.Entry<Long, List<WageReportDTO>> procEntry : byProcess.entrySet()) {
                List<WageReportDTO> procReports = procEntry.getValue();
                WageSummaryVO.ProcessSummary ps = new WageSummaryVO.ProcessSummary();
                ps.setProcessName(procReports.get(0).getProcessName() != null ? procReports.get(0).getProcessName() : "未知工序");
                ps.setQty(procReports.stream().mapToInt(r -> r.getQty() != null ? r.getQty() : 0).sum());
                ps.setPrice(procReports.get(0).getPrice());
                ps.setAmount(procReports.stream()
                        .map(WageReportDTO::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                processSummaries.add(ps);
            }

            WageSummaryVO.StyleSummary ss = new WageSummaryVO.StyleSummary();
            ss.setStyleNo(styleNo);
            ss.setQty(qty);
            ss.setAmount(amount);
            ss.setShowMobilePrice(showPrice);
            ss.setProcesses(processSummaries);
            styleSummaries.add(ss);
        }

        // 按日期分组
        Map<String, List<WageReportDTO>> byDate = reports.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getReportTime().toLocalDate().toString(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<WageSummaryVO.DailyGroup> dailyGroups = new ArrayList<>();
        for (Map.Entry<String, List<WageReportDTO>> entry : byDate.entrySet()) {
            WageSummaryVO.DailyGroup dg = new WageSummaryVO.DailyGroup();
            dg.setDate(entry.getKey());
            dg.setAmount(entry.getValue().stream()
                    .map(WageReportDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            dg.setRecords(entry.getValue().stream()
                    .map(r -> {
                        WorkReportRecordVO vo = new WorkReportRecordVO();
                        vo.setId(r.getId());
                        vo.setReportTime(r.getReportTime() != null ? r.getReportTime().toString() : null);
                        vo.setQty(r.getQty());
                        vo.setPrice(r.getPrice());
                        vo.setAmount(r.getAmount());
                        vo.setOrderNo(r.getOrderNo());
                        vo.setStyleNo(r.getStyleCode());
                        vo.setProcessName(r.getProcessName());
                        return vo;
                    })
                    .collect(Collectors.toList()));
            dailyGroups.add(dg);
        }
        dailyGroups.sort(Comparator.comparing(WageSummaryVO.DailyGroup::getDate).reversed());

        WageSummaryVO vo = new WageSummaryVO();
        vo.setTotalQty(totalQty);
        vo.setTotalAmount(totalAmount);
        vo.setVisibleAmount(visibleAmount);
        vo.setStyleSummaries(styleSummaries);
        vo.setDailyGroups(dailyGroups);
        return vo;
    }

    public ReportRecordsVO getReportRecords(Long employeeId, String startDate, String endDate, int page, int size) {
        int offset = (page - 1) * size;
        long total = workReportMapper.selectReportRecordsCount(employeeId, startDate, endDate);
        List<ReportRecordDTO> records = workReportMapper.selectReportRecords(employeeId, startDate, endDate, offset, size);

        ReportRecordsVO vo = new ReportRecordsVO();
        vo.setRecords(records.stream().map(r -> {
            WorkReportRecordVO item = new WorkReportRecordVO();
            item.setId(r.getId());
            item.setReportTime(r.getReportTime() != null ? r.getReportTime().toString() : null);
            item.setQty(r.getQty());
            item.setPrice(r.getPrice());
            item.setAmount(r.getAmount());
            item.setOrderNo(r.getOrderNo());
            item.setStyleNo(r.getStyleCode());
            item.setBundleNo(r.getBundleNo());
            item.setProcessName(r.getProcessName());
            item.setShowMobilePrice(r.getShowMobilePrice() != null ? r.getShowMobilePrice() : 1);
            return item;
        }).collect(Collectors.toList()));
        vo.setTotal(total);
        vo.setPage(page);
        vo.setSize(size);
        vo.setHasMore(offset + records.size() < total);
        return vo;
    }

    // ========== Admin methods (Task 4) ==========

    public DashboardOrderPageVO getDashboardOrders(String startDate, String endDate, String orderNo, int page, int size) {
        Long factoryId = TenantContext.get();

        // 如果有日期范围，先查出该范围内有报工的订单ID
        Set<Long> orderIdsFromReports = null;
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            LambdaQueryWrapper<WorkReport> reportWrapper = new LambdaQueryWrapper<WorkReport>()
                    .eq(WorkReport::getFactoryId, factoryId)
                    .eq(WorkReport::getStatus, 1)
                    .apply("DATE(report_time) >= {0}", startDate)
                    .apply("DATE(report_time) <= {0}", endDate)
                    .select(WorkReport::getOrderId);
            List<WorkReport> reports = workReportMapper.selectList(reportWrapper);
            orderIdsFromReports = reports.stream()
                    .map(WorkReport::getOrderId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (orderIdsFromReports.isEmpty()) {
                DashboardOrderPageVO empty = new DashboardOrderPageVO();
                empty.setRecords(new ArrayList<>());
                empty.setTotal(0);
                empty.setPage(page);
                empty.setSize(size);
                empty.setHasMore(false);
                return empty;
            }
        }

        // 构建订单查询
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getFactoryId, factoryId);
        if (orderIdsFromReports != null) {
            orderWrapper.in(Order::getId, orderIdsFromReports);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            orderWrapper.like(Order::getOrderNo, orderNo.trim());
        }
        orderWrapper.orderByDesc(Order::getCreatedAt);

        // 分页
        long total = orderMapper.selectCount(orderWrapper);
        int offset = (page - 1) * size;
        orderWrapper.last("LIMIT " + offset + ", " + size);
        List<Order> orders = orderMapper.selectList(orderWrapper);

        List<DashboardOrderVO> result = new ArrayList<>();
        for (Order order : orders) {
            Long oid = order.getId();

            Style style = order.getStyleId() != null ? styleMapper.selectById(order.getStyleId()) : null;

            List<Bundle> orderBundles = bundleMapper.selectList(
                    new LambdaQueryWrapper<Bundle>().eq(Bundle::getOrderId, oid)
            );

            List<OrderProcess> orderProcesses = orderProcessMapper.selectList(
                    new LambdaQueryWrapper<OrderProcess>()
                            .eq(OrderProcess::getOrderId, oid)
                            .orderByAsc(OrderProcess::getSort)
            );

            int totalBundles = orderBundles.size();
            Set<Long> bundleIds = orderBundles.stream().map(Bundle::getId).collect(Collectors.toSet());

            List<DashboardOrderVO.ProcessProgress> processProgressList = new ArrayList<>();
            BigDecimal lowestRate = BigDecimal.ONE;
            String bottleneckProcess = null;

            for (OrderProcess op : orderProcesses) {
                int completed = 0;
                if (!bundleIds.isEmpty()) {
                    LambdaQueryWrapper<WorkReport> wrWrapper = new LambdaQueryWrapper<WorkReport>()
                            .in(WorkReport::getBundleId, bundleIds)
                            .eq(WorkReport::getProcessId, op.getProcessId())
                            .eq(WorkReport::getStatus, 1);
                    if (startDate != null && !startDate.isEmpty()) {
                        wrWrapper.apply("DATE(report_time) >= {0}", startDate);
                    }
                    if (endDate != null && !endDate.isEmpty()) {
                        wrWrapper.apply("DATE(report_time) <= {0}", endDate);
                    }
                    Long completedCount = workReportMapper.selectCount(wrWrapper);
                    completed = completedCount != null ? completedCount.intValue() : 0;
                }
                BigDecimal rate = totalBundles > 0
                        ? BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(totalBundles), 4, BigDecimal.ROUND_HALF_UP)
                        : BigDecimal.ZERO;

                Process proc = processMapper.selectById(op.getProcessId());

                DashboardOrderVO.ProcessProgress pp = new DashboardOrderVO.ProcessProgress();
                pp.setProcessId(op.getProcessId());
                pp.setProcessName(proc != null ? proc.getName() : "未知工序");
                pp.setCompleted(completed);
                pp.setTotal(totalBundles);
                pp.setRate(rate);
                processProgressList.add(pp);

                if (rate.compareTo(lowestRate) < 0) {
                    lowestRate = rate;
                    bottleneckProcess = proc != null ? proc.getName() : "未知工序";
                }
            }

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
            vo.setOrderDate(order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate().toString() : null);
            vo.setTotalBundles(totalBundles);
            vo.setCompletionRate(completionRate);
            vo.setBottleneckProcess(bottleneckProcess);
            vo.setProcessProgress(processProgressList);
            result.add(vo);
        }

        DashboardOrderPageVO pageResult = new DashboardOrderPageVO();
        pageResult.setRecords(result);
        pageResult.setTotal(total);
        pageResult.setPage(page);
        pageResult.setSize(size);
        pageResult.setHasMore(offset + result.size() < total);
        return pageResult;
    }

    public DashboardWorkerPageVO getDashboardWorkers(String startDate, String endDate, String keyword, int page, int size) {
        Long factoryId = TenantContext.get();

        // 默认本月
        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().withDayOfMonth(1).toString();
        }
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }

        LambdaQueryWrapper<WorkReport> wrapper = new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, factoryId)
                .eq(WorkReport::getStatus, 1)
                .apply("DATE(report_time) >= {0}", startDate)
                .apply("DATE(report_time) <= {0}", endDate);

        List<WorkReport> reports = workReportMapper.selectList(wrapper);

        // Group by employee
        Map<Long, List<WorkReport>> byEmployee = reports.stream()
                .filter(r -> r.getEmployeeId() != null)
                .collect(Collectors.groupingBy(WorkReport::getEmployeeId, LinkedHashMap::new, Collectors.toList()));

        // 构建工人列表
        List<DashboardWorkerVO> allWorkers = new ArrayList<>();

        for (Map.Entry<Long, List<WorkReport>> entry : byEmployee.entrySet()) {
            Long empId = entry.getKey();
            List<WorkReport> empReports = entry.getValue();

            Employee emp = employeeMapper.selectById(empId);

            int qty = empReports.stream().mapToInt(r -> r.getQty() != null ? r.getQty() : 0).sum();
            BigDecimal amount = empReports.stream()
                    .map(WorkReport::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            DashboardWorkerVO vo = new DashboardWorkerVO();
            vo.setEmployeeId(empId);
            vo.setEmployeeNo(emp != null ? emp.getEmployeeNo() : null);
            vo.setEmployeeName(emp != null ? emp.getName() : null);
            vo.setTotalQty(qty);
            vo.setTotalAmount(amount);
            allWorkers.add(vo);
        }

        // 关键词筛选
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            allWorkers = allWorkers.stream()
                    .filter(w -> (w.getEmployeeName() != null && w.getEmployeeName().contains(kw))
                            || (w.getEmployeeNo() != null && w.getEmployeeNo().contains(kw)))
                    .collect(Collectors.toList());
        }

        // 按金额降序
        allWorkers.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        // 汇总（基于筛选后的数据）
        int totalQty = allWorkers.stream().mapToInt(DashboardWorkerVO::getTotalQty).sum();
        BigDecimal totalAmount = allWorkers.stream()
                .map(DashboardWorkerVO::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 分页
        long total = allWorkers.size();
        int offset = (page - 1) * size;
        List<DashboardWorkerVO> pagedWorkers = allWorkers.stream()
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());

        DashboardWorkerPageVO result = new DashboardWorkerPageVO();
        result.setWorkers(pagedWorkers);
        result.setTotalQty(totalQty);
        result.setTotalAmount(totalAmount);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setHasMore(offset + pagedWorkers.size() < total);
        return result;
    }

    public WorkerDetailVO getWorkerDetail(Long employeeId, String startDate, String endDate) {
        Long factoryId = TenantContext.get();

        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().withDayOfMonth(1).toString();
        }
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }

        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) {
            throw new BizException("员工不存在");
        }

        // 查询报工记录
        LambdaQueryWrapper<WorkReport> wrapper = new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, factoryId)
                .eq(WorkReport::getEmployeeId, employeeId)
                .eq(WorkReport::getStatus, 1)
                .apply("DATE(report_time) >= {0}", startDate)
                .apply("DATE(report_time) <= {0}", endDate)
                .orderByDesc(WorkReport::getReportTime);
        List<WorkReport> reports = workReportMapper.selectList(wrapper);

        // 批量加载关联数据
        Set<Long> orderIds = reports.stream().map(WorkReport::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> processIds = reports.stream().map(WorkReport::getProcessId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> bundleIds = reports.stream().map(WorkReport::getBundleId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Order> orderMap = orderIds.isEmpty() ? new HashMap<>() :
                orderMapper.selectBatchIds(orderIds).stream().collect(Collectors.toMap(Order::getId, o -> o));
        Set<Long> styleIds = orderMap.values().stream().map(Order::getStyleId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Style> styleMap = styleIds.isEmpty() ? new HashMap<>() :
                styleMapper.selectBatchIds(styleIds).stream().collect(Collectors.toMap(Style::getId, s -> s));
        Map<Long, Process> processMap = processIds.isEmpty() ? new HashMap<>() :
                processMapper.selectBatchIds(processIds).stream().collect(Collectors.toMap(Process::getId, p -> p));
        Map<Long, Bundle> bundleMap = bundleIds.isEmpty() ? new HashMap<>() :
                bundleMapper.selectBatchIds(bundleIds).stream().collect(Collectors.toMap(Bundle::getId, b -> b));

        // 汇总
        int totalQty = reports.stream().mapToInt(r -> r.getQty() != null ? r.getQty() : 0).sum();
        BigDecimal totalAmount = reports.stream()
                .map(WorkReport::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按订单汇总
        Map<Long, List<WorkReport>> byOrderMap = reports.stream()
                .filter(r -> r.getOrderId() != null)
                .collect(Collectors.groupingBy(WorkReport::getOrderId, LinkedHashMap::new, Collectors.toList()));
        List<WorkerDetailVO.OrderSummary> byOrder = new ArrayList<>();
        for (Map.Entry<Long, List<WorkReport>> entry : byOrderMap.entrySet()) {
            Order order = orderMap.get(entry.getKey());
            Style style = order != null && order.getStyleId() != null ? styleMap.get(order.getStyleId()) : null;
            List<WorkReport> orderReports = entry.getValue();

            WorkerDetailVO.OrderSummary os = new WorkerDetailVO.OrderSummary();
            os.setOrderId(entry.getKey());
            os.setOrderNo(order != null ? order.getOrderNo() : null);
            os.setStyleNo(style != null ? style.getStyleCode() : null);
            os.setQty(orderReports.stream().mapToInt(r -> r.getQty() != null ? r.getQty() : 0).sum());
            os.setAmount(orderReports.stream().map(WorkReport::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            byOrder.add(os);
        }
        byOrder.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        // 按工序汇总
        Map<Long, List<WorkReport>> byProcessMap = reports.stream()
                .filter(r -> r.getProcessId() != null)
                .collect(Collectors.groupingBy(WorkReport::getProcessId, LinkedHashMap::new, Collectors.toList()));
        List<WorkerDetailVO.ProcessSummary> byProcess = new ArrayList<>();
        for (Map.Entry<Long, List<WorkReport>> entry : byProcessMap.entrySet()) {
            Process proc = processMap.get(entry.getKey());
            List<WorkReport> procReports = entry.getValue();

            WorkerDetailVO.ProcessSummary ps = new WorkerDetailVO.ProcessSummary();
            ps.setProcessId(entry.getKey());
            ps.setProcessName(proc != null ? proc.getName() : null);
            ps.setQty(procReports.stream().mapToInt(r -> r.getQty() != null ? r.getQty() : 0).sum());
            ps.setAmount(procReports.stream().map(WorkReport::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            byProcess.add(ps);
        }
        byProcess.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        // 按日期分组
        Map<String, List<WorkReport>> byDateMap = reports.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getReportTime().toLocalDate().toString(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<WorkerDetailVO.DailyGroup> dailyReports = new ArrayList<>();
        for (Map.Entry<String, List<WorkReport>> entry : byDateMap.entrySet()) {
            List<WorkReport> dayReports = entry.getValue();

            List<WorkerDetailVO.ReportRecord> records = dayReports.stream().map(r -> {
                WorkerDetailVO.ReportRecord rec = new WorkerDetailVO.ReportRecord();
                rec.setId(r.getId());
                Order order = r.getOrderId() != null ? orderMap.get(r.getOrderId()) : null;
                Style style = order != null && order.getStyleId() != null ? styleMap.get(order.getStyleId()) : null;
                rec.setOrderNo(order != null ? order.getOrderNo() : null);
                rec.setStyleNo(style != null ? style.getStyleCode() : null);
                Process proc = r.getProcessId() != null ? processMap.get(r.getProcessId()) : null;
                rec.setProcessName(proc != null ? proc.getName() : null);
                Bundle bundle = r.getBundleId() != null ? bundleMap.get(r.getBundleId()) : null;
                rec.setBundleNo(bundle != null ? bundle.getBundleNo() : null);
                rec.setQty(r.getQty());
                rec.setPrice(r.getPrice());
                rec.setAmount(r.getAmount());
                return rec;
            }).collect(Collectors.toList());

            WorkerDetailVO.DailyGroup dg = new WorkerDetailVO.DailyGroup();
            dg.setDate(entry.getKey());
            dg.setQty(dayReports.stream().mapToInt(r -> r.getQty() != null ? r.getQty() : 0).sum());
            dg.setAmount(dayReports.stream().map(WorkReport::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            dg.setRecords(records);
            dailyReports.add(dg);
        }

        WorkerDetailVO result = new WorkerDetailVO();
        WorkerDetailVO.EmployeeInfo empInfo = new WorkerDetailVO.EmployeeInfo();
        empInfo.setId(emp.getId());
        empInfo.setEmployeeNo(emp.getEmployeeNo());
        empInfo.setEmployeeName(emp.getName());
        result.setEmployee(empInfo);
        result.setTotalQty(totalQty);
        result.setTotalAmount(totalAmount);
        result.setByOrder(byOrder);
        result.setByProcess(byProcess);
        result.setDailyReports(dailyReports);
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

    public void supplementAnomaly(Long id, String employeeNo, Integer qty) {
        Long factoryId = TenantContext.get();
        Employee emp = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getEmployeeNo, employeeNo)
                        .eq(Employee::getFactoryId, factoryId)
        );
        if (emp == null) {
            throw new BizException("工号不存在：" + employeeNo);
        }
        anomalyService.supplement(id, emp.getId(), qty);
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

        Employee emp = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getEmployeeNo, req.getEmployeeNo())
                        .eq(Employee::getFactoryId, factoryId)
        );
        if (emp == null) {
            throw new BizException("工号不存在：" + req.getEmployeeNo());
        }

        TimedWage wage = new TimedWage();
        wage.setFactoryId(factoryId);
        wage.setEmployeeId(emp.getId());
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

        Long factoryId = TenantContext.get();
        Employee emp = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getEmployeeNo, req.getEmployeeNo())
                        .eq(Employee::getFactoryId, factoryId)
        );
        if (emp == null) {
            throw new BizException("工号不存在：" + req.getEmployeeNo());
        }
        wage.setEmployeeId(emp.getId());
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

    // ========== Employee management ==========

    public EmployeesPageVO getEmployees(String keyword, int page, int size) {
        Long factoryId = TenantContext.get();
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<Employee>()
                .eq(Employee::getFactoryId, factoryId)
                .and(w -> w.eq(Employee::getStatus, 1).or().eq(Employee::getStatus, 0));
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                    .like(Employee::getName, kw)
                    .or().like(Employee::getEmployeeNo, kw)
                    .or().like(Employee::getPhone, kw)
            );
        }
        wrapper.orderByDesc(Employee::getCreatedAt);

        // 分页查询
        long total = employeeMapper.selectCount(wrapper);
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + ", " + size);
        List<Employee> employees = employeeMapper.selectList(wrapper);

        // Load roles for name lookup
        Set<Long> roleIds = employees.stream()
                .map(Employee::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> roleNameMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            for (Role r : roles) {
                roleNameMap.put(r.getId(), r.getName());
            }
        }

        List<EmployeeMobileVO> records = employees.stream().map(emp -> {
            EmployeeMobileVO vo = new EmployeeMobileVO();
            vo.setId(emp.getId());
            vo.setEmployeeNo(emp.getEmployeeNo());
            vo.setName(emp.getName());
            vo.setPhone(emp.getPhone());
            vo.setRoleId(emp.getRoleId());
            vo.setRoleName(roleNameMap.get(emp.getRoleId()));
            vo.setLoginAccount(emp.getLoginAccount());
            vo.setStatus(emp.getStatus());
            vo.setRemark(emp.getRemark());
            return vo;
        }).collect(Collectors.toList());

        EmployeesPageVO result = new EmployeesPageVO();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setHasMore(offset + records.size() < total);
        return result;
    }

    public List<Role> getRoles() {
        Long factoryId = TenantContext.get();
        return roleMapper.selectList(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getFactoryId, factoryId)
                        .orderByAsc(Role::getSort)
        );
    }

    @Transactional
    public void createEmployee(EmployeeRequest req) {
        Long factoryId = TenantContext.get();
        Employee employee = new Employee();
        employee.setFactoryId(factoryId);
        employee.setName(req.getName());
        employee.setPhone(req.getPhone());
        employee.setRoleId(req.getRoleId());
        employee.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        employee.setRemark(req.getRemark());

        // Generate employee number
        employee.setEmployeeNo(employeeNoGenerator.generate(factoryId));
        // Login account defaults to employeeNo
        employee.setLoginAccount(req.getLoginAccount() != null && !req.getLoginAccount().isEmpty()
                ? req.getLoginAccount() : employee.getEmployeeNo());
        // Default password
        employee.setLoginPassword(BCrypt.hashpw(
                req.getLoginPassword() != null && !req.getLoginPassword().isEmpty()
                        ? req.getLoginPassword() : "123456"));

        employeeMapper.insert(employee);
    }

    @Transactional
    public void updateEmployee(Long id, EmployeeRequest req) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        employee.setName(req.getName());
        employee.setPhone(req.getPhone());
        employee.setRoleId(req.getRoleId());
        if (req.getStatus() != null) {
            employee.setStatus(req.getStatus());
        }
        if (req.getLoginAccount() != null && !req.getLoginAccount().isEmpty()) {
            employee.setLoginAccount(req.getLoginAccount());
        }
        employee.setRemark(req.getRemark());
        employeeMapper.updateById(employee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employeeMapper.deleteById(id);
    }

    @Transactional
    public void toggleEmployeeStatus(Long id, Integer status) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        employee.setStatus(status);
        employeeMapper.updateById(employee);
    }

    // ========== Report management ==========

    public List<AdminReportVO> getReports(String keyword) {
        Long factoryId = TenantContext.get();
        LambdaQueryWrapper<WorkReport> wrapper = new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, factoryId)
                .eq(WorkReport::getStatus, 1)
                .orderByDesc(WorkReport::getReportTime)
                .last("LIMIT 200");
        List<WorkReport> reports = workReportMapper.selectList(wrapper);

        return reports.stream().map(r -> {
            AdminReportVO vo = new AdminReportVO();
            vo.setId(r.getId());
            vo.setEmployeeId(r.getEmployeeId());
            vo.setQty(r.getQty());
            vo.setPrice(r.getPrice());
            vo.setAmount(r.getAmount());
            vo.setReportType(r.getReportType());
            vo.setStatus(r.getStatus());
            vo.setReportTime(r.getReportTime() != null ? r.getReportTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
            vo.setClearReason(r.getClearReason());
            vo.setSupplementFlag(r.getSupplementFlag());

            if (r.getEmployeeId() != null) {
                Employee emp = employeeMapper.selectById(r.getEmployeeId());
                if (emp != null) {
                    vo.setEmployeeName(emp.getName());
                    vo.setEmployeeNo(emp.getEmployeeNo());
                }
            }
            if (r.getOrderId() != null) {
                Order order = orderMapper.selectById(r.getOrderId());
                vo.setOrderNo(order != null ? order.getOrderNo() : null);
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
        }).collect(Collectors.toList());
    }

    @Transactional
    public void clearReport(Long reportId, String reason) {
        WorkReport report = workReportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException("报工记录不存在");
        }
        if (report.getStatus() != 1) {
            throw new BizException("该报工记录已作废");
        }
        report.setStatus(0);
        report.setClearReason(reason);
        workReportMapper.updateById(report);
    }

    @Transactional
    public void supplementReport(String employeeNo, Long bundleId, Long processId, Integer qty) {
        Long factoryId = TenantContext.get();

        // 按工号查找员工
        Employee emp = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getEmployeeNo, employeeNo)
                        .eq(Employee::getFactoryId, factoryId)
        );
        if (emp == null) {
            throw new BizException("工号不存在：" + employeeNo);
        }
        Long employeeId = emp.getId();

        Bundle bundle = bundleMapper.selectById(bundleId);
        if (bundle == null) {
            throw new BizException("扎号不存在");
        }

        Process process = processMapper.selectById(processId);
        if (process == null) {
            throw new BizException("工序不存在");
        }

        // 查找订单工序定价
        BigDecimal price = BigDecimal.ZERO;
        if (bundle.getOrderId() != null) {
            OrderProcess op = orderProcessMapper.selectOne(
                    new LambdaQueryWrapper<OrderProcess>()
                            .eq(OrderProcess::getOrderId, bundle.getOrderId())
                            .eq(OrderProcess::getProcessId, processId)
            );
            if (op != null && op.getProcessPrice() != null) {
                price = op.getProcessPrice();
            }
        }

        BigDecimal amount = price.multiply(BigDecimal.valueOf(qty));

        WorkReport report = new WorkReport();
        report.setFactoryId(factoryId);
        report.setBundleId(bundleId);
        report.setOrderId(bundle.getOrderId());
        report.setProcessId(processId);
        report.setEmployeeId(employeeId);
        report.setQty(qty);
        report.setPrice(price);
        report.setAmount(amount);
        report.setReportType(3); // 管理员补录
        report.setStatus(1);
        report.setReportTime(LocalDateTime.now());
        report.setSupplementFlag(1);
        report.setCreatedBy(StpUtil.getLoginIdAsLong());
        workReportMapper.insert(report);
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

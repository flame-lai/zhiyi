package com.zhiyi.erp.module.admin.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.exception.BizException;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.common.util.XlsxExportUtil;
import com.zhiyi.erp.module.admin.dto.WageQueryDTO;
import com.zhiyi.erp.module.admin.entity.WageOther;
import com.zhiyi.erp.module.admin.entity.WageSummary;
import com.zhiyi.erp.module.admin.entity.TimedWage;
import com.zhiyi.erp.module.admin.mapper.TimedWageMapper;
import com.zhiyi.erp.module.admin.mapper.WageOtherMapper;
import com.zhiyi.erp.module.admin.mapper.WageSummaryMapper;
import com.zhiyi.erp.module.admin.vo.WageDetailVO;
import com.zhiyi.erp.module.admin.vo.WagePieceDetailVO;
import com.zhiyi.erp.module.admin.vo.WageSummaryVO;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import com.zhiyi.erp.module.system.entity.Employee;
import com.zhiyi.erp.module.system.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WageManageService extends ServiceImpl<WageSummaryMapper, WageSummary> {

    @Autowired
    private WageOtherMapper wageOtherMapper;
    @Autowired
    private WorkReportMapper workReportMapper;
    @Autowired
    private TimedWageMapper timedWageMapper;
    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private TimedWageService timedWageService;

    public IPage<WageSummaryVO> summaryPage(Page<WageSummaryVO> page, WageQueryDTO query) {
        List<WageSummaryVO> all = buildSummaries(query);
        long total = all.size();
        int from = (int) Math.min((page.getCurrent() - 1) * page.getSize(), total);
        int to = (int) Math.min(from + page.getSize(), total);
        page.setRecords(all.subList(from, to));
        page.setTotal(total);
        return page;
    }

    public void exportExcel(WageQueryDTO query, OutputStream outputStream) throws IOException {
        String yearMonth = normalizeYearMonth(query.getYearMonth());
        List<WageSummaryVO> summaries = buildSummaries(query);
        Set<Long> exportEmployeeIds = summaries.stream()
                .map(WageSummaryVO::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, List<WagePieceDetailVO>> pieceDetailMap = selectPieceDetailsForExport(yearMonth, query.getEmployeeId()).stream()
                .filter(item -> exportEmployeeIds.contains(item.getEmployeeId()))
                .collect(Collectors.groupingBy(WagePieceDetailVO::getEmployeeId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<com.zhiyi.erp.module.admin.vo.TimedWageVO>> timedDetailMap = timedWageService.buildVOList(selectTimedWages(yearMonth, query.getEmployeeId())).stream()
                .filter(item -> exportEmployeeIds.contains(item.getEmployeeId()))
                .collect(Collectors.groupingBy(com.zhiyi.erp.module.admin.vo.TimedWageVO::getEmployeeId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<WageOther>> otherItemMap = selectOtherItems(yearMonth, query.getEmployeeId()).stream()
                .filter(item -> exportEmployeeIds.contains(item.getEmployeeId()))
                .collect(Collectors.groupingBy(WageOther::getEmployeeId, LinkedHashMap::new, Collectors.toList()));

        List<List<Object>> summaryRows = new ArrayList<>();
        List<List<Object>> pieceRows = new ArrayList<>();
        List<List<Object>> timedRows = new ArrayList<>();
        List<List<Object>> otherRows = new ArrayList<>();

        summaryRows.add(row("工号", "姓名", "月份", "计件件数", "计件工资", "计时工时", "计时工资", "其他项目合计", "应发合计", "状态"));
        pieceRows.add(row("工号", "姓名", "月份", "日期", "订单号", "扎号", "工序", "件数", "工价", "金额"));
        timedRows.add(row("工号", "姓名", "月份", "日期", "工时", "时薪", "金额", "工作内容", "录入时间", "录入人"));
        otherRows.add(row("工号", "姓名", "月份", "项目名称", "金额", "备注", "录入时间"));

        for (WageSummaryVO summary : summaries) {
            String compactMonth = compactYearMonth(summary.getYearMonth());
            summaryRows.add(row(
                    summary.getEmployeeNo(),
                    summary.getEmployeeName(),
                    compactMonth,
                    summary.getPieceQty(),
                    summary.getPieceWage(),
                    summary.getTimedHours(),
                    summary.getTimedWage(),
                    summary.getOtherWage(),
                    summary.getTotalWage(),
                    statusText(summary.getStatus())
            ));

            for (WagePieceDetailVO item : pieceDetailMap.getOrDefault(summary.getEmployeeId(), List.of())) {
                pieceRows.add(row(
                        summary.getEmployeeNo(),
                        summary.getEmployeeName(),
                        compactMonth,
                        datePart(item.getReportTime()),
                        item.getOrderNo(),
                        item.getBundleNo(),
                        item.getProcessName(),
                        item.getQty(),
                        item.getPrice(),
                        item.getAmount()
                ));
            }
            for (var item : timedDetailMap.getOrDefault(summary.getEmployeeId(), List.of())) {
                timedRows.add(row(
                        summary.getEmployeeNo(),
                        summary.getEmployeeName(),
                        compactMonth,
                        item.getWorkDate(),
                        item.getHours(),
                        item.getHourlyRate(),
                        item.getAmount(),
                        item.getWorkContent(),
                        item.getCreatedAt(),
                        item.getCreatedByName()
                ));
            }
            for (WageOther item : otherItemMap.getOrDefault(summary.getEmployeeId(), List.of())) {
                otherRows.add(row(
                        summary.getEmployeeNo(),
                        summary.getEmployeeName(),
                        compactMonth,
                        item.getItemName(),
                        item.getAmount(),
                        item.getRemark(),
                        item.getCreatedAt()
                ));
            }
        }

        XlsxExportUtil.write(outputStream, List.of(
                new XlsxExportUtil.Sheet("工资汇总", summaryRows),
                new XlsxExportUtil.Sheet("计件明细", pieceRows),
                new XlsxExportUtil.Sheet("计时明细", timedRows),
                new XlsxExportUtil.Sheet("其他项目明细", otherRows)
        ));
    }

    private List<WagePieceDetailVO> selectPieceDetailsForExport(String yearMonth, Long employeeId) {
        YearMonth ym = YearMonth.parse(yearMonth);
        return workReportMapper.selectPieceDetailsForWageExport(
                TenantContext.get(),
                ym.atDay(1).atStartOfDay(),
                ym.plusMonths(1).atDay(1).atStartOfDay(),
                employeeId
        );
    }

    private List<WageSummaryVO> buildSummaries(WageQueryDTO query) {
        String yearMonth = normalizeYearMonth(query.getYearMonth());
        List<WorkReport> reports = selectPieceReports(yearMonth, query.getEmployeeId());
        List<TimedWage> timedWages = selectTimedWages(yearMonth, query.getEmployeeId());
        List<WageOther> otherItems = selectOtherItems(yearMonth, query.getEmployeeId());
        List<WageSummary> summaries = selectSummaries(yearMonth, query.getEmployeeId());

        Set<Long> employeeIds = new java.util.HashSet<>();
        reports.stream().map(WorkReport::getEmployeeId).filter(Objects::nonNull).forEach(employeeIds::add);
        timedWages.stream().map(TimedWage::getEmployeeId).filter(Objects::nonNull).forEach(employeeIds::add);
        otherItems.stream().map(WageOther::getEmployeeId).filter(Objects::nonNull).forEach(employeeIds::add);
        summaries.stream().map(WageSummary::getEmployeeId).filter(Objects::nonNull).forEach(employeeIds::add);

        Map<Long, Employee> employeeMap = employeeIds.isEmpty()
                ? Map.of()
                : employeeMapper.selectBatchIds(employeeIds).stream()
                .filter(e -> Objects.equals(e.getFactoryId(), TenantContext.get()))
                .collect(Collectors.toMap(Employee::getId, e -> e));

        Map<Long, WageSummaryVO> summaryMap = new LinkedHashMap<>();
        for (Long employeeId : employeeIds) {
            Employee employee = employeeMap.get(employeeId);
            WageSummaryVO vo = new WageSummaryVO();
            vo.setEmployeeId(employeeId);
            vo.setEmployeeNo(employee != null ? employee.getEmployeeNo() : null);
            vo.setEmployeeName(employee != null ? employee.getName() : null);
            vo.setYearMonth(yearMonth);
            vo.setPieceQty(0);
            vo.setPieceWage(BigDecimal.ZERO);
            vo.setTimedHours(BigDecimal.ZERO);
            vo.setTimedWage(BigDecimal.ZERO);
            vo.setOtherWage(BigDecimal.ZERO);
            vo.setTotalWage(BigDecimal.ZERO);
            vo.setStatus(0);
            summaryMap.put(employeeId, vo);
        }

        reports.forEach(report -> {
            WageSummaryVO vo = summaryMap.get(report.getEmployeeId());
            if (vo != null) {
                vo.setPieceQty(vo.getPieceQty() + nullToZero(report.getQty()));
                vo.setPieceWage(vo.getPieceWage().add(nullToZero(report.getAmount())));
            }
        });
        timedWages.forEach(wage -> {
            WageSummaryVO vo = summaryMap.get(wage.getEmployeeId());
            if (vo != null) {
                vo.setTimedHours(vo.getTimedHours().add(nullToZero(wage.getHours())));
                vo.setTimedWage(vo.getTimedWage().add(nullToZero(wage.getAmount())));
            }
        });
        otherItems.forEach(item -> {
            WageSummaryVO vo = summaryMap.get(item.getEmployeeId());
            if (vo != null) {
                vo.setOtherWage(vo.getOtherWage().add(nullToZero(item.getAmount())));
            }
        });

        Map<Long, WageSummary> summaryStatusMap = summaries.stream()
                .collect(Collectors.toMap(WageSummary::getEmployeeId, s -> s, (a, b) -> a));
        summaryMap.values().forEach(vo -> {
            WageSummary persisted = summaryStatusMap.get(vo.getEmployeeId());
            if (persisted != null) {
                vo.setSummaryId(persisted.getId());
                vo.setStatus(persisted.getStatus());
            }
            vo.setTotalWage(vo.getPieceWage().add(vo.getTimedWage()).add(vo.getOtherWage()));
        });

        List<WageSummaryVO> all = summaryMap.values().stream()
                .filter(vo -> query.getStatus() == null || Objects.equals(vo.getStatus(), query.getStatus()))
                .filter(vo -> filterByWageType(vo, query.getWageType()))
                .sorted(Comparator.comparing(WageSummaryVO::getEmployeeNo, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        return all;
    }

    @Transactional
    public void audit(Long employeeId, String yearMonth) {
        WageSummary summary = getOrCreateSummary(employeeId, normalizeYearMonth(yearMonth));
        if (Objects.equals(summary.getStatus(), 2)) {
            throw new BizException("已发放工资不可重新审核");
        }
        summary.setStatus(1);
        updateById(summary);
    }

    @Transactional
    public void markPaid(Long employeeId, String yearMonth) {
        WageSummary summary = getOrCreateSummary(employeeId, normalizeYearMonth(yearMonth));
        if (!Objects.equals(summary.getStatus(), 1)) {
            throw new BizException("只有已审核工资才能标记发放");
        }
        summary.setStatus(2);
        updateById(summary);
    }

    @Transactional
    public void auditBySummaryId(Long id) {
        WageSummary summary = getTenantSummary(id);
        audit(summary.getEmployeeId(), summary.getYearMonth());
    }

    @Transactional
    public void markPaidBySummaryId(Long id) {
        WageSummary summary = getTenantSummary(id);
        markPaid(summary.getEmployeeId(), summary.getYearMonth());
    }

    public WageDetailVO detail(Long employeeId, String yearMonth) {
        String ym = normalizeYearMonth(yearMonth);
        WageDetailVO detail = new WageDetailVO();
        detail.setPieceDetails(selectPieceReports(ym, employeeId).stream()
                .map(this::buildPieceDetail)
                .collect(Collectors.toList()));
        detail.setTimedDetails(timedWageService.buildVOList(selectTimedWages(ym, employeeId)));
        detail.setOtherItems(selectOtherItems(ym, employeeId));
        return detail;
    }

    public List<WageOther> getOtherItems(Long employeeId, String yearMonth) {
        return selectOtherItems(normalizeYearMonth(yearMonth), employeeId);
    }

    @Transactional
    public void addOtherItem(WageOther item) {
        assertNotPaid(item.getEmployeeId(), item.getYearMonth());
        item.setFactoryId(TenantContext.get());
        item.setYearMonth(normalizeYearMonth(item.getYearMonth()));
        item.setCreatedBy(StpUtil.getLoginIdAsLong());
        validateEmployee(item.getEmployeeId());
        if (item.getItemName() == null || item.getItemName().isBlank()) {
            throw new BizException("请填写项目名称");
        }
        if (item.getAmount() == null) {
            throw new BizException("请填写金额");
        }
        wageOtherMapper.insert(item);
    }

    @Transactional
    public void updateOtherItem(Long id, WageOther item) {
        WageOther existing = getTenantOtherItem(id);
        assertNotPaid(existing.getEmployeeId(), existing.getYearMonth());
        existing.setItemName(item.getItemName());
        existing.setAmount(item.getAmount());
        existing.setRemark(item.getRemark());
        if (existing.getItemName() == null || existing.getItemName().isBlank()) {
            throw new BizException("请填写项目名称");
        }
        if (existing.getAmount() == null) {
            throw new BizException("请填写金额");
        }
        wageOtherMapper.updateById(existing);
    }

    @Transactional
    public void deleteOtherItem(Long id) {
        WageOther existing = getTenantOtherItem(id);
        assertNotPaid(existing.getEmployeeId(), existing.getYearMonth());
        wageOtherMapper.deleteById(id);
    }

    private List<WorkReport> selectPieceReports(String yearMonth, Long employeeId) {
        LambdaQueryWrapper<WorkReport> wrapper = new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getFactoryId, TenantContext.get())
                .eq(WorkReport::getStatus, 1)
                .ge(WorkReport::getReportTime, YearMonth.parse(yearMonth).atDay(1).atStartOfDay())
                .lt(WorkReport::getReportTime, YearMonth.parse(yearMonth).plusMonths(1).atDay(1).atStartOfDay());
        if (employeeId != null) {
            wrapper.eq(WorkReport::getEmployeeId, employeeId);
        }
        return workReportMapper.selectList(wrapper);
    }

    private List<TimedWage> selectTimedWages(String yearMonth, Long employeeId) {
        LocalDate start = YearMonth.parse(yearMonth).atDay(1);
        LocalDate end = YearMonth.parse(yearMonth).atEndOfMonth();
        LambdaQueryWrapper<TimedWage> wrapper = new LambdaQueryWrapper<TimedWage>()
                .eq(TimedWage::getFactoryId, TenantContext.get())
                .ge(TimedWage::getWorkDate, start)
                .le(TimedWage::getWorkDate, end)
                .orderByDesc(TimedWage::getWorkDate);
        if (employeeId != null) {
            wrapper.eq(TimedWage::getEmployeeId, employeeId);
        }
        return timedWageMapper.selectList(wrapper);
    }

    private List<WageOther> selectOtherItems(String yearMonth, Long employeeId) {
        LambdaQueryWrapper<WageOther> wrapper = new LambdaQueryWrapper<WageOther>()
                .eq(WageOther::getFactoryId, TenantContext.get())
                .eq(WageOther::getYearMonth, yearMonth)
                .orderByDesc(WageOther::getCreatedAt);
        if (employeeId != null) {
            wrapper.eq(WageOther::getEmployeeId, employeeId);
        }
        return wageOtherMapper.selectList(wrapper);
    }

    private List<WageSummary> selectSummaries(String yearMonth, Long employeeId) {
        LambdaQueryWrapper<WageSummary> wrapper = new LambdaQueryWrapper<WageSummary>()
                .eq(WageSummary::getFactoryId, TenantContext.get())
                .eq(WageSummary::getYearMonth, yearMonth);
        if (employeeId != null) {
            wrapper.eq(WageSummary::getEmployeeId, employeeId);
        }
        return list(wrapper);
    }

    private WageSummary getOrCreateSummary(Long employeeId, String yearMonth) {
        validateEmployee(employeeId);
        WageSummary summary = getOne(new LambdaQueryWrapper<WageSummary>()
                .eq(WageSummary::getFactoryId, TenantContext.get())
                .eq(WageSummary::getEmployeeId, employeeId)
                .eq(WageSummary::getYearMonth, yearMonth), false);
        if (summary != null) {
            return summary;
        }
        summary = new WageSummary();
        summary.setFactoryId(TenantContext.get());
        summary.setEmployeeId(employeeId);
        summary.setYearMonth(yearMonth);
        summary.setPieceWage(BigDecimal.ZERO);
        summary.setTimedWage(BigDecimal.ZERO);
        summary.setOtherWage(BigDecimal.ZERO);
        summary.setTotalWage(BigDecimal.ZERO);
        summary.setStatus(0);
        save(summary);
        return summary;
    }

    private WagePieceDetailVO buildPieceDetail(WorkReport report) {
        WagePieceDetailVO vo = new WagePieceDetailVO();
        vo.setId(report.getId());
        vo.setReportTime(report.getReportTime());
        vo.setQty(report.getQty());
        vo.setPrice(report.getPrice());
        vo.setAmount(report.getAmount());
        Map<String, Object> detail = workReportMapper.selectPieceDetailById(report.getId());
        if (detail != null) {
            vo.setOrderNo((String) detail.get("orderNo"));
            vo.setBundleNo((String) detail.get("bundleNo"));
            vo.setProcessName((String) detail.get("processName"));
        }
        return vo;
    }

    private WageOther getTenantOtherItem(Long id) {
        WageOther existing = wageOtherMapper.selectById(id);
        if (existing == null || !Objects.equals(existing.getFactoryId(), TenantContext.get())) {
            throw new BizException("其他工资项目不存在");
        }
        return existing;
    }

    private WageSummary getTenantSummary(Long id) {
        WageSummary summary = getById(id);
        if (summary == null || !Objects.equals(summary.getFactoryId(), TenantContext.get())) {
            throw new BizException("工资汇总不存在");
        }
        return summary;
    }

    private void assertNotPaid(Long employeeId, String yearMonth) {
        WageSummary summary = getOne(new LambdaQueryWrapper<WageSummary>()
                .eq(WageSummary::getFactoryId, TenantContext.get())
                .eq(WageSummary::getEmployeeId, employeeId)
                .eq(WageSummary::getYearMonth, normalizeYearMonth(yearMonth)), false);
        if (summary != null && Objects.equals(summary.getStatus(), 2)) {
            throw new BizException("已发放工资不可修改");
        }
    }

    private void validateEmployee(Long employeeId) {
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null || !Objects.equals(employee.getFactoryId(), TenantContext.get())) {
            throw new BizException("工人不存在");
        }
    }

    private boolean filterByWageType(WageSummaryVO vo, String wageType) {
        if (wageType == null || wageType.isBlank() || "ALL".equalsIgnoreCase(wageType)) {
            return true;
        }
        return switch (wageType.toUpperCase()) {
            case "PIECE" -> vo.getPieceWage().compareTo(BigDecimal.ZERO) != 0;
            case "TIMED" -> vo.getTimedWage().compareTo(BigDecimal.ZERO) != 0;
            case "OTHER" -> vo.getOtherWage().compareTo(BigDecimal.ZERO) != 0;
            default -> true;
        };
    }

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private String compactYearMonth(String yearMonth) {
        return yearMonth == null ? "" : yearMonth.replace("-", "");
    }

    private Object datePart(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.toLocalDate();
    }

    private String statusText(Integer status) {
        if (Objects.equals(status, 1)) {
            return "已审核";
        }
        if (Objects.equals(status, 2)) {
            return "已发放";
        }
        return "待审核";
    }

    private String normalizeYearMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return YearMonth.now().toString();
        }
        return yearMonth;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

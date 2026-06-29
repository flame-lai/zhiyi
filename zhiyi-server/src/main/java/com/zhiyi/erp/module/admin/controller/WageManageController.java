package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.dto.WageQueryDTO;
import com.zhiyi.erp.module.admin.entity.WageOther;
import com.zhiyi.erp.module.admin.service.WageManageService;
import com.zhiyi.erp.module.admin.vo.WageDetailVO;
import com.zhiyi.erp.module.admin.vo.WageSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/admin/wages")
@Tag(name = "工资管理", description = "工资汇总、审核、其他项目管理")
public class WageManageController {
    @Autowired
    private WageManageService wageManageService;

    @Operation(summary = "工资汇总列表")
    @GetMapping
    public R<IPage<WageSummaryVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "年月，格式yyyy-MM") @RequestParam(required = false) String yearMonth,
            @Parameter(description = "工人ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "工资类型") @RequestParam(required = false) String wageType,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer status) {
        WageQueryDTO query = new WageQueryDTO();
        query.setYearMonth(yearMonth);
        query.setEmployeeId(employeeId);
        query.setWageType(wageType);
        query.setStatus(status);
        return R.ok(wageManageService.summaryPage(new Page<>(page, pageSize != null ? pageSize : size), query));
    }

    @Operation(summary = "导出工资Excel")
    @GetMapping("/export")
    public void export(
            @Parameter(description = "年月，格式yyyy-MM") @RequestParam(required = false) String yearMonth,
            @Parameter(description = "工人ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "工资类型") @RequestParam(required = false) String wageType,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        WageQueryDTO query = new WageQueryDTO();
        query.setYearMonth(yearMonth);
        query.setEmployeeId(employeeId);
        query.setWageType(wageType);
        query.setStatus(status);

        String exportMonth = (yearMonth == null || yearMonth.isBlank() ? YearMonth.now().toString() : yearMonth).replace("-", "");
        String filename = URLEncoder.encode("工资汇总-" + exportMonth + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        wageManageService.exportExcel(query, response.getOutputStream());
    }

    @Operation(summary = "审核工资")
    @PostMapping("/audit")
    public R<Void> audit(@Parameter(description = "工人ID") @RequestParam Long employeeId,
                         @Parameter(description = "年月，格式yyyy-MM") @RequestParam String yearMonth) {
        wageManageService.audit(employeeId, yearMonth);
        return R.ok();
    }

    @Operation(summary = "标记已发放")
    @PostMapping("/mark-paid")
    public R<Void> markPaid(@Parameter(description = "工人ID") @RequestParam Long employeeId,
                            @Parameter(description = "年月，格式yyyy-MM") @RequestParam String yearMonth) {
        wageManageService.markPaid(employeeId, yearMonth);
        return R.ok();
    }

    @Operation(summary = "工资明细")
    @GetMapping("/detail")
    public R<WageDetailVO> detail(
            @Parameter(description = "工人ID") @RequestParam Long employeeId,
            @Parameter(description = "年月，格式yyyy-MM") @RequestParam String yearMonth) {
        return R.ok(wageManageService.detail(employeeId, yearMonth));
    }

    @Deprecated
    @Operation(summary = "审核工资（兼容旧接口）")
    @PostMapping("/{id}/audit")
    public R<Void> auditById(@Parameter(description = "工资汇总ID") @PathVariable Long id) {
        wageManageService.auditBySummaryId(id);
        return R.ok();
    }

    @Deprecated
    @Operation(summary = "标记已发放（兼容旧接口）")
    @PostMapping("/{id}/mark-paid")
    public R<Void> markPaidById(@Parameter(description = "工资汇总ID") @PathVariable Long id) {
        wageManageService.markPaidBySummaryId(id);
        return R.ok();
    }

    @Operation(summary = "查询其他工资项目")
    @GetMapping("/other-items")
    public R<List<WageOther>> getOtherItems(
            @Parameter(description = "工人ID") @RequestParam Long employeeId, @Parameter(description = "年月，格式yyyy-MM") @RequestParam String yearMonth) {
        return R.ok(wageManageService.getOtherItems(employeeId, yearMonth));
    }

    @Operation(summary = "新增其他工资项目")
    @PostMapping("/other-items")
    public R<Void> addOtherItem(@RequestBody WageOther item) {
        wageManageService.addOtherItem(item);
        return R.ok();
    }

    @Operation(summary = "编辑其他工资项目")
    @PutMapping("/other-items/{id}")
    public R<Void> updateOtherItem(@PathVariable Long id, @RequestBody WageOther item) {
        wageManageService.updateOtherItem(id, item);
        return R.ok();
    }

    @Operation(summary = "删除其他工资项目")
    @DeleteMapping("/other-items/{id}")
    public R<Void> deleteOtherItem(@PathVariable Long id) {
        wageManageService.deleteOtherItem(id);
        return R.ok();
    }
}

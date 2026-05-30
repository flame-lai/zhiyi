package com.zhiyi.erp.module.report.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report/wage")
@Tag(name = "工资查询", description = "工人查工资（今日/本月汇总+明细）")
public class WageController {
    @Autowired
    private ReportService reportService;

    @Operation(summary = "查询工资汇总")
    @GetMapping("/summary")
    public R<List<Map<String, Object>>> getSummary(
            @Parameter(description = "年月，格式yyyy-MM") @RequestParam(required = false) String yearMonth) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        if (yearMonth == null) {
            yearMonth = LocalDate.now().toString().substring(0, 7);
        }
        return R.ok(reportService.getEmployeeWageSummary(employeeId, yearMonth));
    }
}

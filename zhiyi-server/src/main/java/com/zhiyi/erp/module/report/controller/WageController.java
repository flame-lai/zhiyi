package com.zhiyi.erp.module.report.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report/wage")
public class WageController {
    @Autowired
    private ReportService reportService;

    @GetMapping("/summary")
    public R<List<Map<String, Object>>> getSummary(
            @RequestParam(required = false) String yearMonth) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        if (yearMonth == null) {
            yearMonth = LocalDate.now().toString().substring(0, 7);
        }
        return R.ok(reportService.getEmployeeWageSummary(employeeId, yearMonth));
    }
}

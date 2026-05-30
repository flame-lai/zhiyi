package com.zhiyi.erp.module.report.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.report.service.ReportService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ScanReportController {
    @Autowired
    private ReportService reportService;

    @Data
    public static class BundleReportDTO {
        private Long bundleId;
        private List<Long> processIds;
    }

    @Data
    public static class CuttingReportDTO {
        private String barcode;
    }

    @PostMapping("/bundle")
    public R<Map<String, Object>> reportByBundle(@RequestBody BundleReportDTO dto) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(reportService.reportByBundle(employeeId, dto.getBundleId(), dto.getProcessIds()));
    }

    @PostMapping("/cutting")
    public R<Map<String, Object>> reportByCutting(@RequestBody CuttingReportDTO dto) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(reportService.reportByCuttingBarcode(employeeId, dto.getBarcode()));
    }
}

package com.zhiyi.erp.module.report.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@Tag(name = "扫码报工", description = "扫码报工（扎号QR码+裁床条形码）")
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

    @Operation(summary = "扎号扫码报工")
    @PostMapping("/bundle")
    public R<Map<String, Object>> reportByBundle(@RequestBody BundleReportDTO dto) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(reportService.reportByBundle(employeeId, dto.getBundleId(), dto.getProcessIds()));
    }

    @Operation(summary = "裁床条形码扫码报工")
    @PostMapping("/cutting")
    public R<Map<String, Object>> reportByCutting(@RequestBody CuttingReportDTO dto) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(reportService.reportByCuttingBarcode(employeeId, dto.getBarcode()));
    }
}

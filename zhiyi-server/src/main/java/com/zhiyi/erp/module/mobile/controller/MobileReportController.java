package com.zhiyi.erp.module.mobile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.mobile.dto.ReportSubmitRequest;
import com.zhiyi.erp.module.mobile.dto.ScanResolveRequest;
import com.zhiyi.erp.module.mobile.service.MobileService;
import com.zhiyi.erp.module.mobile.vo.ReportSubmitResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "移动端报工", description = "扫码解析、确认报工、工资、报工记录")
@RestController
@RequestMapping("/api/mobile/report")
public class MobileReportController {

    @Autowired
    private MobileService mobileService;

    @Operation(summary = "扫码解析")
    @PostMapping("/scan/resolve")
    public R<Object> resolveScan(@Valid @RequestBody ScanResolveRequest req) {
        return R.ok(mobileService.resolveScan(req.getCode()));
    }

    @Operation(summary = "确认上报")
    @PostMapping("/submit")
    public R<ReportSubmitResultVO> submitReport(@Valid @RequestBody ReportSubmitRequest req) {
        Long employeeId = StpUtil.getSession().getLong("employeeId");
        return R.ok(mobileService.submitReport(employeeId, req));
    }
}

package com.zhiyi.erp.module.admin.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "生产看板", description = "生产看板（订单维度+工人维度）")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @Operation(summary = "订单维度看板")
    @GetMapping("/order")
    public R<List<Map<String, Object>>> orderDashboard(
            @Parameter(description = "订单ID，不传则查全部") @RequestParam(required = false) Long orderId) {
        return R.ok(dashboardService.getOrderDashboard(orderId));
    }

    @Operation(summary = "工人维度看板")
    @GetMapping("/worker")
    public R<List<Map<String, Object>>> workerDashboard(
            @Parameter(description = "日期，格式yyyy-MM-dd，默认今天") @RequestParam(required = false) String date) {
        if (date == null) date = LocalDate.now().toString();
        return R.ok(dashboardService.getWorkerDashboard(date));
    }
}

package com.zhiyi.erp.module.admin.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/order")
    public R<List<Map<String, Object>>> orderDashboard(
            @RequestParam(required = false) Long orderId) {
        return R.ok(dashboardService.getOrderDashboard(orderId));
    }

    @GetMapping("/worker")
    public R<List<Map<String, Object>>> workerDashboard(
            @RequestParam(required = false) String date) {
        if (date == null) date = LocalDate.now().toString();
        return R.ok(dashboardService.getWorkerDashboard(date));
    }
}

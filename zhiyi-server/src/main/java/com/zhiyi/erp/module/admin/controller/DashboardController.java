package com.zhiyi.erp.module.admin.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.service.DashboardService;
import com.zhiyi.erp.module.admin.vo.DashboardOrderPageVO;
import com.zhiyi.erp.module.admin.vo.DashboardOrderProgressVO;
import com.zhiyi.erp.module.admin.vo.DashboardOverviewVO;
import com.zhiyi.erp.module.admin.vo.DashboardWorkerCapacityVO;
import com.zhiyi.erp.module.admin.vo.DashboardWorkerPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "生产看板", description = "生产看板（订单维度+工人维度）")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @Operation(summary = "生产看板汇总")
    @GetMapping("/overview")
    public R<DashboardOverviewVO> overview(
            @Parameter(description = "日期，格式yyyy-MM-dd，默认今天") @RequestParam(required = false) String date) {
        return R.ok(dashboardService.getOverview(date));
    }

    @Operation(summary = "订单进度看板")
    @GetMapping("/orders")
    public R<DashboardOrderPageVO> orders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "订单号关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status) {
        return R.ok(dashboardService.getOrders(keyword, status, page, size));
    }

    @Operation(summary = "工人产能看板")
    @GetMapping("/workers")
    public R<DashboardWorkerPageVO> workers(
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "工人工号/姓名关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(dashboardService.getWorkers(startDate, endDate, keyword, page, size));
    }

    @Operation(summary = "订单维度看板")
    @GetMapping("/order")
    public R<List<DashboardOrderProgressVO>> orderDashboard(
            @Parameter(description = "订单ID，不传则查全部") @RequestParam(required = false) Long orderId) {
        return R.ok(dashboardService.getOrderById(orderId));
    }

    @Operation(summary = "工人维度看板")
    @GetMapping("/worker")
    public R<List<DashboardWorkerCapacityVO>> workerDashboard(
            @Parameter(description = "日期，格式yyyy-MM-dd，默认今天") @RequestParam(required = false) String date) {
        if (date == null) date = LocalDate.now().toString();
        DashboardWorkerPageVO page = dashboardService.getWorkers(date, date, null, 1, 100);
        return R.ok(page.getRecords());
    }
}

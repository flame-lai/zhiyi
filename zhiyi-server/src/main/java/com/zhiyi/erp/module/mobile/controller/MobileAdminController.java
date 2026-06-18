package com.zhiyi.erp.module.mobile.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.mobile.dto.*;
import com.zhiyi.erp.module.mobile.service.MobileService;
import com.zhiyi.erp.module.mobile.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "移动端管理", description = "看板、异常、计时、查询")
@RestController
@RequestMapping("/api/mobile/admin")
public class MobileAdminController {

    @Autowired
    private MobileService mobileService;

    @Operation(summary = "订单维度看板")
    @GetMapping("/dashboard/orders")
    public R<List<DashboardOrderVO>> getDashboardOrders(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long orderId) {
        return R.ok(mobileService.getDashboardOrders(date, orderId));
    }

    @Operation(summary = "工人维度看板")
    @GetMapping("/dashboard/workers")
    public R<List<DashboardWorkerVO>> getDashboardWorkers(
            @RequestParam(required = false) String date) {
        if (date == null || date.isEmpty()) date = LocalDate.now().toString();
        return R.ok(mobileService.getDashboardWorkers(date));
    }

    @Operation(summary = "异常列表")
    @GetMapping("/anomalies")
    public R<List<AnomalyVO>> getAnomalies(@RequestParam(required = false) Integer status) {
        return R.ok(mobileService.getAnomalies(status));
    }

    @Operation(summary = "忽略异常")
    @PostMapping("/anomalies/{id}/ignore")
    public R<Void> ignoreAnomaly(@PathVariable Long id, @Valid @RequestBody AnomalyIgnoreRequest req) {
        mobileService.ignoreAnomaly(id, req.getRemark());
        return R.ok();
    }

    @Operation(summary = "清除异常")
    @PostMapping("/anomalies/{id}/clear")
    public R<Void> clearAnomaly(@PathVariable Long id, @Valid @RequestBody AnomalyClearRequest req) {
        mobileService.clearAnomaly(id, req.getReason());
        return R.ok();
    }

    @Operation(summary = "补录异常")
    @PostMapping("/anomalies/{id}/supplement")
    public R<Void> supplementAnomaly(@PathVariable Long id, @Valid @RequestBody AnomalySupplementRequest req) {
        mobileService.supplementAnomaly(id, req.getEmployeeId(), req.getQty());
        return R.ok();
    }

    @Operation(summary = "计时工资列表")
    @GetMapping("/timed-wages")
    public R<List<TimedWageVO>> getTimedWages(@RequestParam(required = false) String yearMonth) {
        return R.ok(mobileService.getTimedWages(yearMonth));
    }

    @Operation(summary = "新增计时工资")
    @PostMapping("/timed-wages")
    public R<Void> createTimedWage(@Valid @RequestBody TimedWageRequest req) {
        mobileService.createTimedWage(req);
        return R.ok();
    }

    @Operation(summary = "编辑计时工资")
    @PutMapping("/timed-wages/{id}")
    public R<Void> updateTimedWage(@PathVariable Long id, @Valid @RequestBody TimedWageRequest req) {
        mobileService.updateTimedWage(id, req);
        return R.ok();
    }

    @Operation(summary = "删除计时工资")
    @DeleteMapping("/timed-wages/{id}")
    public R<Void> deleteTimedWage(@PathVariable Long id) {
        mobileService.deleteTimedWage(id);
        return R.ok();
    }

    @Operation(summary = "扎号查询")
    @GetMapping("/bundles/search")
    public R<BundleDetailVO> searchBundle(@RequestParam String code) {
        return R.ok(mobileService.searchBundle(code));
    }

    @Operation(summary = "订单查询")
    @GetMapping("/orders/search")
    public R<OrderSearchVO> searchOrder(@RequestParam String orderNo) {
        return R.ok(mobileService.searchOrder(orderNo));
    }
}

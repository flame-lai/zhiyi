package com.zhiyi.erp.module.mobile.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.mobile.dto.*;
import com.zhiyi.erp.module.mobile.service.MobileService;
import com.zhiyi.erp.module.mobile.vo.*;
import com.zhiyi.erp.module.system.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "移动端管理", description = "看板、异常、计时、查询")
@RestController
@RequestMapping("/api/mobile/admin")
public class MobileAdminController {

    @Autowired
    private MobileService mobileService;

    @Operation(summary = "订单维度看板")
    @GetMapping("/dashboard/orders")
    public R<DashboardOrderPageVO> getDashboardOrders(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderNo,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return R.ok(mobileService.getDashboardOrders(startDate, endDate, orderNo, page, size));
    }

    @Operation(summary = "工人维度看板")
    @GetMapping("/dashboard/workers")
    public R<DashboardWorkerPageVO> getDashboardWorkers(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return R.ok(mobileService.getDashboardWorkers(startDate, endDate, keyword, page, size));
    }

    @Operation(summary = "工人详情")
    @GetMapping("/dashboard/worker-detail")
    public R<WorkerDetailVO> getWorkerDetail(
            @RequestParam Long employeeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.ok(mobileService.getWorkerDetail(employeeId, startDate, endDate));
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
        mobileService.supplementAnomaly(id, req.getEmployeeNo(), req.getQty());
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

    @Operation(summary = "员工列表")
    @GetMapping("/employees")
    public R<EmployeesPageVO> getEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return R.ok(mobileService.getEmployees(keyword, page, size));
    }

    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public R<List<Role>> getRoles() {
        return R.ok(mobileService.getRoles());
    }

    @Operation(summary = "新增员工")
    @PostMapping("/employees")
    public R<Void> createEmployee(@Valid @RequestBody EmployeeRequest req) {
        mobileService.createEmployee(req);
        return R.ok();
    }

    @Operation(summary = "编辑员工")
    @PutMapping("/employees/{id}")
    public R<Void> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequest req) {
        mobileService.updateEmployee(id, req);
        return R.ok();
    }

    @Operation(summary = "删除员工")
    @DeleteMapping("/employees/{id}")
    public R<Void> deleteEmployee(@PathVariable Long id) {
        mobileService.deleteEmployee(id);
        return R.ok();
    }

    @Operation(summary = "启停用员工")
    @PutMapping("/employees/{id}/status")
    public R<Void> toggleEmployeeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        mobileService.toggleEmployeeStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "报工记录列表")
    @GetMapping("/reports")
    public R<List<AdminReportVO>> getReports(@RequestParam(required = false) String keyword) {
        return R.ok(mobileService.getReports(keyword));
    }

    @Operation(summary = "清除报工")
    @PostMapping("/reports/{id}/clear")
    public R<Void> clearReport(@PathVariable Long id, @RequestBody Map<String, String> body) {
        mobileService.clearReport(id, body.get("reason"));
        return R.ok();
    }

    @Operation(summary = "补录报工")
    @PostMapping("/reports/supplement")
    public R<Void> supplementReport(@RequestBody Map<String, Object> body) {
        String employeeNo = body.get("employeeNo").toString();
        Long bundleId = Long.valueOf(body.get("bundleId").toString());
        Long processId = Long.valueOf(body.get("processId").toString());
        Integer qty = Integer.valueOf(body.get("qty").toString());
        mobileService.supplementReport(employeeNo, bundleId, processId, qty);
        return R.ok();
    }
}

package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.entity.AnomalyAlert;
import com.zhiyi.erp.module.admin.service.AnomalyService;
import com.zhiyi.erp.module.admin.vo.AnomalyAlertVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/anomalies")
@Tag(name = "异常预警", description = "断层预警列表、处理（忽略/清除/补录）")
public class AnomalyController {
    @Autowired
    private AnomalyService anomalyService;

    @Operation(summary = "异常预警列表")
    @GetMapping
    public R<Page<AnomalyAlertVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "状态，0-待处理 1-已忽略 2-已清除 3-已补录 9-已处理") @RequestParam(required = false) Integer status) {
        return R.ok(anomalyService.pageVO(new Page<>(page, size), status));
    }

    @Operation(summary = "忽略预警")
    @PostMapping("/{id}/ignore")
    public R<Void> ignore(@Parameter(description = "预警ID") @PathVariable Long id, @Parameter(description = "忽略原因") @RequestParam String remark) {
        anomalyService.ignore(id, remark);
        return R.ok();
    }

    @Operation(summary = "清除预警")
    @PostMapping("/{id}/clear")
    public R<Void> clear(@Parameter(description = "预警ID") @PathVariable Long id, @Parameter(description = "清除原因") @RequestParam String reason) {
        anomalyService.clearReport(id, reason);
        return R.ok();
    }

    @Operation(summary = "补录报工")
    @PostMapping("/{id}/supplement")
    public R<Void> supplement(@Parameter(description = "预警ID") @PathVariable Long id, @Parameter(description = "工人ID") @RequestParam Long employeeId, @Parameter(description = "补录数量") @RequestParam Integer qty) {
        anomalyService.supplement(id, employeeId, qty);
        return R.ok();
    }
}

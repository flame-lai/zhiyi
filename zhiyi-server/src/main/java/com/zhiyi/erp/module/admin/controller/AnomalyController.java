package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.entity.AnomalyAlert;
import com.zhiyi.erp.module.admin.service.AnomalyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/anomalies")
public class AnomalyController {
    @Autowired
    private AnomalyService anomalyService;

    @GetMapping
    public R<Page<AnomalyAlert>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return R.ok(anomalyService.page(new Page<>(page, size), status));
    }

    @PostMapping("/{id}/ignore")
    public R<Void> ignore(@PathVariable Long id, @RequestParam String remark) {
        anomalyService.ignore(id, remark);
        return R.ok();
    }

    @PostMapping("/{id}/clear")
    public R<Void> clear(@PathVariable Long id, @RequestParam String reason) {
        anomalyService.clearReport(id, reason);
        return R.ok();
    }

    @PostMapping("/{id}/supplement")
    public R<Void> supplement(@PathVariable Long id, @RequestParam Long employeeId, @RequestParam Integer qty) {
        anomalyService.supplement(id, employeeId, qty);
        return R.ok();
    }
}

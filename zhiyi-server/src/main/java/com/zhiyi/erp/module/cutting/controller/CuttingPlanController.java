package com.zhiyi.erp.module.cutting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.service.CuttingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cutting/plans")
public class CuttingPlanController {
    @Autowired
    private CuttingService cuttingService;

    @GetMapping
    public R<Page<CuttingPlan>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long orderItemId) {
        return R.ok(cuttingService.page(new Page<>(page, size), orderItemId));
    }

    @PostMapping
    public R<Void> create(@RequestBody CuttingPlan plan) {
        cuttingService.save(plan);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody CuttingPlan plan) {
        plan.setId(id);
        cuttingService.updateById(plan);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        cuttingService.removeById(id);
        return R.ok();
    }

    @PostMapping("/{id}/input-actual")
    public R<Void> inputActualQty(@PathVariable Long id, @RequestParam Integer actualQty) {
        cuttingService.inputActualQty(id, actualQty);
        return R.ok();
    }

    @PostMapping("/{id}/open-bundle")
    public R<List<Bundle>> openBundle(@PathVariable Long id) {
        return R.ok(cuttingService.openBundle(id));
    }

    @GetMapping("/{id}/bundles")
    public R<List<Bundle>> getBundles(@PathVariable Long id) {
        return R.ok(cuttingService.getBundles(id));
    }
}

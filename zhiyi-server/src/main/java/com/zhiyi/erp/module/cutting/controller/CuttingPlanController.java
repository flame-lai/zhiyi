package com.zhiyi.erp.module.cutting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.cutting.dto.BundleDetailDTO;
import com.zhiyi.erp.module.cutting.dto.CuttingPlanDetailDTO;
import com.zhiyi.erp.module.cutting.dto.CuttingProgressDTO;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
import com.zhiyi.erp.module.cutting.entity.CuttingPlanDetail;
import com.zhiyi.erp.module.cutting.service.CuttingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "裁床计划", description = "裁床计划CRUD、录入实际数、开菲")
@RestController
@RequestMapping("/api/cutting/plans")
public class CuttingPlanController {
    @Autowired
    private CuttingService cuttingService;

    @Operation(summary = "分页查询裁床计划列表")
    @GetMapping
    public R<Page<CuttingPlanDetailDTO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "款号") @RequestParam(required = false) String styleCode,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return R.ok(cuttingService.page(new Page<>(page, size), orderNo, styleCode, status));
    }

    @Operation(summary = "根据ID查询裁床计划详情")
    @GetMapping("/{id}")
    public R<CuttingPlanDetailDTO> getById(@Parameter(description = "裁床计划ID") @PathVariable Long id) {
        return R.ok(cuttingService.getPlanDetailById(id));
    }

    @Operation(summary = "查询裁床计划的配比明细")
    @GetMapping("/{id}/details")
    public R<List<CuttingPlanDetail>> getDetails(@Parameter(description = "裁床计划ID") @PathVariable Long id) {
        return R.ok(cuttingService.getPlanDetails(id));
    }

    @Operation(summary = "创建裁床计划（含配比明细）")
    @PostMapping
    public R<Long> create(@RequestBody CreatePlanDTO dto) {
        return R.ok(cuttingService.savePlanWithDetails(dto.getPlan(), dto.getDetails()));
    }

    @Operation(summary = "更新裁床计划（含配比明细）")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "裁床计划ID") @PathVariable Long id, @RequestBody CreatePlanDTO dto) {
        dto.getPlan().setId(id);
        cuttingService.savePlanWithDetails(dto.getPlan(), dto.getDetails());
        return R.ok();
    }

    @Operation(summary = "删除裁床计划")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "裁床计划ID") @PathVariable Long id) {
        cuttingService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "录入实际裁剪数量（按颜色尺码明细）")
    @PostMapping("/{id}/input-actual")
    public R<Void> inputActualQty(@Parameter(description = "裁床计划ID") @PathVariable Long id, @RequestBody List<CuttingPlanDetail> details) {
        cuttingService.inputActualQty(id, details);
        return R.ok();
    }

    @Operation(summary = "开菲生成扎号")
    @PostMapping("/{id}/open-bundle")
    public R<List<Bundle>> openBundle(@Parameter(description = "裁床计划ID") @PathVariable Long id) {
        return R.ok(cuttingService.openBundle(id));
    }

    @Operation(summary = "查询裁床计划的扎号列表")
    @GetMapping("/{id}/bundles")
    public R<List<Bundle>> getBundles(@Parameter(description = "裁床计划ID") @PathVariable Long id) {
        return R.ok(cuttingService.getBundles(id));
    }

    @Operation(summary = "查询裁床计划的扎号详情（含订单号、款号）")
    @GetMapping("/{id}/bundle-details")
    public R<List<BundleDetailDTO>> getBundleDetails(@Parameter(description = "裁床计划ID") @PathVariable Long id) {
        return R.ok(cuttingService.getBundleDetails(id));
    }

    @Operation(summary = "查询订单的裁剪进度")
    @GetMapping("/progress")
    public R<List<CuttingProgressDTO>> getProgress(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "款号ID") @RequestParam Long styleId) {
        return R.ok(cuttingService.getCuttingProgress(orderId, styleId));
    }

    public static class CreatePlanDTO {
        private CuttingPlan plan;
        private List<CuttingPlanDetail> details;
        public CuttingPlan getPlan() { return plan; }
        public void setPlan(CuttingPlan plan) { this.plan = plan; }
        public List<CuttingPlanDetail> getDetails() { return details; }
        public void setDetails(List<CuttingPlanDetail> details) { this.details = details; }
    }
}

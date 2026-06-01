package com.zhiyi.erp.module.cutting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.cutting.dto.CuttingPlanDetailDTO;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.entity.CuttingPlan;
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
            @Parameter(description = "颜色") @RequestParam(required = false) String color,
            @Parameter(description = "尺码") @RequestParam(required = false) String size2) {
        return R.ok(cuttingService.page(new Page<>(page, size), orderNo, styleCode, color, size2));
    }

    @Operation(summary = "根据ID查询裁床计划详情")
    @GetMapping("/{id}")
    public R<CuttingPlanDetailDTO> getById(@Parameter(description = "裁床计划ID") @PathVariable Long id) {
        return R.ok(cuttingService.getPlanDetailById(id));
    }

    @Operation(summary = "创建裁床计划")
    @PostMapping
    public R<Void> create(@RequestBody CuttingPlan plan) {
        cuttingService.save(plan);
        return R.ok();
    }

    @Operation(summary = "更新裁床计划")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "裁床计划ID") @PathVariable Long id, @RequestBody CuttingPlan plan) {
        plan.setId(id);
        cuttingService.updateById(plan);
        return R.ok();
    }

    @Operation(summary = "删除裁床计划")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "裁床计划ID") @PathVariable Long id) {
        cuttingService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "录入实际裁剪数量")
    @PostMapping("/{id}/input-actual")
    public R<Void> inputActualQty(@Parameter(description = "裁床计划ID") @PathVariable Long id, @RequestBody InputActualDTO dto) {
        cuttingService.inputActualQty(id, dto.getActualQty());
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

    public static class InputActualDTO {
        private Integer actualQty;
        public Integer getActualQty() { return actualQty; }
        public void setActualQty(Integer actualQty) { this.actualQty = actualQty; }
    }
}

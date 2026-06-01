package com.zhiyi.erp.module.cutting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.cutting.dto.BundleDetailDTO;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.service.CuttingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "扎号管理", description = "扎号查询、分页列表")
@RestController
@RequestMapping("/api/cutting/bundles")
public class BundleController {
    @Autowired
    private CuttingService cuttingService;

    @Operation(summary = "分页查询扎号列表")
    @GetMapping
    public R<Page<BundleDetailDTO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "款号") @RequestParam(required = false) String styleCode,
            @Parameter(description = "颜色") @RequestParam(required = false) String color,
            @Parameter(description = "尺码") @RequestParam(required = false) String size2,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return R.ok(cuttingService.pageBundles(new Page<>(page, size), orderNo, styleCode, color, size2, status));
    }

    @Operation(summary = "根据ID查询扎号详情")
    @GetMapping("/{id}")
    public R<BundleDetailDTO> getById(@Parameter(description = "扎号ID") @PathVariable Long id) {
        return R.ok(cuttingService.getBundleDetailById(id));
    }

    @Operation(summary = "根据订单ID查询扎号列表")
    @GetMapping("/by-order/{orderId}")
    public R<List<Bundle>> getByOrderId(@Parameter(description = "订单ID") @PathVariable Long orderId) {
        return R.ok(cuttingService.getBundlesByOrderId(orderId));
    }
}

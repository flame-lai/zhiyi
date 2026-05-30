package com.zhiyi.erp.module.cutting.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.service.CuttingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "扎号管理", description = "扎号查询")
@RestController
@RequestMapping("/api/cutting/bundles")
public class BundleController {
    @Autowired
    private CuttingService cuttingService;

    @Operation(summary = "根据ID查询扎号详情")
    @GetMapping("/{id}")
    public R<Bundle> getById(@Parameter(description = "扎号ID") @PathVariable Long id) {
        return R.ok(cuttingService.getBundleById(id));
    }

    @Operation(summary = "根据订单ID查询扎号列表")
    @GetMapping("/by-order/{orderId}")
    public R<List<Bundle>> getByOrderId(@Parameter(description = "订单ID") @PathVariable Long orderId) {
        return R.ok(cuttingService.getBundlesByOrderId(orderId));
    }
}

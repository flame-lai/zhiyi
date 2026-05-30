package com.zhiyi.erp.module.cutting.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.cutting.entity.Bundle;
import com.zhiyi.erp.module.cutting.service.CuttingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cutting/bundles")
public class BundleController {
    @Autowired
    private CuttingService cuttingService;

    @GetMapping("/{id}")
    public R<Bundle> getById(@PathVariable Long id) {
        return R.ok(cuttingService.getBundleById(id));
    }

    @GetMapping("/by-order/{orderId}")
    public R<List<Bundle>> getByOrderId(@PathVariable Long orderId) {
        return R.ok(cuttingService.getBundlesByOrderId(orderId));
    }
}

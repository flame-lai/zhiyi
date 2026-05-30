package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.entity.WageOther;
import com.zhiyi.erp.module.admin.entity.WageSummary;
import com.zhiyi.erp.module.admin.service.WageManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/wages")
public class WageManageController {
    @Autowired
    private WageManageService wageManageService;

    @GetMapping
    public R<Page<WageSummary>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) Integer status) {
        return R.ok(wageManageService.page(new Page<>(page, size), yearMonth, status));
    }

    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id) {
        wageManageService.audit(id);
        return R.ok();
    }

    @PostMapping("/{id}/mark-paid")
    public R<Void> markPaid(@PathVariable Long id) {
        wageManageService.markPaid(id);
        return R.ok();
    }

    @GetMapping("/other-items")
    public R<List<WageOther>> getOtherItems(
            @RequestParam Long employeeId, @RequestParam String yearMonth) {
        return R.ok(wageManageService.getOtherItems(employeeId, yearMonth));
    }

    @PostMapping("/other-items")
    public R<Void> addOtherItem(@RequestBody WageOther item) {
        wageManageService.addOtherItem(item);
        return R.ok();
    }
}

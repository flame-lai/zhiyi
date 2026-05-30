package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.entity.WageOther;
import com.zhiyi.erp.module.admin.entity.WageSummary;
import com.zhiyi.erp.module.admin.service.WageManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/wages")
@Tag(name = "工资管理", description = "工资汇总、审核、其他项目管理")
public class WageManageController {
    @Autowired
    private WageManageService wageManageService;

    @Operation(summary = "工资汇总列表")
    @GetMapping
    public R<Page<WageSummary>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "年月，格式yyyy-MM") @RequestParam(required = false) String yearMonth,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer status) {
        return R.ok(wageManageService.page(new Page<>(page, size), yearMonth, status));
    }

    @Operation(summary = "审核工资")
    @PostMapping("/{id}/audit")
    public R<Void> audit(@Parameter(description = "工资汇总ID") @PathVariable Long id) {
        wageManageService.audit(id);
        return R.ok();
    }

    @Operation(summary = "标记已发放")
    @PostMapping("/{id}/mark-paid")
    public R<Void> markPaid(@Parameter(description = "工资汇总ID") @PathVariable Long id) {
        wageManageService.markPaid(id);
        return R.ok();
    }

    @Operation(summary = "查询其他工资项目")
    @GetMapping("/other-items")
    public R<List<WageOther>> getOtherItems(
            @Parameter(description = "工人ID") @RequestParam Long employeeId, @Parameter(description = "年月，格式yyyy-MM") @RequestParam String yearMonth) {
        return R.ok(wageManageService.getOtherItems(employeeId, yearMonth));
    }

    @Operation(summary = "新增其他工资项目")
    @PostMapping("/other-items")
    public R<Void> addOtherItem(@RequestBody WageOther item) {
        wageManageService.addOtherItem(item);
        return R.ok();
    }
}

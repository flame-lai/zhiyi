package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.entity.TimedWage;
import com.zhiyi.erp.module.admin.service.TimedWageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/timed-wages")
@Tag(name = "计时工资", description = "计时工资CRUD、批量新增")
public class TimedWageController {
    @Autowired
    private TimedWageService timedWageService;

    @Operation(summary = "计时工资列表")
    @GetMapping
    public R<Page<TimedWage>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "工人ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "年月，格式yyyy-MM") @RequestParam(required = false) String yearMonth) {
        return R.ok(timedWageService.page(new Page<>(page, size), employeeId, yearMonth));
    }

    @Operation(summary = "新增计时工资")
    @PostMapping
    public R<Void> create(@RequestBody TimedWage wage) {
        timedWageService.save(wage);
        return R.ok();
    }

    @Operation(summary = "批量新增计时工资")
    @PostMapping("/batch")
    public R<Void> createBatch(@RequestBody List<TimedWage> wages) {
        timedWageService.createBatch(wages);
        return R.ok();
    }

    @Operation(summary = "修改计时工资")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "计时工资ID") @PathVariable Long id, @RequestBody TimedWage wage) {
        wage.setId(id);
        timedWageService.updateById(wage);
        return R.ok();
    }

    @Operation(summary = "删除计时工资")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "计时工资ID") @PathVariable Long id) {
        timedWageService.removeById(id);
        return R.ok();
    }
}

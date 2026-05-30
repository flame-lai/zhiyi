package com.zhiyi.erp.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.admin.entity.TimedWage;
import com.zhiyi.erp.module.admin.service.TimedWageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/timed-wages")
public class TimedWageController {
    @Autowired
    private TimedWageService timedWageService;

    @GetMapping
    public R<Page<TimedWage>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String yearMonth) {
        return R.ok(timedWageService.page(new Page<>(page, size), employeeId, yearMonth));
    }

    @PostMapping
    public R<Void> create(@RequestBody TimedWage wage) {
        timedWageService.save(wage);
        return R.ok();
    }

    @PostMapping("/batch")
    public R<Void> createBatch(@RequestBody List<TimedWage> wages) {
        timedWageService.createBatch(wages);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody TimedWage wage) {
        wage.setId(id);
        timedWageService.updateById(wage);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        timedWageService.removeById(id);
        return R.ok();
    }
}

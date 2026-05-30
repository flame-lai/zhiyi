package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Process;
import com.zhiyi.erp.module.base.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/processes")
public class ProcessController {
    @Autowired
    private ProcessService processService;

    @GetMapping
    public R<Page<Process>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category) {
        return R.ok(processService.page(new Page<>(page, size), category));
    }

    @PostMapping
    public R<Void> create(@RequestBody Process process) {
        processService.save(process);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Process process) {
        process.setId(id);
        processService.updateById(process);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        processService.removeById(id);
        return R.ok();
    }
}

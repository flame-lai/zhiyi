package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Process;
import com.zhiyi.erp.module.base.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "工序管理", description = "工序CRUD、分类管理")
@RestController
@RequestMapping("/api/base/processes")
public class ProcessController {
    @Autowired
    private ProcessService processService;

    @Operation(summary = "分页查询工序列表")
    @GetMapping
    public R<Page<Process>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "工序分类") @RequestParam(required = false) String category) {
        return R.ok(processService.page(new Page<>(page, size), category));
    }

    @Operation(summary = "新增工序")
    @PostMapping
    public R<Void> create(@RequestBody Process process) {
        processService.save(process);
        return R.ok();
    }

    @Operation(summary = "修改工序")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "工序ID") @PathVariable Long id, @RequestBody Process process) {
        process.setId(id);
        processService.updateById(process);
        return R.ok();
    }

    @Operation(summary = "删除工序")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "工序ID") @PathVariable Long id) {
        processService.removeById(id);
        return R.ok();
    }
}

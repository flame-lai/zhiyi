package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Part;
import com.zhiyi.erp.module.base.service.PartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "部位管理", description = "裁片部位CRUD")
@RestController
@RequestMapping("/api/base/parts")
public class PartController {
    @Autowired
    private PartService partService;

    @Operation(summary = "分页查询部位列表")
    @GetMapping
    public R<Page<Part>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return R.ok(partService.page(new Page<>(page, size), keyword));
    }

    @Operation(summary = "新增部位")
    @PostMapping
    public R<Void> create(@RequestBody Part part) {
        partService.save(part);
        return R.ok();
    }

    @Operation(summary = "修改部位")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "部位ID") @PathVariable Long id, @RequestBody Part part) {
        part.setId(id);
        partService.updateById(part);
        return R.ok();
    }

    @Operation(summary = "删除部位")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "部位ID") @PathVariable Long id) {
        partService.removeById(id);
        return R.ok();
    }
}

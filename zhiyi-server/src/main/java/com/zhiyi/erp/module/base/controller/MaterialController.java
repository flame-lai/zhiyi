package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Material;
import com.zhiyi.erp.module.base.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "物料管理", description = "物料CRUD、类型管理")
@RestController
@RequestMapping("/api/base/materials")
public class MaterialController {
    @Autowired
    private MaterialService materialService;

    @Operation(summary = "分页查询物料列表")
    @GetMapping
    public R<Page<Material>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "类型：1原料 2辅料") @RequestParam(required = false) Integer type) {
        return R.ok(materialService.page(new Page<>(page, size), keyword, type));
    }

    @Operation(summary = "根据ID查询物料详情")
    @GetMapping("/{id}")
    public R<Material> getById(@Parameter(description = "物料ID") @PathVariable Long id) {
        return R.ok(materialService.getById(id));
    }

    @Operation(summary = "新增物料")
    @PostMapping
    public R<Void> create(@RequestBody Material material) {
        materialService.save(material);
        return R.ok();
    }

    @Operation(summary = "修改物料")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "物料ID") @PathVariable Long id, @RequestBody Material material) {
        material.setId(id);
        materialService.updateById(material);
        return R.ok();
    }

    @Operation(summary = "删除物料")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "物料ID") @PathVariable Long id) {
        materialService.removeById(id);
        return R.ok();
    }
}

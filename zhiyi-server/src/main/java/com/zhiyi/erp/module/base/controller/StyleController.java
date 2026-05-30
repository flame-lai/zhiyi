package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Style;
import com.zhiyi.erp.module.base.entity.StyleMaterial;
import com.zhiyi.erp.module.base.entity.StyleProcess;
import com.zhiyi.erp.module.base.service.StyleService;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "款号管理", description = "款号CRUD、标准工序路由、标准物料清单")
@RestController
@RequestMapping("/api/base/styles")
public class StyleController {
    @Autowired
    private StyleService styleService;

    @Operation(summary = "分页查询款号列表")
    @GetMapping
    public R<Page<Style>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return R.ok(styleService.page(new Page<>(page, size), keyword));
    }

    @Operation(summary = "根据ID查询款号详情")
    @GetMapping("/{id}")
    public R<Style> getById(@Parameter(description = "款号ID") @PathVariable Long id) {
        return R.ok(styleService.getById(id));
    }

    @Operation(summary = "新增款号")
    @PostMapping
    public R<Long> create(@RequestBody Style style) {
        styleService.save(style);
        return R.ok(style.getId());
    }

    @Operation(summary = "修改款号")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "款号ID") @PathVariable Long id, @RequestBody Style style) {
        style.setId(id);
        styleService.updateById(style);
        return R.ok();
    }

    @Operation(summary = "删除款号")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "款号ID") @PathVariable Long id) {
        styleService.removeById(id);
        return R.ok();
    }

    @Operation(summary = "查询款号标准工序路由")
    @GetMapping("/{id}/processes")
    public R<List<StyleProcess>> getProcesses(@Parameter(description = "款号ID") @PathVariable Long id) {
        return R.ok(styleService.getStyleProcesses(id));
    }

    @Operation(summary = "保存款号标准工序路由")
    @PostMapping("/{id}/processes")
    public R<Void> saveProcesses(@Parameter(description = "款号ID") @PathVariable Long id, @RequestBody List<StyleProcess> processes) {
        styleService.saveStyleProcesses(id, processes);
        return R.ok();
    }

    @Operation(summary = "查询款号标准物料清单")
    @GetMapping("/{id}/materials")
    public R<List<StyleMaterial>> getMaterials(@Parameter(description = "款号ID") @PathVariable Long id) {
        return R.ok(styleService.getStyleMaterials(id));
    }

    @Operation(summary = "保存款号标准物料清单")
    @PostMapping("/{id}/materials")
    public R<Void> saveMaterials(@Parameter(description = "款号ID") @PathVariable Long id, @RequestBody List<StyleMaterial> materials) {
        styleService.saveStyleMaterials(id, materials);
        return R.ok();
    }
}

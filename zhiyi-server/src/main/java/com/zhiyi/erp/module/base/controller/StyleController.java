package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Style;
import com.zhiyi.erp.module.base.entity.StyleMaterial;
import com.zhiyi.erp.module.base.entity.StyleProcess;
import com.zhiyi.erp.module.base.service.StyleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/base/styles")
public class StyleController {
    @Autowired
    private StyleService styleService;

    @GetMapping
    public R<Page<Style>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return R.ok(styleService.page(new Page<>(page, size), keyword));
    }

    @GetMapping("/{id}")
    public R<Style> getById(@PathVariable Long id) {
        return R.ok(styleService.getById(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody Style style) {
        styleService.save(style);
        return R.ok(style.getId());
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Style style) {
        style.setId(id);
        styleService.updateById(style);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        styleService.removeById(id);
        return R.ok();
    }

    @GetMapping("/{id}/processes")
    public R<List<StyleProcess>> getProcesses(@PathVariable Long id) {
        return R.ok(styleService.getStyleProcesses(id));
    }

    @PostMapping("/{id}/processes")
    public R<Void> saveProcesses(@PathVariable Long id, @RequestBody List<StyleProcess> processes) {
        styleService.saveStyleProcesses(id, processes);
        return R.ok();
    }

    @GetMapping("/{id}/materials")
    public R<List<StyleMaterial>> getMaterials(@PathVariable Long id) {
        return R.ok(styleService.getStyleMaterials(id));
    }

    @PostMapping("/{id}/materials")
    public R<Void> saveMaterials(@PathVariable Long id, @RequestBody List<StyleMaterial> materials) {
        styleService.saveStyleMaterials(id, materials);
        return R.ok();
    }
}

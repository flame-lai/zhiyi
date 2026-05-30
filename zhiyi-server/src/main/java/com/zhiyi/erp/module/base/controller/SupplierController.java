package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Supplier;
import com.zhiyi.erp.module.base.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "供应商管理", description = "供应商档案CRUD")
@RestController
@RequestMapping("/api/base/suppliers")
public class SupplierController {
    @Autowired
    private SupplierService supplierService;

    @Operation(summary = "分页查询供应商列表")
    @GetMapping
    public R<Page<Supplier>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return R.ok(supplierService.page(new Page<>(page, size), keyword));
    }

    @Operation(summary = "新增供应商")
    @PostMapping
    public R<Void> create(@RequestBody Supplier supplier) {
        supplierService.save(supplier);
        return R.ok();
    }

    @Operation(summary = "修改供应商")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "供应商ID") @PathVariable Long id, @RequestBody Supplier supplier) {
        supplier.setId(id);
        supplierService.updateById(supplier);
        return R.ok();
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "供应商ID") @PathVariable Long id) {
        supplierService.removeById(id);
        return R.ok();
    }
}

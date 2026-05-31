package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Customer;
import com.zhiyi.erp.module.base.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "客户管理", description = "客户档案CRUD")
@RestController
@RequestMapping("/api/base/customers")
public class CustomerController {
    @Autowired
    private CustomerService customerService;

    @Operation(summary = "分页查询客户列表")
    @GetMapping
    public R<Page<Customer>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return R.ok(customerService.page(new Page<>(page, size), keyword));
    }

    @Operation(summary = "根据ID查询客户详情")
    @GetMapping("/{id}")
    public R<Customer> getById(@Parameter(description = "客户ID") @PathVariable Long id) {
        return R.ok(customerService.getById(id));
    }

    @Operation(summary = "新增客户")
    @PostMapping
    public R<Void> create(@RequestBody Customer customer) {
        customerService.save(customer);
        return R.ok();
    }

    @Operation(summary = "修改客户")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "客户ID") @PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        customerService.updateById(customer);
        return R.ok();
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "客户ID") @PathVariable Long id) {
        customerService.removeById(id);
        return R.ok();
    }
}

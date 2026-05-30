package com.zhiyi.erp.module.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.base.entity.Customer;
import com.zhiyi.erp.module.base.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/customers")
public class CustomerController {
    @Autowired
    private CustomerService customerService;

    @GetMapping
    public R<Page<Customer>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return R.ok(customerService.page(new Page<>(page, size), keyword));
    }

    @PostMapping
    public R<Void> create(@RequestBody Customer customer) {
        customerService.save(customer);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        customerService.updateById(customer);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        customerService.removeById(id);
        return R.ok();
    }
}

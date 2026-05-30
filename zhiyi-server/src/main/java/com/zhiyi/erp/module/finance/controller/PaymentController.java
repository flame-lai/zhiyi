package com.zhiyi.erp.module.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.finance.entity.Payment;
import com.zhiyi.erp.module.finance.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/payments")
@Tag(name = "收支管理", description = "收支记录CRUD")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @Operation(summary = "收支记录列表")
    @GetMapping
    public R<Page<Payment>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "收支类型，1-收入 2-支出") @RequestParam(required = false) Integer type) {
        return R.ok(paymentService.page(new Page<>(page, size), orderId, type));
    }

    @Operation(summary = "新增收支记录")
    @PostMapping
    public R<Void> create(@RequestBody Payment payment) {
        paymentService.save(payment);
        return R.ok();
    }

    @Operation(summary = "修改收支记录")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "收支记录ID") @PathVariable Long id, @RequestBody Payment payment) {
        payment.setId(id);
        paymentService.updateById(payment);
        return R.ok();
    }

    @Operation(summary = "删除收支记录")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "收支记录ID") @PathVariable Long id) {
        paymentService.removeById(id);
        return R.ok();
    }
}

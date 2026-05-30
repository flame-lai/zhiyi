package com.zhiyi.erp.module.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.finance.entity.Payment;
import com.zhiyi.erp.module.finance.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public R<Page<Payment>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer type) {
        return R.ok(paymentService.page(new Page<>(page, size), orderId, type));
    }

    @PostMapping
    public R<Void> create(@RequestBody Payment payment) {
        paymentService.save(payment);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Payment payment) {
        payment.setId(id);
        paymentService.updateById(payment);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        paymentService.removeById(id);
        return R.ok();
    }
}

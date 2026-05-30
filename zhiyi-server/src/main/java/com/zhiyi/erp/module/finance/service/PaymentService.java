package com.zhiyi.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.finance.entity.Payment;
import com.zhiyi.erp.module.finance.mapper.PaymentMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService extends ServiceImpl<PaymentMapper, Payment> {

    public Page<Payment> page(Page<Payment> page, Long orderId, Integer type) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getFactoryId, TenantContext.get());
        if (orderId != null) {
            wrapper.eq(Payment::getOrderId, orderId);
        }
        if (type != null) {
            wrapper.eq(Payment::getType, type);
        }
        wrapper.orderByDesc(Payment::getPaymentDate);
        return page(page, wrapper);
    }

    public BigDecimal getOrderReceivedAmount(Long orderId) {
        List<Payment> payments = list(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getType, 1));
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrderSuggestVO {
    private Long id;
    private String orderNo;
    private String customerName;
    private BigDecimal totalAmount;
    private LocalDate deliveryDate;
    private Integer status;
}

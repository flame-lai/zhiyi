package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderSearchVO {
    private Long id;
    private String orderNo;
    private String customerName;
    private BigDecimal totalAmount;
    private LocalDate deliveryDate;
    private Integer status;
    private BigDecimal completionRate;
    private List<OrderItemVO> items;

    @Data
    public static class OrderItemVO {
        private String styleNo;
        private String color;
        private String size;
        private Integer qty;
        private Integer shippedQty;
        private BigDecimal progress;
    }
}

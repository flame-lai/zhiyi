package com.zhiyi.erp.module.mobile.vo;

import lombok.Data;

import java.util.List;

@Data
public class OrderListPageVO {
    private List<OrderListItemVO> records;
    private long total;
    private int page;
    private int size;
    private boolean hasMore;

    @Data
    public static class OrderListItemVO {
        private Long id;
        private String orderNo;
        private String customerName;
        private String styleNo;
        private String totalAmount;
        private Integer totalQty;
        private String deliveryDate;
        private Integer status;
        private String createdAt;
    }
}
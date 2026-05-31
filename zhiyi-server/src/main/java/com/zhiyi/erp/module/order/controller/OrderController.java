package com.zhiyi.erp.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.order.entity.*;
import com.zhiyi.erp.module.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "订单管理", description = "订单CRUD、状态流转、工序定价、物料清单")
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Operation(summary = "分页查询订单列表")
    @GetMapping
    public R<Page<Map<String, Object>>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        return R.ok(orderService.pageWithDetails(new Page<>(page, size), keyword, status, startDate, endDate));
    }

    @Operation(summary = "根据ID查询订单详情")
    @GetMapping("/{id}")
    public R<Order> getById(@Parameter(description = "订单ID") @PathVariable Long id) {
        return R.ok(orderService.getById(id));
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public R<Long> create(@RequestBody CreateOrderDTO dto) {
        return R.ok(orderService.createOrder(dto.getOrder(), dto.getItems()));
    }

    @Operation(summary = "更新订单")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "订单ID") @PathVariable Long id, @RequestBody Order order) {
        order.setId(id);
        orderService.updateById(order);
        return R.ok();
    }

    @Operation(summary = "查询订单物料明细")
    @GetMapping("/{id}/items")
    public R<List<OrderItem>> getItems(@Parameter(description = "订单ID") @PathVariable Long id) {
        return R.ok(orderService.getOrderItems(id));
    }

    @Operation(summary = "查询订单工序列表")
    @GetMapping("/{id}/processes")
    public R<List<OrderProcess>> getProcesses(@Parameter(description = "订单ID") @PathVariable Long id) {
        return R.ok(orderService.getOrderProcesses(id));
    }

    @Operation(summary = "保存订单工序定价")
    @PostMapping("/{id}/processes")
    public R<Void> saveProcesses(@Parameter(description = "订单ID") @PathVariable Long id, @RequestBody List<OrderProcess> processes) {
        orderService.saveOrderProcesses(id, processes);
        return R.ok();
    }

    @Operation(summary = "查询订单物料清单")
    @GetMapping("/{id}/materials")
    public R<List<OrderMaterial>> getMaterials(@Parameter(description = "订单ID") @PathVariable Long id) {
        return R.ok(orderService.getOrderMaterials(id));
    }

    @Operation(summary = "保存订单物料清单")
    @PostMapping("/{id}/materials")
    public R<Void> saveMaterials(@Parameter(description = "订单ID") @PathVariable Long id, @RequestBody List<OrderMaterial> materials) {
        orderService.saveOrderMaterials(id, materials);
        return R.ok();
    }

    @Operation(summary = "确认订单")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@Parameter(description = "订单ID") @PathVariable Long id) {
        orderService.confirmOrder(id);
        return R.ok();
    }

    @Operation(summary = "结算订单")
    @PostMapping("/{id}/settle")
    public R<Void> settle(@Parameter(description = "订单ID") @PathVariable Long id) {
        orderService.settleOrder(id);
        return R.ok();
    }

    public static class CreateOrderDTO {
        private Order order;
        private List<OrderItem> items;
        public Order getOrder() { return order; }
        public void setOrder(Order order) { this.order = order; }
        public List<OrderItem> getItems() { return items; }
        public void setItems(List<OrderItem> items) { this.items = items; }
    }
}

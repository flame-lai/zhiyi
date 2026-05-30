package com.zhiyi.erp.module.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.order.entity.*;
import com.zhiyi.erp.module.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping
    public R<Page<Order>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(orderService.page(new Page<>(page, size), keyword, status));
    }

    @GetMapping("/{id}")
    public R<Order> getById(@PathVariable Long id) {
        return R.ok(orderService.getById(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody CreateOrderDTO dto) {
        return R.ok(orderService.createOrder(dto.getOrder(), dto.getItems()));
    }

    @GetMapping("/{id}/items")
    public R<List<OrderItem>> getItems(@PathVariable Long id) {
        return R.ok(orderService.getOrderItems(id));
    }

    @GetMapping("/{id}/processes")
    public R<List<OrderProcess>> getProcesses(@PathVariable Long id) {
        return R.ok(orderService.getOrderProcesses(id));
    }

    @PostMapping("/{id}/processes")
    public R<Void> saveProcesses(@PathVariable Long id, @RequestBody List<OrderProcess> processes) {
        orderService.saveOrderProcesses(id, processes);
        return R.ok();
    }

    @GetMapping("/{id}/materials")
    public R<List<OrderMaterial>> getMaterials(@PathVariable Long id) {
        return R.ok(orderService.getOrderMaterials(id));
    }

    @PostMapping("/{id}/materials")
    public R<Void> saveMaterials(@PathVariable Long id, @RequestBody List<OrderMaterial> materials) {
        orderService.saveOrderMaterials(id, materials);
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        orderService.confirmOrder(id);
        return R.ok();
    }

    @PostMapping("/{id}/settle")
    public R<Void> settle(@PathVariable Long id) {
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

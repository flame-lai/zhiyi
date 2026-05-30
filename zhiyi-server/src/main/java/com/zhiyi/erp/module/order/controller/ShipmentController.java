package com.zhiyi.erp.module.order.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.order.entity.Shipment;
import com.zhiyi.erp.module.order.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "出货管理", description = "出货记录CRUD")
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    @Autowired
    private ShipmentService shipmentService;

    @Operation(summary = "查询订单出货记录")
    @GetMapping
    public R<List<Shipment>> list(@Parameter(description = "订单ID") @RequestParam Long orderId) {
        return R.ok(shipmentService.listByOrder(orderId));
    }

    @Operation(summary = "创建出货记录")
    @PostMapping
    public R<Void> create(@RequestBody Shipment shipment) {
        shipmentService.createShipment(shipment);
        return R.ok();
    }

    @Operation(summary = "删除出货记录")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "出货记录ID") @PathVariable Long id) {
        shipmentService.removeById(id);
        return R.ok();
    }
}

package com.zhiyi.erp.module.order.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.order.entity.Shipment;
import com.zhiyi.erp.module.order.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    @Autowired
    private ShipmentService shipmentService;

    @GetMapping
    public R<List<Shipment>> list(@RequestParam Long orderId) {
        return R.ok(shipmentService.listByOrder(orderId));
    }

    @PostMapping
    public R<Void> create(@RequestBody Shipment shipment) {
        shipmentService.createShipment(shipment);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        shipmentService.removeById(id);
        return R.ok();
    }
}

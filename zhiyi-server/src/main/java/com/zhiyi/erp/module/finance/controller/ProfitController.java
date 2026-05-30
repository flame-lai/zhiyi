package com.zhiyi.erp.module.finance.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.finance.service.ProfitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/profit")
@Tag(name = "利润分析", description = "利润分析（按订单/汇总）")
public class ProfitController {
    @Autowired
    private ProfitService profitService;

    @Operation(summary = "按订单查看利润")
    @GetMapping("/by-order")
    public R<List<Map<String, Object>>> byOrder() {
        return R.ok(profitService.getProfitByOrder());
    }

    @Operation(summary = "利润汇总")
    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        return R.ok(profitService.getProfitSummary());
    }
}

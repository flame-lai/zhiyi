package com.zhiyi.erp.module.finance.controller;

import com.zhiyi.erp.common.result.R;
import com.zhiyi.erp.module.finance.service.ProfitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/profit")
public class ProfitController {
    @Autowired
    private ProfitService profitService;

    @GetMapping("/by-order")
    public R<List<Map<String, Object>>> byOrder() {
        return R.ok(profitService.getProfitByOrder());
    }

    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        return R.ok(profitService.getProfitSummary());
    }
}

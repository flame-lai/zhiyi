package com.zhiyi.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyi.erp.common.tenant.TenantContext;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import com.zhiyi.erp.module.report.entity.WorkReport;
import com.zhiyi.erp.module.report.mapper.WorkReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfitService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private WorkReportMapper workReportMapper;

    public List<Map<String, Object>> getProfitByOrder() {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getFactoryId, TenantContext.get())
                        .eq(Order::getDeleted, 0));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Order order : orders) {
            List<WorkReport> reports = workReportMapper.selectList(
                    new LambdaQueryWrapper<WorkReport>()
                            .eq(WorkReport::getOrderId, order.getId())
                            .eq(WorkReport::getStatus, 1));

            BigDecimal cost = reports.stream()
                    .map(WorkReport::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal revenue = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal profit = revenue.subtract(cost);

            Map<String, Object> item = new HashMap<>();
            item.put("orderId", order.getId());
            item.put("orderNo", order.getOrderNo());
            item.put("revenue", revenue);
            item.put("cost", cost);
            item.put("profit", profit);
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                item.put("profitRate", profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP));
            } else {
                item.put("profitRate", BigDecimal.ZERO);
            }
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getProfitSummary() {
        List<Map<String, Object>> byOrder = getProfitByOrder();
        BigDecimal totalRevenue = byOrder.stream()
                .map(m -> (BigDecimal) m.get("revenue"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = byOrder.stream()
                .map(m -> (BigDecimal) m.get("cost"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = totalRevenue.subtract(totalCost);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalCost", totalCost);
        summary.put("totalProfit", totalProfit);
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            summary.put("avgProfitRate", totalProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP));
        } else {
            summary.put("avgProfitRate", BigDecimal.ZERO);
        }
        return summary;
    }
}

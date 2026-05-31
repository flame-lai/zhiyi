package com.zhiyi.erp.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyi.erp.module.order.entity.Order;
import com.zhiyi.erp.module.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static OrderMapper orderMapper;

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        OrderNoGenerator.orderMapper = orderMapper;
    }

    public static synchronized String generate() {
        String today = LocalDate.now().format(DATE_FMT);
        String prefix = "ORD" + today;

        // 查询今天最大的订单号
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Order::getOrderNo, prefix);
        wrapper.orderByDesc(Order::getOrderNo);
        wrapper.last("LIMIT 1");
        Order last = orderMapper.selectOne(wrapper);

        int nextSeq = 1;
        if (last != null && last.getOrderNo() != null) {
            String lastNo = last.getOrderNo();
            if (lastNo.startsWith(prefix) && lastNo.length() == prefix.length() + 3) {
                try {
                    nextSeq = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }

        return String.format("%s%03d", prefix, nextSeq);
    }
}

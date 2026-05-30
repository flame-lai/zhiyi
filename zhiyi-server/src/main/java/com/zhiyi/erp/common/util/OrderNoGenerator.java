package com.zhiyi.erp.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class OrderNoGenerator {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static String lastDate = "";

    public static synchronized String generate() {
        String today = LocalDate.now().format(DATE_FMT);
        if (!today.equals(lastDate)) {
            lastDate = today;
            SEQUENCE.set(0);
        }
        long seq = SEQUENCE.incrementAndGet();
        return String.format("ORD%s%03d", today, seq);
    }
}

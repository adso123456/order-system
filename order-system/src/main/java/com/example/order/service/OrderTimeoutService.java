package com.example.order.service;

import com.example.order.common.IllegalOrderStateException;
import com.example.order.common.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
public class OrderTimeoutService {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutService.class);
    private static final String ZSET_KEY = "order:delay";

    private final StringRedisTemplate redisTemplate;
    private final OrderService orderService;
    private final long timeoutSeconds;

    public OrderTimeoutService(StringRedisTemplate redisTemplate,
                               OrderService orderService,
                               @Value("${order.timeout-seconds}") long timeoutSeconds) {
        this.redisTemplate = redisTemplate;
        this.orderService = orderService;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Scheduled(fixedRate = 5000)
    public void processExpiredOrders() {
        long now = Instant.now().getEpochSecond();
        // 捞出所有 score <= 当前时间戳 的到期订单
        Set<String> expiredIds = redisTemplate.opsForZSet()
                .rangeByScore(ZSET_KEY, 0, now);

        if (expiredIds == null || expiredIds.isEmpty()) {
            return;
        }

        for (String idStr : expiredIds) {
            Long orderId = Long.valueOf(idStr);
            try {
                orderService.cancelOrder(orderId);
                log.info("超时取消订单成功: orderId={}", orderId);
            } catch (IllegalOrderStateException | ResourceNotFoundException e) {
                // 订单已支付 / 已取消 / 不存在，正常跳过
                log.debug("跳过订单 {}: {}", orderId, e.getMessage());
            }
            // 无论取消成功或跳过，都从 ZSet 移除，避免重复处理
            redisTemplate.opsForZSet().remove(ZSET_KEY, idStr);
        }
    }
}

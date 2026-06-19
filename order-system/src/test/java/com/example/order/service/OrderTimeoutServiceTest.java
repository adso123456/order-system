package com.example.order.service;

import com.example.order.common.IllegalOrderStateException;
import com.example.order.common.ResourceNotFoundException;
import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.OrderStatus;
import com.example.order.entity.Product;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "order.timeout-seconds=3")
class OrderTimeoutServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        redisTemplate.delete("order:delay");
    }

    @Test
    void shouldAutoCancelExpiredOrder() throws InterruptedException {
        Product p = createProduct("超时商品", new BigDecimal("99.00"), 10);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(1L, p.getId(), 2));
        assertThat(created.getStatus()).isEqualTo("PENDING");

        // 验证 Redis ZSet 有记录
        Set<String> members = redisTemplate.opsForZSet()
                .rangeByScore("order:delay", 0, Long.MAX_VALUE);
        assertThat(members).contains(created.getId().toString());

        // 库存已扣
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(8);

        // 等待超时 + 轮询周期（3s timeout + 5s polling + buffer）
        Thread.sleep(10_000);

        // 订单状态变为 CANCELLED
        var order = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 库存恢复
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(10);
    }

    @Test
    void shouldNotCancelPaidOrder() throws InterruptedException {
        Product p = createProduct("支付后不超时商品", new BigDecimal("50.00"), 10);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(1L, p.getId(), 2));

        // 立即支付
        orderService.payOrder(created.getId());
        assertThat(orderRepository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAID);

        // 等待超过超时时间
        Thread.sleep(10_000);

        // 仍为 PAID，库存不变
        var order = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(8);
    }

    // ===== 辅助方法 =====
    private Product createProduct(String name, BigDecimal price, int stock) {
        Product p = new Product(name, price, stock);
        return productRepository.save(p);
    }

    private OrderRequest buildOrderRequest(Long userId, Long productId, int quantity) {
        OrderRequest req = new OrderRequest();
        req.setUserId(userId);
        OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        req.setItems(Collections.singletonList(item));
        return req;
    }
}

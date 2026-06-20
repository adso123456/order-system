package com.example.order.service;

import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.OrderStatus;
import com.example.order.entity.Product;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

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
        setAuth(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAutoCancelExpiredOrder() {
        Product p = createProduct("超时商品", new BigDecimal("99.00"), 10);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(p.getId(), 2));
        assertThat(created.getStatus()).isEqualTo("PENDING");

        // 库存已扣
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(8);

        // 轮询等待定时任务执行取消 (3s timeout + 5s polling + buffer)
        await().atMost(Duration.ofSeconds(15))
               .pollInterval(Duration.ofMillis(500))
               .untilAsserted(() -> {
                   var order = orderRepository.findById(created.getId()).orElseThrow();
                   assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
               });

        // 库存恢复
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(10);
    }

    @Test
    void shouldNotCancelPaidOrder() {
        Product p = createProduct("支付后不超时商品", new BigDecimal("50.00"), 10);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(p.getId(), 2));

        // 立即支付
        orderService.payOrder(created.getId());
        assertThat(orderRepository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAID);

        // 等待确保超时已过
        await().pollDelay(Duration.ofSeconds(5))
               .atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> {
                   var order = orderRepository.findById(created.getId()).orElseThrow();
                   assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
               });

        // 库存不变
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(8);
    }

    // ===== 辅助方法 =====
    private Product createProduct(String name, BigDecimal price, int stock) {
        Product p = new Product(name, price, stock);
        return productRepository.save(p);
    }

    private void setAuth(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
    }

    private OrderRequest buildOrderRequest(Long productId, int quantity) {
        OrderRequest req = new OrderRequest();
        OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        req.setItems(Collections.singletonList(item));
        return req;
    }
}

package com.example.order.service;

import com.example.order.common.IllegalOrderStateException;
import com.example.order.common.InsufficientStockException;
import com.example.order.common.ResourceNotFoundException;
import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.entity.Product;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        setAuth(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===== 测试1: 正常下单 =====
    @Test
    void shouldPlaceOrderSuccessfully() {
        Product p = createProduct("测试商品", new BigDecimal("29.90"), 5);

        OrderRequest req = buildOrderRequest(p.getId(), 2);
        OrderResponse resp = orderService.placeOrder(req);

        assertThat(resp.getStatus()).isEqualTo("PENDING");
        assertThat(resp.getTotalAmount()).isEqualByComparingTo(new BigDecimal("59.80"));
        assertThat(resp.getItems()).hasSize(1);
        assertThat(resp.getItems().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("29.90"));

        // 验证库存扣减
        Product updated = productRepository.findById(p.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(3);
    }

    // ===== 测试2: 库存不足 =====
    @Test
    void shouldRejectWhenStockInsufficient() {
        Product p = createProduct("限量商品", new BigDecimal("100.00"), 1);

        OrderRequest req = buildOrderRequest(p.getId(), 5);

        assertThatThrownBy(() -> orderService.placeOrder(req))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("库存不足");

        // 库存未变
        Product unchanged = productRepository.findById(p.getId()).orElseThrow();
        assertThat(unchanged.getStock()).isEqualTo(1);
    }

    // ===== 测试3: 并发下单——验证乐观锁防止超卖 =====
    @Test
    void shouldHandleConcurrentOrdersWithOptimisticLock() throws InterruptedException {
        int initialStock = 10;
        int quantityPerOrder = 1;
        Product p = createProduct("热销商品", new BigDecimal("10.00"), initialStock);

        int threads = 20;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final long userId = 100 + i;
            executor.submit(() -> {
                try {
                    setAuth(userId);
                    OrderRequest req = buildOrderRequest(p.getId(), quantityPerOrder);
                    orderService.placeOrder(req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        int successes = successCount.get();
        int fails = failCount.get();

        // 定理 1: 库存不会被扣成负数（乐观锁防超卖）
        Product after = productRepository.findById(p.getId()).orElseThrow();
        assertThat(after.getStock()).isGreaterThanOrEqualTo(0);

        // 定理 2: 扣减守恒 — 初始库存 - 最终库存 == 成功订单数 × 每单购买量
        assertThat(initialStock - after.getStock()).isEqualTo(successes * quantityPerOrder);

        // 统计完整性: 成功 + 失败 == 总请求数
        assertThat(successes + fails).isEqualTo(threads);
    }

    // ===== T7 支付测试 =====

    @Test
    void shouldPayOrderSuccessfully() {
        Product p = createProduct("支付测试商品", new BigDecimal("99.00"), 10);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(p.getId(), 1));
        assertThat(created.getStatus()).isEqualTo("PENDING");

        OrderResponse paid = orderService.payOrder(created.getId());
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(paid.getPaidAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicatePayment() {
        Product p = createProduct("重复支付商品", new BigDecimal("50.00"), 5);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(p.getId(), 1));
        orderService.payOrder(created.getId()); // 第一次支付成功

        assertThatThrownBy(() -> orderService.payOrder(created.getId()))
                .isInstanceOf(IllegalOrderStateException.class)
                .hasMessageContaining("订单状态不允许支付");
    }

    @Test
    void shouldRejectPaymentForNonExistentOrder() {
        assertThatThrownBy(() -> orderService.payOrder(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    void shouldRejectPaymentForCancelledOrder() {
        Product p = createProduct("已取消商品", new BigDecimal("30.00"), 5);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(p.getId(), 1));

        // 手动改为 CANCELLED（模拟 T8 场景）
        Order order = orderRepository.findById(created.getId()).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        assertThatThrownBy(() -> orderService.payOrder(created.getId()))
                .isInstanceOf(IllegalOrderStateException.class)
                .hasMessageContaining("订单状态不允许支付");
    }

    // ===== T8 取消订单测试 =====

    @Test
    void shouldCancelOrderSuccessfully() {
        Product p = createProduct("取消测试商品", new BigDecimal("50.00"), 10);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(p.getId(), 2));
        assertThat(created.getStatus()).isEqualTo("PENDING");

        // 下单后库存扣减
        Product afterOrder = productRepository.findById(p.getId()).orElseThrow();
        assertThat(afterOrder.getStock()).isEqualTo(8);

        // 取消订单
        OrderResponse cancelled = orderService.cancelOrder(created.getId());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");

        // 库存恢复
        Product afterCancel = productRepository.findById(p.getId()).orElseThrow();
        assertThat(afterCancel.getStock()).isEqualTo(10);
    }

    @Test
    void shouldRejectDuplicateCancel() {
        Product p = createProduct("重复取消商品", new BigDecimal("50.00"), 10);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(p.getId(), 2));

        orderService.cancelOrder(created.getId()); // 第一次取消

        // 第二次取消应返回 409
        assertThatThrownBy(() -> orderService.cancelOrder(created.getId()))
                .isInstanceOf(IllegalOrderStateException.class)
                .hasMessageContaining("订单状态不允许取消");

        // 库存只恢复一次，不会多还
        Product after = productRepository.findById(p.getId()).orElseThrow();
        assertThat(after.getStock()).isEqualTo(10);
    }

    @Test
    void shouldRejectCancelForPaidOrder() {
        Product p = createProduct("已支付取消商品", new BigDecimal("99.00"), 5);
        OrderResponse created = orderService.placeOrder(buildOrderRequest(p.getId(), 1));
        orderService.payOrder(created.getId());

        // 取消已支付订单应返回 409
        assertThatThrownBy(() -> orderService.cancelOrder(created.getId()))
                .isInstanceOf(IllegalOrderStateException.class)
                .hasMessageContaining("订单状态不允许取消");

        // 库存不变
        Product after = productRepository.findById(p.getId()).orElseThrow();
        assertThat(after.getStock()).isEqualTo(4);
    }

    @Test
    void shouldRejectCancelForNonExistentOrder() {
        assertThatThrownBy(() -> orderService.cancelOrder(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("订单不存在");
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

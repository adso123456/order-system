package com.example.order.service;

import com.example.order.common.IllegalOrderStateException;
import com.example.order.common.InsufficientStockException;
import com.example.order.common.ResourceNotFoundException;
import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.OrderStatus;
import com.example.order.entity.Product;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final long timeoutSeconds;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        StringRedisTemplate redisTemplate,
                        @Value("${order.timeout-seconds}") long timeoutSeconds) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        // 1. 批量查询商品
        List<Long> productIds = request.getItems().stream()
                .map(OrderRequest.OrderItemRequest::getProductId)
                .distinct()
                .toList();

        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException("部分商品不存在");
        }

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 2. 逐条检查库存并扣减（save 触发 @Version 乐观锁，冲突在事务提交时抛出 ObjectOptimisticLockingFailureException）
        for (OrderRequest.OrderItemRequest item : request.getItems()) {
            Product product = productMap.get(item.getProductId());
            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "库存不足: " + product.getName() + " 当前库存 " + product.getStock() + ", 需要 " + item.getQuantity());
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        // 3. 构建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new java.util.ArrayList<>();

        for (OrderRequest.OrderItemRequest item : request.getItems()) {
            Product product = productMap.get(item.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice());           // 单价快照
            orderItem.setQuantity(item.getQuantity());
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            orderItem.setSubtotal(subtotal);
            orderItems.add(orderItem);

            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);
        // 写入 Redis ZSet 延时队列，score = 当前时间戳 + 超时秒数
        long deadline = Instant.now().getEpochSecond() + timeoutSeconds;
        redisTemplate.opsForZSet().add("order:delay", saved.getId().toString(), deadline);
        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public OrderResponse payOrder(Long orderId) {
        // 条件更新：只更新 PENDING 状态的订单，原子操作无需加锁
        int updated = orderRepository.payOrder(orderId, OrderStatus.PAID, OrderStatus.PENDING, LocalDateTime.now());
        if (updated == 0) {
            if (!orderRepository.existsById(orderId)) {
                throw new ResourceNotFoundException("订单不存在: id=" + orderId);
            }
            throw new IllegalOrderStateException("订单状态不允许支付");
        }
        // clearAutomatically 已清空持久化上下文，findById 读到 PAID 状态
        Order order = orderRepository.findById(orderId).orElseThrow();
        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        int updated = orderRepository.cancelOrder(orderId, OrderStatus.PENDING);
        if (updated == 0) {
            if (!orderRepository.existsById(orderId)) {
                throw new ResourceNotFoundException("订单不存在: id=" + orderId);
            }
            throw new IllegalOrderStateException("订单状态不允许取消");
        }
        // 恢复库存（仅在实际取消时执行，确保幂等取消不会多还库存）
        Order order = orderRepository.findById(orderId).orElseThrow();
        for (OrderItem item : order.getItems()) {
            productRepository.restoreStock(item.getProductId(), item.getQuantity());
        }
        return OrderResponse.fromEntity(order);
    }

    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

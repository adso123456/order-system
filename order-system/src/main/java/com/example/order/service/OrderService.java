package com.example.order.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
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
        return OrderResponse.fromEntity(saved);
    }

    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

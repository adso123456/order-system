package com.example.order.repository;

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Order o SET o.status = :paid, o.paidAt = :now WHERE o.id = :id AND o.status = :pending")
    int payOrder(@Param("id") Long id,
                 @Param("paid") OrderStatus paid,
                 @Param("pending") OrderStatus pending,
                 @Param("now") LocalDateTime now);
}

package org.example.trusttrade.order.repository;

import jakarta.persistence.LockModeType;
import org.example.trusttrade.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    @Query("SELECT o FROM Order o WHERE o.buyer.id = :userId")
    List<Order> getOrdersByUserId(@Param("userId") UUID userId);

    Optional<Order> findByOrderLockKey(String lockKey);
}

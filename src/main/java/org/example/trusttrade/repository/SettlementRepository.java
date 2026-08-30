package org.example.trusttrade.repository;

import org.example.trusttrade.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;


public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findBySellerId(UUID sellerId);

    List<Settlement> findByStatus(Settlement.Status status);
}
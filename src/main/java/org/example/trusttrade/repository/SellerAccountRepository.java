package org.example.trusttrade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.example.trusttrade.domain.SellerAccount;

public interface SellerAccountRepository extends JpaRepository<SellerAccount, Long> {

    Optional<SellerAccount> findByUserId(UUID userId);
}
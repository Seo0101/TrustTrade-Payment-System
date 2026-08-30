package org.example.trusttrade.service;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.domain.Settlement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import org.example.trusttrade.repository.SettlementRepository;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.login.domain.User;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;

    public List<Settlement> findBySellerId(UUID sellerId) {
        return settlementRepository.findBySellerId(sellerId);
    }

    public List<Settlement> findByStatus(Settlement.Status status) {
        return settlementRepository.findByStatus(status);
    }

    public Optional<Settlement> findById(Long id) {
        return settlementRepository.findById(id);
    }

    @Transactional
    public Settlement create(UUID sellerId, Long orderId, int amount, int fee) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Not Exist"));

        Settlement settlement = Settlement.builder()
                .orderId(orderId)
                .seller(seller)
                .amount(amount)
                .fee(fee)
                .status(Settlement.Status.READY)
                .build();

        return settlementRepository.save(settlement);
    }

    @Transactional
    public void markComplete(Long settlementId) {
        settlementRepository.findById(settlementId).ifPresent(s -> {
            s.setStatus(Settlement.Status.COMPLETE);
            s.setSettledAt(java.time.LocalDateTime.now());
        });
    }

    @Transactional
    public void markFailed(Long settlementId) {
        settlementRepository.findById(settlementId).ifPresent(s -> {
            s.setStatus(Settlement.Status.FAILED);
        });
    }
}

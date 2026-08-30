package org.example.trusttrade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import org.example.trusttrade.domain.SellerAccount;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.repository.SellerAccountRepository;
import org.example.trusttrade.login.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerAccountService {

    private final SellerAccountRepository sellerAccountRepository;
    private final UserRepository userRepository;

    public Optional<SellerAccount> findByUserId(UUID userId) {
        return sellerAccountRepository.findByUserId(userId);
    }

    @Transactional
    public SellerAccount create(UUID userId, String bankName, String bankCode, String accountNum, String holder) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Not Exist"));

        SellerAccount account = SellerAccount.builder()
                .user(user)
                .bankName(bankName)
                .bankCode(bankCode)
                .accountNum(accountNum)
                .accountHolder(holder)
                .isVerified(false)
                .build();

        return sellerAccountRepository.save(account);
    }

    @Transactional
    public void verify(Long accountId) {
        SellerAccount account = sellerAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Not Exist"));

        account.setIsVerified(true);
        account.setVerifiedAt(java.time.LocalDateTime.now());
    }
}

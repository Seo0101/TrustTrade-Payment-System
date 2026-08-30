package org.example.trusttrade.auction.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.auction.service.AuctionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class AuctionScheduler {
    private final AuctionService auctionService;

    // 매 1분마다 실행
    @Scheduled(fixedRate = 60000)
    public void checkAndCloseAuctions() throws IOException, InterruptedException {
        auctionService.closeExpiredAuctions();
        System.out.println(LocalDateTime.now());
        System.out.println("==Auctions closed==");
    }
}

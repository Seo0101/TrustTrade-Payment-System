package org.example.trusttrade.auction.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.auction.domain.Bids;
import org.example.trusttrade.auction.dto.BidsResponseDto;
import org.example.trusttrade.auction.dto.CreateBidReq;
import org.example.trusttrade.auction.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
@Slf4j
public class BidController {

    @Autowired private final BidService bidService;

    //bid 생성
    @PostMapping("/new")
    public ResponseEntity<?> createBid(@RequestBody CreateBidReq createBidReq) {
        try {
            Bids bid = bidService.creatBid
                    (createBidReq.getUser(), createBidReq.getAuctionId(), createBidReq.getBidPrice());
            BidsResponseDto response = BidsResponseDto.from(bid);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException  e) {
            return ResponseEntity.badRequest().body("입찰 생성 실패 : " + e.getMessage());
        }catch (Exception e) {
            log.error("예상치 못한 에러");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    //top5 bidder 조회 by auctionId
    @GetMapping("/top5/{auctionId}")
    public ResponseEntity<List<BidsResponseDto>> getTop5BidsByAuction(@PathVariable("auctionId") Long auctionId) {

        try{
            List<Bids> top5Bids = bidService.getTop5BidsByAuction(auctionId);

            List<BidsResponseDto> response = top5Bids.stream()
                    .map(BidsResponseDto::from)
                    .collect(Collectors.toList());

            if (response.isEmpty()) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.ok(response);
            }
        }catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

}

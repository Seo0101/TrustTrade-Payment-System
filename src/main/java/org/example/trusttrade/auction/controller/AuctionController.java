package org.example.trusttrade.auction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.auction.domain.AuctionStatus;
import org.example.trusttrade.auction.dto.AuctionItemDto;
import org.example.trusttrade.auction.dto.AuctionResDto;
import org.example.trusttrade.auction.service.AuctionService;
import org.example.trusttrade.item.dto.request.CategoryDto;
import org.example.trusttrade.item.dto.response.ItemResponseDto;
import org.example.trusttrade.item.service.ItemService;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
@Slf4j
public class AuctionController {

    @Autowired
    private final AuctionService auctionService;
    @Autowired
    private final ItemService itemService;
    private final UserService userService;

    //임시 경매 등록
    @PostMapping("/new")
    public ResponseEntity<?> createAuction(@RequestBody AuctionItemDto auctionItemDto) {
        User seller = userService.validateBusinessUser(auctionItemDto.getSellerId());
        auctionService.registerAuctionItem(auctionItemDto, seller, null);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //경매 조회 by 판매자
    @GetMapping("/{sellerId}/list")
    public ResponseEntity<?> auctionsBySellerId(@PathVariable("sellerId") UUID sellerId) {
        List<Auction> auctions = auctionService.getAuctionsBySeller(sellerId);

        List<AuctionResDto> response = auctions.stream()
                .map(AuctionResDto::fromEntity)
                .toList();

        return ResponseEntity.ok(response);

    }

    @GetMapping("/list")
    public ResponseEntity<List<AuctionItemDto>> auctions() {
        List<Auction> auctions = auctionService.getAuctions();

        List<AuctionItemDto> response = auctions.stream()
                .map(AuctionItemDto::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    //경매 페이지 + 낙찰 페이지
    @GetMapping("/{auctionId}/details")
    public ResponseEntity<?> auctionDetails(@PathVariable("auctionId") Long auctionId) {
        try {
            Auction auction = auctionService.getAuctionById(auctionId);

            // 경매 상태에 따른 분기 처리
            if (auction.getAuctionStatus() == AuctionStatus.CLOSED) {
                return ResponseEntity.ok(Map.of(
                        "message", "경매가 종료된 상태입니다.",
                        "auction", AuctionResDto.fromEntity(auction)));
            }
            return ResponseEntity.ok(Map.of(
                    "message", "경매가 진행 중입니다.",
                    "auction", AuctionResDto.fromEntity(auction)));

        } catch (IllegalArgumentException e) {
            // 경매가 존재하지 않는 경우
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    // 경매 물품 등록
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> registerAuction(
            @RequestPart("auction_item") @Valid AuctionItemDto auctionItemDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        try {
            // 판매자 권한 검증
            User seller = userService.validateBusinessUser(auctionItemDto.getSellerId());

            // 경매 물품 등록
            auctionService.registerAuctionItem(auctionItemDto, seller, images);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("경매 물품이 성공적으로 등록되었습니다.");

        } catch (Exception e) {
            log.error("경매 물품 등록 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("경매 물품 등록 실패: " + e.getMessage());
        }
    }

    // 카테고리별 조회
    @GetMapping("/category/list")
    public ResponseEntity<List<ItemResponseDto>> getItemsCategory(@RequestBody CategoryDto categoryDto) {
        List<ItemResponseDto> items = itemService.findByCategoryAndType(categoryDto);
        return ResponseEntity.ok(items);
    }

    // 판매자 이름별 조회
    @GetMapping("/seller/list")
    public ResponseEntity<List<ItemResponseDto>> getItemsSellerAccount(
            @RequestParam String sellerAccount,
            @RequestParam String itemType) {

        List<ItemResponseDto> items = itemService.findBySellerAccountAndType(sellerAccount, itemType);
        return ResponseEntity.ok(items);
    }
}

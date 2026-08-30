package org.example.trusttrade.item.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.dto.StoredImage;
import org.example.trusttrade.item.dto.request.ReserveRequest;
import org.example.trusttrade.item.dto.response.ItemResponseDto;
import org.example.trusttrade.item.service.ProductService;
import org.example.trusttrade.item.service.ImageService;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.item.dto.request.BasicItemDto;

import org.example.trusttrade.login.service.UserService;
import org.example.trusttrade.order.dto.ProductReserveResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserService userService;
    private final ImageService imageService;

    // 일반 물품 등록
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> registerBasicProduct(
            @RequestPart("item") @Valid BasicItemDto basicItemDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        List<StoredImage> storedImages = Collections.emptyList();

        try {

            // 판매자 권한 검증
            User seller = userService.validateBusinessUser(basicItemDto.getSellerId());

            // 일반 물품 등록
            productService.registerBasicItem(basicItemDto,seller,images);

            return ResponseEntity.status(HttpStatus.CREATED).body("일반 물품이 성공적으로 등록되었습니다.");

        } catch (Exception e) {
            log.error("물품 등록 중 오류 발생", e);

            imageService.deleteFiles(storedImages);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("물품 등록 실패: " + e.getMessage());
        }
    }

    // 일반 물품 전체 조회
    @GetMapping("/list")
    public ResponseEntity<List<ItemResponseDto>> getBasicItems() {
        List<ItemResponseDto> items = productService.getBasicItems();
        return ResponseEntity.ok(items);
    }

    //협의 -> 구매자 확정
    @PostMapping("/products/{productId}/reserve")
    public ResponseEntity<ProductReserveResponse> reserveProduct(
            @PathVariable Long productId,
            @RequestBody ReserveRequest request
    ) {
        Product product = productService.reserveProduct(productId, request.getBuyerId());

        ProductReserveResponse response = new ProductReserveResponse(
                productId,
                product.getUser().getId(),
                product.getReservedBuyer().getId()
        );
        return ResponseEntity.ok(response);
    }



}
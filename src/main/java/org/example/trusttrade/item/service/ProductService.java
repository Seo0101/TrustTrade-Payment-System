package org.example.trusttrade.item.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.global.error.ErrorCode;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.domain.products.ProductStatus;
import org.example.trusttrade.item.dto.StoredImage;
import org.example.trusttrade.item.dto.request.BasicItemDto;
import org.example.trusttrade.item.dto.response.ItemResponseDto;
import org.example.trusttrade.item.dto.response.ProductResponseDto;
import org.example.trusttrade.item.repository.ProductRepository;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.login.service.UserService;
import org.example.trusttrade.order.exception.BusinessException;
import org.example.trusttrade.order.exception.ProductNotFoundException;
import org.example.trusttrade.order.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ImageService imageService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;


    // 일반 물품 등록
    @Transactional
    public void registerBasicItem(BasicItemDto basicItemDto, User seller, List<MultipartFile> images) {
        List<StoredImage> storedImages = Collections.emptyList();
        try {

            // 이미지 변환
            storedImages = imageService.processImagesAndSetDto(images, basicItemDto);

            // Product 생성
            Product product = Product.fromDto(basicItemDto, seller);
            productRepository.save(product);

        } catch (Exception e) {
            // 업로드된 사진 삭제
            imageService.deleteFiles(storedImages);
            throw new RuntimeException("일반 물품 등록 중 오류 발생", e);
        }
    }

    // 일반 물품 전체 조회
    public List<ItemResponseDto> getBasicItems() {
        List<Product> items = productRepository.findAll();
        return items.stream()
                .map(ItemResponseDto::fromProduct)
                .collect(Collectors.toList());
    }


    // 5km 이내의 물품 조회
    public List<ProductResponseDto> findProductsNearby(double lat, double lng) {
        List<Product> products = productRepository.findNearby(lat, lng);
        return products.stream()
                .map(product -> ProductResponseDto.builder()
                        .item_id(product.getId())
                        .sellerAccount(product.getUser().getUserAccount()) // 판매자명 대신 계정 사용
                        .title(product.getName())
                        .price(product.getProductPrice())
                        .latitude(product.getProductLocation().getLatitude())
                        .longitude(product.getProductLocation().getLongitude())
                        .build())
                .collect(Collectors.toList());
    }

    //구매자 확정 및 상품 상태 변경
    @Transactional
    public Product reserveProduct(Long productId, UUID buyerId) {

        //중복 요청 처리 -> 락 적용된 조회(A 트랜잭션 진행중에 B는 조회 안됨)
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new UserNotFoundException(buyerId));

        //기존 Product 있는 경우 반환
        if (product.isIdempotentRequest(buyerId)) {
            return product;
        }

        //상품 상태, 구매자 정보 검증
        product.validateOrderable(buyerId);

        product.reserve(buyer);

        return product;
    }

}
package org.example.trusttrade.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.auction.domain.Auction;
import org.example.trusttrade.auction.repository.AuctionRepository;
import org.example.trusttrade.global.error.NotAllowUserType;
import org.example.trusttrade.item.domain.Category;
import org.example.trusttrade.item.domain.Item;
import org.example.trusttrade.item.domain.ItemCategory;
import org.example.trusttrade.item.domain.ItemImage;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.dto.request.CategoryDto;
import org.example.trusttrade.item.repository.*;
import org.example.trusttrade.item.dto.response.ItemResponseDto;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemService {

    private final ItemImageRepository itemImageRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final CategoryService categoryService;
    private final ProductRepository productRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    // 카테고리로 물품 조회
    public List<ItemResponseDto> findByCategoryAndType(CategoryDto categoryDto) {
        List<ItemCategory> itemCategories = itemCategoryRepository.findByCategory_Id(categoryDto.getCategoryId());

        return itemCategories.stream()
                .map(ItemCategory::getItem) // 부모 Item
                .filter(item -> item.getItemType() != null && item.getItemType().equalsIgnoreCase(categoryDto.getItemType()))
                .map(item -> {
                    Object unproxied = (item instanceof HibernateProxy) ? ((HibernateProxy) item).getHibernateLazyInitializer().getImplementation() : item;

                    if (unproxied instanceof Product p) return ItemResponseDto.fromProduct(p);
                    if (unproxied instanceof Auction a) return ItemResponseDto.fromAuction(a);

                    throw new IllegalStateException("지원하지 않는 아이템 타입: " + Hibernate.getClass(item).getSimpleName());
                })
                .toList();
    }


    @Transactional
    public void saveItemDetails(Item item, List<String> imageUrls, List<Integer> categoryIds) {
        log.debug("saveItemDetails 시작: itemId={}, imageCount={}, categoryCount={}",
                item.getId(),
                imageUrls == null ? 0 : imageUrls.size(),
                categoryIds == null ? 0 : categoryIds.size());

        // 1) 이미지 저장
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<ItemImage> images = ItemImage.fromDto(item, imageUrls);
            log.debug("이미지 변환 완료: itemId={}, imagesToSave={}", item.getId(), images.size());

            itemImageRepository.saveAll(images);
            log.debug("이미지 저장 완료: itemId={}, savedImages={}", item.getId(), images.size());
        } else {
            log.debug("저장할 이미지 없음: itemId={}", item.getId());
        }

        // 2) 카테고리 매핑
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> categories = categoryService.getValidCategories(categoryIds);
            List<Long> categoryIdList = categories.stream()
                    .map(Category::getId)
                    .toList();
            log.debug("유효 카테고리 조회 완료: itemId={}, categories={}", item.getId(), categoryIdList);

            List<ItemCategory> mappings = ItemCategory.createMappings(item, categories);
            log.debug("ItemCategory 매핑 생성 완료: itemId={}, mappingsCount={}", item.getId(), mappings.size());

            itemCategoryRepository.saveAll(mappings);
            log.debug("카테고리 매핑 저장 완료: itemId={}, savedMappings={}", item.getId(), mappings.size());
        } else {
            log.debug("매핑할 카테고리 없음: itemId={}", item.getId());
        }

        log.debug("saveItemDetails 완료: itemId={}", item.getId());
    }


    // 판매자 아이디로 상품 조회
    @Transactional(readOnly = true)
    public List<ItemResponseDto> findBySellerAccountAndType(String sellerAccount, String itemType) {

        // 사용자 조회
        User seller = userRepository.findByUserAccount(sellerAccount)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매자 계정입니다."));

        // 사용자 조건 조회
        if (seller.getMemberType() != User.MemberType.BUSINESS) {
            throw new NotAllowUserType("일반 회원은 물건을 등록 할 수 없습니다");
        }

        // 타입에 따라 분리 조회
        if ("PRODUCT".equalsIgnoreCase(itemType)) {          // 일반 물품 조회
            return productRepository.findByUser_Id(seller.getId())
                    .stream()
                    .map(ItemResponseDto::fromProduct)
                    .toList();

        } else if ("AUCTION".equalsIgnoreCase(itemType)) {   // 경매 물품 조회
            return auctionRepository.findByUser_Id(seller.getId())
                    .stream()
                    .map(ItemResponseDto::fromAuction)
                    .toList();

        } else {
            throw new IllegalArgumentException("유효하지 않은 아이템 타입입니다: " + itemType);
        }
    }


    // 물품 이름으로 상품 조회
    @Transactional(readOnly = true)
    public List<ItemResponseDto> findByItemTitleAndType(String title, String itemType) {

        String type = itemType == null ? "" : itemType.trim().toUpperCase();

        // 일반 물품 조회
        if ("PRODUCT".equals(type)) {
            return productRepository.findByTitleContainingWithSeller(title).stream()
                    .map(ItemResponseDto::fromProduct)
                    .toList();
        }

        // 경매 물품 조회
        if ("AUCTION".equals(type)) {
            return auctionRepository.findByTitleContainingWithSeller(title).stream()
                    .map(ItemResponseDto::fromAuction)
                    .toList();
        }
        throw new IllegalArgumentException("허용되지 않는 itemType: " + itemType + " (PRODUCT|AUCTION 중 하나를 사용)");
    }


}



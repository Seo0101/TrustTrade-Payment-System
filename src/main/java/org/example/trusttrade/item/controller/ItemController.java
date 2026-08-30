package org.example.trusttrade.item.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.item.dto.request.CategoryDto;
import org.example.trusttrade.item.dto.response.ItemResponseDto;
import org.example.trusttrade.item.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/item")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // 카테고리별 조회
    @GetMapping("/category/list")
    public ResponseEntity<List<ItemResponseDto>> getItemsCategory(@RequestBody CategoryDto categoryDto) {
        List<ItemResponseDto> items = itemService.findByCategoryAndType(categoryDto);
        return ResponseEntity.ok(items);
    }

    // 판매자 이름별(아이디) 조회
    @GetMapping("/seller/list")
    public ResponseEntity<List<ItemResponseDto>> getItemsBySellerAccount(@RequestParam String sellerAccount, @RequestParam String itemType) {
        List<ItemResponseDto> items = itemService.findBySellerAccountAndType(sellerAccount, itemType);
        return ResponseEntity.ok(items);
    }

    // 물품명 조회
    @GetMapping("/title/list")
    public ResponseEntity<List<ItemResponseDto>> getItemsByTitle(@RequestParam String title, @RequestParam String itemType) {
        List<ItemResponseDto> items = itemService.findByItemTitleAndType(title, itemType);
        return ResponseEntity.ok(items);
    }
}

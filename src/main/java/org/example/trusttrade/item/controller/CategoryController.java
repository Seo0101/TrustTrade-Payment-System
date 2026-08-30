package org.example.trusttrade.item.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.item.domain.Category;
import org.example.trusttrade.item.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // 카테고리 종류
    @GetMapping("/list")
    public ResponseEntity<List<Category>> getCategoryList() {
        List<Category> categories = categoryService.getCategoryList();
        return ResponseEntity.ok(categories);
    }



}

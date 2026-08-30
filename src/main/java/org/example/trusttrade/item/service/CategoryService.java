package org.example.trusttrade.item.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.trusttrade.item.domain.Category;
import org.example.trusttrade.item.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getValidCategories(List<Integer> categoryIds) {
        return categoryIds.stream()
                .limit(3) // 최대 3개의 카테고리 설정
                .map(id -> categoryRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("카테고리 없음: ID = " + id)))
                .toList();
    }

    public List<Category> getCategoryList() {
        return categoryRepository.findAll();
    }





}

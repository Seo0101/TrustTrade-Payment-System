package org.example.trusttrade.item.repository;

import org.example.trusttrade.item.domain.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory,Integer> {
    List<ItemCategory> findByCategory_Id(Long categoryId);
}
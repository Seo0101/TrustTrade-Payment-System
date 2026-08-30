package org.example.trusttrade.item.repository;

import org.example.trusttrade.item.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category,Integer> {
}

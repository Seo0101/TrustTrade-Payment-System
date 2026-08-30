package org.example.trusttrade.item.repository;

import org.example.trusttrade.item.domain.products.ProductLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductLocationRepository extends JpaRepository<ProductLocation,Integer> {
}

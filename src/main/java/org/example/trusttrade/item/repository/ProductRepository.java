package org.example.trusttrade.item.repository;

import jakarta.persistence.LockModeType;
import org.example.trusttrade.item.domain.products.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 5km 이내의 모든 물품 조회
    @Query("SELECT p FROM Product p WHERE " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(p.productLocation.latitude)) " +
            "* cos(radians(p.productLocation.longitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(p.productLocation.latitude)))) <= 5")
    List<Product> findNearby(@Param("lat") double lat, @Param("lng") double lng);

    @Query("SELECT a FROM Auction a WHERE a.user.id = :sellerId")
    List<Product> getProductBySellerId(@Param("sellerId") UUID sellerId);

    @Query("""
        select p
        from Product p
        join fetch p.user u
        where lower(p.name) like lower(concat('%', :title, '%'))
    """)
    List<Product> findByTitleContainingWithSeller(@Param("title") String title);


    // 판매자(UUID)로 이 사람이 올린 상품만
    List<Product> findByUser_Id(UUID sellerId);

    //동시 reserve 요청 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

}



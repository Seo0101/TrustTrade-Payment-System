package org.example.trusttrade.item.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import org.example.trusttrade.item.domain.products.ProductLocation;
import org.example.trusttrade.item.dto.request.BasicItemDto;

import org.example.trusttrade.login.domain.User;


import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@SuperBuilder
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "item_type") // 하위 테이블 구분 컬럼 생성
@Table(name = "item")
public class Item {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User user;


    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "description", length = 200, nullable = false)
    private String description;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "trade_place_id")
    private ProductLocation productLocation;

    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;


    public static Item fromDto(BasicItemDto dto,User seller,ProductLocation productLocation){
        return Item.builder()
                .user(seller)
                .name(dto.getTitle())
                .description(dto.getDescription())
                .productLocation(productLocation)
                .createdTime(LocalDateTime.now())
                .build();
    }

    // 물품 구분
    @Transient
    public String getItemType() {
        return this.getClass().getAnnotation(DiscriminatorValue.class).value();
    }


}

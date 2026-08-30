package org.example.trusttrade.item.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="item_image_id")
    private Long id;

    @Column(name = "image")
    private String image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id",nullable = false)
    private Item item;

    @Column(name = "saved_time",nullable = false)
    private LocalDateTime savedTime;


    // 이미지 저장
    public static List<ItemImage> fromDto(Item item, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Collections.emptyList();
        }

        return imageUrls.stream()
                .limit(5) // 최대 5장
                .map(url -> ItemImage.builder()
                        .image(url)
                        .item(item)
                        .savedTime(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
    }

}

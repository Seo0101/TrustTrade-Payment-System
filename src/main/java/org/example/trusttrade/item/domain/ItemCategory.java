package org.example.trusttrade.item.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ItemCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="item_category_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="item_id",nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="category_id",nullable = false)
    private Category category;

    public static List<ItemCategory> createMappings(Item item, List<Category> categories) {
        return categories.stream()
                .map(category -> ItemCategory.builder()
                        .item(item)
                        .category(category)
                        .build())
                .toList();
    }
}

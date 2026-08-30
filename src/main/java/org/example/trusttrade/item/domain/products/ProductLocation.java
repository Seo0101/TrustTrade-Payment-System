package org.example.trusttrade.item.domain.products;

import jakarta.persistence.*;
import lombok.*;
import org.example.trusttrade.auction.dto.AuctionItemDto;
import org.example.trusttrade.item.dto.request.BasicItemDto;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="product_location_id")
    private Long id;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(length = 100,nullable = false)
    private String address;

    public static ProductLocation fromDto(BasicItemDto dto) {
        return ProductLocation.builder()
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .address(dto.getAddress())
                .build();
    }
    public static ProductLocation fromDto(AuctionItemDto dto) {
        return ProductLocation.builder()
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .address(dto.getAddress())
                .build();
    }

}

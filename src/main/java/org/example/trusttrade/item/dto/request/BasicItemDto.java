    package org.example.trusttrade.item.dto.request;

    import jakarta.validation.constraints.NotNull;
    import jakarta.validation.constraints.Size;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.util.List;
    import java.util.UUID;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public class BasicItemDto extends AbstractItemImageDto {

        @NotNull
        private String title;

        @NotNull
        private Integer price;

        @NotNull
        private String description;

        // 판매자 정보
        @NotNull
        private UUID sellerId;

        @Size(max = 3, message = "카테고리는 최대 3개까지 선택할 수 있습니다.")
        private List<Integer> categoryIds;

        // 주소
        @NotNull
        private String address;
        @NotNull
        private Double latitude;
        @NotNull
        private Double longitude;
    }
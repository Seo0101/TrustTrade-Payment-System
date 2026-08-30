package org.example.trusttrade.item.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {
    Long categoryId;
    String itemType; // "PRODUCT" or "AUCTION"
}
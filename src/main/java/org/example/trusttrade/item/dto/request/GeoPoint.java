package org.example.trusttrade.item.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GeoPoint {

    private Double lat;
    private Double lng;

}
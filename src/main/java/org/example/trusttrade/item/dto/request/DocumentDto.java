package org.example.trusttrade.item.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDto {

    @JsonProperty("address_name")
    private String addressName;

    @JsonProperty("x")
    private double longitude;

    @JsonProperty("y")
    private double latitude;

}
package org.example.trusttrade.business.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessStatusRequest {
    private List<Map<String, String>> businesses;
}
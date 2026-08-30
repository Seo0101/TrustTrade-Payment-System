package org.example.trusttrade.item.controller;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.item.dto.request.GeoPoint;
import org.example.trusttrade.item.dto.response.ProductResponseDto;
import org.example.trusttrade.item.service.ProductService;
import org.example.trusttrade.service.MapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class MapController {

    private final ProductService productService;
    private final MapService mapService;

    // 일반 물품 전체 조회 (사용자 주소 기반 5km 이내의 모든 일반 물품 조회)
    @GetMapping("/list")
    public ResponseEntity<List<ProductResponseDto>> getProductsOnMapByAddress(@RequestParam("address") String address) {
        GeoPoint geocode = mapService.addressToGeocode(address);
        List<ProductResponseDto> products = productService.findProductsNearby(geocode.getLat(), geocode.getLng());
        return ResponseEntity.ok(products);
    }
}
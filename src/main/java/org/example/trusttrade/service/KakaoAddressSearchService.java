package org.example.trusttrade.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.item.dto.response.KakaoApiResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAddressSearchService {

    private final RestTemplate restTemplate;
    private final KakaoUriBuilderService kakaoUriBuilderService;


    @Value("${geocoding.kakao.rest-api-key}")
    private String kakaoRestApiKey;

    // kakaoAPI에 생성된 URI HTTP로 호출하기
    public KakaoApiResponseDto requestAddressSearch(String address) {

        if(ObjectUtils.isEmpty(address)) return null;

        URI uri = kakaoUriBuilderService.buildUriByAddressSearch(address);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey);
        HttpEntity httpEntity = new HttpEntity(headers);

        // kakao api 호출
        return restTemplate.exchange(uri, HttpMethod.GET, httpEntity, KakaoApiResponseDto.class).getBody();
    }

    public double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(v));
    }

}
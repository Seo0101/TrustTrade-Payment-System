package org.example.trusttrade.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service
public class KakaoUriBuilderService {
    private static final String KAKAO_LOCAL_SEARCH_ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json";

    // 입력 받은 주소 -> KakaoAPI로 요청할 URI 생성
    public URI buildUriByAddressSearch(String address) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(KAKAO_LOCAL_SEARCH_ADDRESS_URL);

        uriBuilder.queryParam("query", address);

        URI uri = uriBuilder.build().encode().toUri();
        log.info("[KakaoApi에 요청할 uri 생성] address: {}, uri : {}", address, uri);

        return uri;
    }
}
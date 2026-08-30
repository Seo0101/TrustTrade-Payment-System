package org.example.trusttrade.login.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoLoginRequest {
    private String authorizationCode;
    private String redirectUri;
}
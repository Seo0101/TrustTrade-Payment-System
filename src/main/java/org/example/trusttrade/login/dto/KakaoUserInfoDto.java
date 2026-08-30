package org.example.trusttrade.login.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoUserInfoDto {
    private String email;
    private String name;
    private String profileImageUrl;
    private String kakaoId;
}
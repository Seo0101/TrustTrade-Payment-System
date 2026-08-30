package org.example.trusttrade.login.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UUID id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private boolean isRegistered;
}
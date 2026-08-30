package org.example.trusttrade.login.service;

import org.example.trusttrade.login.domain.User;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {
    public String createAccessToken(User user) {
        return "access-token-example";
    }
    public String createRefreshToken(User user) {
        return "refresh-token-example";
    }
}
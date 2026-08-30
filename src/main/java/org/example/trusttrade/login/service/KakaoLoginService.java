package org.example.trusttrade.login.service;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.domain.SocialLogin;
import org.example.trusttrade.login.dto.LoginResponse;
import org.example.trusttrade.login.dto.KakaoUserInfoDto;
import org.example.trusttrade.login.repository.UserRepository;
import org.example.trusttrade.login.repository.SocialLoginRepository;
import org.json.JSONException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private final KakaoAPIClient kakaoAPIClient;
    private final UserRepository userRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResponse kakaoLogin(String authorizationCode, String redirectUri) throws JSONException {
        String accessToken = kakaoAPIClient.getAccessToken(authorizationCode, redirectUri);
        KakaoUserInfoDto userInfo = kakaoAPIClient.getUserInfo(accessToken);

        Optional<SocialLogin> socialLoginOpt = socialLoginRepository.findByProviderAndProviderUserId(
                SocialLogin.Provider.KAKAO, userInfo.getKakaoId());
        User user;
        boolean isRegistered = socialLoginOpt.isPresent();
        if (isRegistered) {
            user = socialLoginOpt.get().getUser();
        } else {
            user = User.builder()
                    .id(UUID.randomUUID())
                    .email(userInfo.getEmail())
                    .profileImage(userInfo.getProfileImageUrl())
                    .role(User.Role.USER)
                    .memberType(User.MemberType.GENERAL)
                    .roughAddress("")
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);

            SocialLogin socialLogin = SocialLogin.builder()
                    .user(user)
                    .provider(SocialLogin.Provider.KAKAO)
                    .providerUserId(userInfo.getKakaoId())
                    .build();
            socialLoginRepository.save(socialLogin);
        }

        String jwtAccessToken = jwtProvider.createAccessToken(user);
        String jwtRefreshToken = jwtProvider.createRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(jwtAccessToken)
                .refreshToken(jwtRefreshToken)
                .id(user.getId())
                .email(user.getEmail())
                .profileImageUrl(userInfo.getProfileImageUrl())
                .isRegistered(isRegistered)
                .build();
    }
}
    package org.example.trusttrade.login.controller;

    import lombok.RequiredArgsConstructor;
    import org.example.trusttrade.login.dto.LoginResponse;
    import org.example.trusttrade.login.service.KakaoLoginService;
    import org.json.JSONException;
    import org.springframework.http.HttpHeaders;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.util.UriComponentsBuilder;

    import java.net.URI;
    import java.net.URLEncoder;
    import java.nio.charset.StandardCharsets;

    @RestController
    @RequestMapping("/auth")
    @RequiredArgsConstructor
    public class KakaoLoginController {

        private final KakaoLoginService kakaoLoginService;

        @GetMapping("/kakao/login")
        public ResponseEntity<Void> kakaoLogin(@RequestParam("code") String code) throws JSONException {
            String redirectUri = "http://54.66.146.131:8080/auth/kakao/login";
            LoginResponse response = kakaoLoginService.kakaoLogin(code, redirectUri);

            URI redirect = UriComponentsBuilder
                    .fromUriString("http://localhost:5173/oauth/kakao")
                    .queryParam("UUID", response.getId())
                    .queryParam("accessToken", response.getAccessToken())
                    .queryParam("refreshToken", response.getRefreshToken())
                    .queryParam("email", response.getEmail())
                    .queryParam("nickname", response.getNickname())
                    .queryParam("profileImageUrl", URLEncoder.encode(response.getProfileImageUrl(), StandardCharsets.UTF_8))
                    .queryParam("registered", response.isRegistered())
                    .build(true)
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(redirect);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }
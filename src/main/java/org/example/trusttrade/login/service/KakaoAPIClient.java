package org.example.trusttrade.login.service;

import org.example.trusttrade.login.dto.KakaoUserInfoDto;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.json.JSONObject;

@Component
public class KakaoAPIClient {

    @Value("${kakao.api.service-key}")
    private String clientId;
    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken(String code, String redirectUri) throws JSONException {
        String url = "https://kauth.kakao.com/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=authorization_code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&code=" + code;
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        JSONObject json = new JSONObject(response.getBody());
        return json.getString("access_token");
    }

    public KakaoUserInfoDto getUserInfo(String accessToken) throws JSONException {
        String url = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        JSONObject json = new JSONObject(response.getBody());
        JSONObject kakaoAccount = json.getJSONObject("kakao_account");
        String email = kakaoAccount.getString("email");
        String name = kakaoAccount.getString("name");
        String profileImageUrl = kakaoAccount.getJSONObject("profile").optString("profile_image_url", "");
        String kakaoId = json.get("id").toString();
        return new KakaoUserInfoDto(email, name, profileImageUrl, kakaoId);
    }
}
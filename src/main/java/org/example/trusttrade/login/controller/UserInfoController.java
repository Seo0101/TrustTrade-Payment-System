package org.example.trusttrade.login.controller;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.login.dto.UserInfoResponse;
import org.example.trusttrade.login.service.UserInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/userinfo")
@RequiredArgsConstructor
public class UserInfoController {
    private final UserInfoService userInfoService;

    @GetMapping("{uuid}")
    public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable UUID uuid) {
        UserInfoResponse userInfoResponse = userInfoService.getUserInfo(uuid);


        return ResponseEntity.ok(userInfoResponse);
    }
}

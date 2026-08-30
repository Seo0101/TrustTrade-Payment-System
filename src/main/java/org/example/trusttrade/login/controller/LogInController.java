package org.example.trusttrade.login.controller;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.item.dto.request.LogInRequest;
import org.example.trusttrade.login.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LogInController {

    private final UserService userService;
    @PostMapping("")
    public ResponseEntity<String> userLogin(@RequestBody LogInRequest request) {
        try{
            userService.userLogin(request);
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("아이디나 비밀번호를 다시 입력해주세요");
        }
        return ResponseEntity.ok("로그인 성공");
    }

}
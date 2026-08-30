package org.example.trusttrade.item.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.item.dto.request.AccountDto;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.dto.SignUpRequest;
import org.example.trusttrade.login.dto.SignUpResponse;
import org.example.trusttrade.login.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // 아이디 중복 체크
    @PostMapping("/account/verify-duplicate")
    public ResponseEntity<String> verifyAccountDuplicate(@RequestBody AccountDto request) {
        try {
            userService.verifyAccountDuplicate(request.getAccount());
            return ResponseEntity.ok("사용 가능한 아이디 입니다");
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 아이디입니다");
        }
    }

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequest request) {
        try {
            User saved = userService.signUp(request);

            SignUpResponse response = new SignUpResponse(
                    saved.getUserAccount(),
                    saved.getId()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (DataIntegrityViolationException e) {
            log.warn("회원가입 실패 - 중복 계정: {}", request.getAccount(), e);
            // 에러도 통일된 DTO로 내보내고 싶으면 별도 ErrorResponse 만들면 됨
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "이미 존재하는 회원입니다."));
        } catch (Exception e) {
            log.error("회원가입 실패 - 알 수 없는 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "회원가입 중 오류가 발생했습니다."));
        }
    }

}
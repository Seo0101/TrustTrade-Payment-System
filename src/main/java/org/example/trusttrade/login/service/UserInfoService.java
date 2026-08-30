package org.example.trusttrade.login.service;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.dto.UserInfoResponse;
import org.example.trusttrade.login.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserRepository userRepository;

    public UserInfoResponse getUserInfo(UUID id) {
        Optional<User> optionalUser = userRepository.findById(id);

        User user = optionalUser.orElseThrow(() ->
                new IllegalArgumentException("해당 UUID의 유저가 없음: " + id));

        return UserInfoResponse.builder()
                .id(user.getId())
                .userAccount(Optional.ofNullable(user.getUserAccount()).orElse(""))
                .userPw(Optional.ofNullable(user.getUserPw()).orElse(""))
                .email(Optional.ofNullable(user.getEmail()).orElse(""))
                .profileImage(Optional.ofNullable(user.getProfileImage()).orElse(""))
                .role(user.getRole())
                .memberType(user.getMemberType())
                .businessNumber(Optional.ofNullable(user.getBusinessNumber()).orElse(""))
                .roughAddress(Optional.ofNullable(user.getRoughAddress()).orElse(""))
                .build();
    }
}
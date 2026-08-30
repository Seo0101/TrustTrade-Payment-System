package org.example.trusttrade.login.dto;

import lombok.*;
import org.example.trusttrade.login.domain.User;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private UUID id;
    private String userAccount;
    private String userPw;
    private String email;
    private String profileImage;
    private User.Role role;
    private User.MemberType memberType;
    private String businessNumber;
    private String roughAddress;
}

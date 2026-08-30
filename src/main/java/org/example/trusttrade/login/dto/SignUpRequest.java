package org.example.trusttrade.login.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignUpRequest {

    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    private String account;

    @NotBlank(message = "이메일은 필수 입력값 입니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    private String password;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String userName;

    @NotBlank(message = "전화번호는 필수 입력 값입니다.")
    private String telephone;

    @NotBlank(message = "주소는 필수 입력값 입니다.")
    private String roughAddress;

}
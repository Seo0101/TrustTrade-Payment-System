package org.example.trusttrade.login.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.UUID;

@Entity
@Table(name = "social_login")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLogin {

    @Id
    @Column(name = "user_id", nullable = false,columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    public enum Provider {
        GOOGLE, KAKAO
    }
}
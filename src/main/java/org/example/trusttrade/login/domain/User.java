package org.example.trusttrade.login.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.example.trusttrade.domain.SellerAccount;
import org.example.trusttrade.domain.Settlement;
import org.example.trusttrade.item.domain.products.ProductLocation;
import org.example.trusttrade.login.dto.SignUpRequest;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name="user_account",unique = true)
    private String userAccount;

    @Column(name = "userPw")
    private String userPw;

    @Column(name = "email")
    private String email;

    @Column(name = "profile_image", columnDefinition = "TEXT")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false)
    private MemberType memberType;

    @Column(name = "business_number", unique = true)
    private String businessNumber;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 사용자 주소
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_location")
    private ProductLocation user_location;

    @Column(name = "rough_address", nullable = false)
    private String roughAddress;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private SocialLogin socialLogin;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerAccount sellerAccount;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Settlement> settlements;

    public static User createUser(SignUpRequest request, ProductLocation userLocation) {
        return User.builder()
                .userAccount(request.getAccount())
                .userPw(request.getPassword())
                .email(request.getEmail())
                .roughAddress(request.getRoughAddress())
                .user_location(userLocation)
                .role(Role.USER)
                .memberType(MemberType.GENERAL)
                .build();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }
    public enum Role {
        USER, ADMIN
    }

    public enum MemberType {
        GENERAL, BUSINESS
    }

}

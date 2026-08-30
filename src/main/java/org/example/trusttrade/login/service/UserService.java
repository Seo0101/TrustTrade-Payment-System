package org.example.trusttrade.login.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.trusttrade.global.error.AddressNotFoundException;
import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.domain.products.ProductLocation;
import org.example.trusttrade.item.dto.request.GeoPoint;
import org.example.trusttrade.item.dto.request.LogInRequest;
import org.example.trusttrade.item.repository.ProductLocationRepository;
import org.example.trusttrade.login.dto.SignUpRequest;
import org.example.trusttrade.service.KakaoAddressSearchService;
import org.example.trusttrade.service.MapService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final KakaoAddressSearchService kakaoAddressSearchService;
    private final MapService mapService;
    private final ProductLocationRepository productLocationRepository;
    // user 권한 조회
    public User validateBusinessUser(UUID userId) {
        log.debug("validateBusinessUser 시작: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User 조회 실패: userId={} (EntityNotFoundException 발생)", userId);
                    return new EntityNotFoundException("회원 정보를 찾을 수 없습니다.");
                });

        log.debug("User 조회 성공: id={}, memberType={}", user.getId(), user.getMemberType());

        if (user.getMemberType() != User.MemberType.BUSINESS) {
            log.warn("권한 검증 실패: userId={}, memberType={} (BUSINESS 아님)", userId, user.getMemberType());
            throw new IllegalStateException("Business 회원만 상품 등록이 가능합니다.");
        }

        log.debug("권한 검증 통과: userId={} is BUSINESS", userId);
        return user;
    }

    // 아이디 중복 체크
    public void verifyAccountDuplicate(String account) {

        log.debug("계정 중복 체크 요청 시작 account = {}", account);
        if (userRepository.findByUserAccount(account).isPresent()) {
            throw new DataIntegrityViolationException("이미 존재하는 계정입니다: " + account);
        }
        log.debug("계정 중복 체크 완료. account = {}", account);

    }

    @Transactional
    public User signUp(SignUpRequest request) {

        ProductLocation loc = null;

        try {
            GeoPoint geocode = mapService.addressToGeocode(request.getRoughAddress());

            loc = ProductLocation.builder()
                    .address(request.getRoughAddress())
                    .latitude(geocode.getLat())
                    .longitude(geocode.getLng())
                    .build();

            productLocationRepository.save(loc);
        } catch (AddressNotFoundException e) { // 주소를 못 찾은 경우
            // 주소 변환만 실패했을 뿐이므로 가입은 진행
            log.warn("주소 좌표 변환 실패. address={}", request.getRoughAddress());
        }

        User user = User.createUser(request, loc);
        user.setId(UUID.randomUUID());
        return userRepository.save(user);
    }




    // 로그인
    public void userLogin(LogInRequest request) {

        // 아이디 존재 여부 확인
        User user = userRepository.findByUserAccount(request.getAccount())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 아이디 또는 비밀번호가 일치하지 않는 경우
        if (!user.getUserPw().equals(request.getPassword()) || !user.getUserAccount().equals(request.getAccount())) {
            throw new IllegalArgumentException();
        }
    }



    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByBusinessNumber(String businessNumber) {
        return userRepository.findByBusinessNumber(businessNumber);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByBusinessNumber(String businessNumber) {
        return userRepository.existsByBusinessNumber(businessNumber);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void softDelete(UUID id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setIsDeleted(true);
            user.setDeletedAt(java.time.LocalDateTime.now());
        });
    }

}
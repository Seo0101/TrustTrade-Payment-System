package org.example.trusttrade.business.service;

import lombok.RequiredArgsConstructor;
import org.example.trusttrade.business.dto.BusinessStatusRequest;
import org.example.trusttrade.business.dto.BusinessStatusResponse;
import org.example.trusttrade.login.domain.User;
import org.example.trusttrade.login.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.web.util.UriComponentsBuilder.fromUri;

@Service
@RequiredArgsConstructor
public class BusinessVerifyService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${nts.api.url}")
    private String apiUrl;

    @Value("${nts.api.service-key}")
    private String serviceKey;

    @Transactional
    public void certifyBusiness(UUID userId, String businessNumber, String startDt, String pNm) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getMemberType() == User.MemberType.BUSINESS) {
            throw new IllegalStateException("이미 사업자 회원입니다.");
        }

        if (userRepository.existsByBusinessNumber(businessNumber)) {
            throw new IllegalStateException("이미 등록된 사업자등록번호입니다.");
        }

        if (!isValidBusiness(businessNumber, startDt, pNm)) {
            throw new IllegalArgumentException("유효하지 않은 사업자 정보입니다.");
        }

        user.setBusinessNumber(businessNumber);
        user.setMemberType(User.MemberType.BUSINESS);

    }

    private boolean isValidBusiness(String bNo, String startDt, String pNm) {

        URI uri = fromUri(URI.create(apiUrl))
                .queryParam("serviceKey", serviceKey)
                .build(true)
                .toUri();

        Map<String, String> business = Map.of(
                "b_no", bNo,
                "start_dt", startDt,
                "p_nm", pNm,
                "p_nm2", "",
                "b_nm", "",
                "corp_no", "",
                "b_sector", "",
                "b_type", ""
        );

        BusinessStatusRequest request = new BusinessStatusRequest(List.of(business));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<BusinessStatusRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<BusinessStatusResponse> response = restTemplate.exchange(
                uri, HttpMethod.POST, entity, BusinessStatusResponse.class
        );

        List<Map<String, Object>> data = response.getBody().getData();
        if (data == null || data.isEmpty()) return false;

        return "01".equals(data.get(0).get("b_stt_cd"));
    }
}
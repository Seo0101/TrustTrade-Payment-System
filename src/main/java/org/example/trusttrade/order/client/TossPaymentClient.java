package org.example.trusttrade.order.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trusttrade.order.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TossPaymentClient {


    @Value("${payment.toss.test-secret-key}")
    private String widgetSecretKey;
    private final ObjectMapper jacksonObjectMapper;

    public TossPaymentClient(ObjectMapper jacksonObjectMapper) {
        this.jacksonObjectMapper = jacksonObjectMapper;
    }

    //결제 PG 조회
    public HttpResponse<String> checkState(String paymentKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.tosspayments.com/v1/payments/" + paymentKey))
                    .header("Authorization", getAuthorizations())
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new ExternalApiException(
                    "토스 결제 조회 API 요청이 인터럽트되었습니다.",
                    e
            );
        } catch (IOException e) {
            throw new ExternalApiException("토스 결제 조회 API 요청 중 오류 발생", e);
        }
    }

    //결제 승인 요청
    public HttpResponse<String> requestConfirm(int amount, String orderId, String paymentKey) {

        //JSON 객체 생성
        JsonNode requestObj = jacksonObjectMapper.createObjectNode()
                .put("orderId", orderId)
                .put("amount", String.valueOf(amount))
                .put("paymentKey", paymentKey);

        //JSON 객체 문자열 변환, 결제 승인 요청
        try {
            String requestBody = jacksonObjectMapper.writeValueAsString(requestObj);
            System.out.println("Sending request to Toss: " + requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.tosspayments.com/v1/payments/confirm"))
                    .header("Authorization", getAuthorizations())
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());


        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new ExternalApiException("토스 결제 인증 API 요청이 인터럽트되었습니다.",
                    e);
        }
        catch (IOException e) {
            throw new ExternalApiException("토스 결제 인증 API 요청 중 오류 발생", e);
        }
    }

    //결제 취소 요청(db 작업 에러시 사용)
    public HttpResponse requestPaymentCancel(String paymentKey, String cancelReason) {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel"))
                    .header("Authorization", getAuthorizations())
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString("{\"cancelReason\" : \"" + cancelReason + "\"}"))
                    .build();

            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new ExternalApiException("토스 취소 API 요청이 인터럽트되었습니다.", e);
        }
        catch (IOException e) {
            throw new ExternalApiException("토스 취소 API 요청 중 오류 발생", e);
        }

    }

    //테스트용 인증키
    private String getAuthorizations() {

        if (widgetSecretKey == null || widgetSecretKey.isBlank()) {
            throw new IllegalStateException("TOSS_WIDGET_SECRET_KEY 설정이 비어있습니다.");
        }

        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodedBytes = encoder.encode((widgetSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        String authorizations = "Basic " + new String(encodedBytes);

        return authorizations;
    }
}
package org.example.trusttrade.order.domain;

import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.dto.request.BasicItemDto;
import org.example.trusttrade.login.domain.User;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentTest {

    @Test
    public void 결제_생성_상태_확인(){

        //given
        User buyer = User.builder()
                .userAccount("buyer")
                .userPw("password1")
                .email("buyer@test.com")
                .roughAddress("서울시")
                .user_location(null)
                .role(User.Role.USER)
                .memberType(User.MemberType.GENERAL)
                .build();
        buyer.setId(UUID.randomUUID());

        User seller = User.builder()
                .userAccount("seller")
                .userPw("password2")
                .email("seller@test.com")
                .roughAddress("서울시")
                .user_location(null)
                .role(User.Role.USER)
                .memberType(User.MemberType.GENERAL)
                .build();
        seller.setId(UUID.randomUUID());

        BasicItemDto productDto = BasicItemDto.builder()
                .title("테스트 상품")
                .price(10000)
                .description("테스트 상품 설명")
                .sellerId(seller.getId())
                .categoryIds(List.of(1))
                .address("서울특별시 서초구")
                .latitude(37.4836)
                .longitude(127.0327)
                .build();

        Product product = Product.fromDto(productDto, seller);

        String orderLockKey = "test-lock-key";

        product.validateOrderable(buyer.getId());
        product.reserve(buyer);

        Order order = Order.create(product, buyer, seller, orderLockKey);

        //when
        order.startPayment();

        Payment payment = Payment.create(order, "test_idempotencyKey");

        //then
        assertThat(payment.getOrder())
                .isEqualTo(order);

        assertThat(payment.getOrder().getAmount())
                .isEqualTo(order.getAmount());

        assertThat(payment.getOrder().getStatus())
                .isEqualTo(Order.Status.PENDING);

        assertThat(payment.getStatus())
                .isEqualTo(Payment.Status.READY);

        assertThat(payment.getIdempotencyKey())
                .isEqualTo("test_idempotencyKey");






    }
}

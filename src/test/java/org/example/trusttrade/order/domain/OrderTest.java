package org.example.trusttrade.order.domain;


import org.example.trusttrade.item.domain.products.Product;
import org.example.trusttrade.item.dto.request.BasicItemDto;
import org.example.trusttrade.login.domain.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderTest {


    @Test
    public void 주문_생성_상태_확인(){

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

        //when


        Order order = Order.create(product, buyer, seller, orderLockKey);


        //then
        assertThat(order.getStatus())
                .isEqualTo(Order.Status.CREATED);

        assertThat(order.getProduct())
                .isEqualTo(product);

        assertThat(order.getBuyer())
                .isEqualTo(buyer);

        assertThat(order.getAmount())
                .isEqualTo(product.getProductPrice());

        assertThat(order.getOrderLockKey())
                .isEqualTo(orderLockKey);
    }

}

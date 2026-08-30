package org.example.trusttrade.order.exception;

public class ProductNotFoundException extends NotFoundException {
    public ProductNotFoundException(Long productId) {
        super("상품 정보를 찾을 수 없습니다. productId = " + productId);
    }
}

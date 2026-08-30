package org.example.trusttrade.order.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/orders")
public class OrderPageController {

    //상태 기반 결과 처리 페이지 라우팅
    @GetMapping("/{orderId}/detail")
    public String orderPage(@PathVariable("orderId") String orderId,
                            @RequestParam(value = "idempotencyKey", required = false)
                            String idempotencyKey,
                            Model model) {

        System.out.println("ORDER DETAIL PAGE = " + orderId);

        model.addAttribute("orderId", orderId);
        model.addAttribute("idempotencyKey", idempotencyKey);

        return "order-detail";// templates/order-detail.html
    }
}

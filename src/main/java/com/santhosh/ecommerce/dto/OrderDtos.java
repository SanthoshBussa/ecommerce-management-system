package com.santhosh.ecommerce.dto;

import com.santhosh.ecommerce.entity.Order;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull
        private Long productId;

        @NotNull
        @Min(1)
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderRequest {
        @NotEmpty(message = "Order must contain at least one item")
        private List<OrderItemRequest> items;

        @NotBlank(message = "Shipping address is required")
        private String shippingAddress;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentVerifyRequest {
        @NotBlank
        private String orderNumber;

        @NotBlank
        private String razorpayOrderId;

        @NotBlank
        private String razorpayPaymentId;

        @NotBlank
        private String razorpaySignature;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingStep {
        private String status;
        private String label;
        private boolean completed;
        private boolean current;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderTrackingResponse {
        private String orderNumber;
        private Order.OrderStatus currentStatus;
        private Order.PaymentStatus paymentStatus;
        private String trackingNumber;
        private String courierPartner;
        private String shippingAddress;
        private BigDecimal totalAmount;
        private LocalDateTime orderedAt;
        private LocalDateTime estimatedDeliveryDate;
        private List<TrackingStep> timeline;
    }
}

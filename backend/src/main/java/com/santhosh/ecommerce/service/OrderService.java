package com.santhosh.ecommerce.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.santhosh.ecommerce.dto.OrderDtos.*;
import com.santhosh.ecommerce.entity.Order;
import com.santhosh.ecommerce.entity.OrderItem;
import com.santhosh.ecommerce.entity.Product;
import com.santhosh.ecommerce.entity.User;
import com.santhosh.ecommerce.repository.OrderRepository;
import com.santhosh.ecommerce.repository.ProductRepository;
import com.santhosh.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency}")
    private String currency;

    @Transactional
    public Order createOrder(String userEmail, CreateOrderRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .orderStatus(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .trackingNumber("TRK-" + System.currentTimeMillis())
                .courierPartner("BlueDart Express")
                .estimatedDeliveryDate(LocalDateTime.now().plusDays(4))
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.getProductId()));

            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }

            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            BigDecimal effectivePrice = product.getDiscountPrice() != null
                    ? product.getDiscountPrice()
                    : product.getPrice();
            BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(effectivePrice)
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);

        // Initialize Razorpay order ID (or simulated ID in local dev mode)
        try {
            if (!razorpayKeyId.contains("placeholder")) {
                RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", totalAmount.multiply(BigDecimal.valueOf(100)).intValue());
                orderRequest.put("currency", currency);
                orderRequest.put("receipt", orderNumber);
                com.razorpay.Order rzpOrder = razorpay.orders.create(orderRequest);
                order.setRazorpayOrderId(rzpOrder.get("id"));
            } else {
                order.setRazorpayOrderId("order_sim_" + UUID.randomUUID().toString().substring(0, 10));
            }
        } catch (Exception e) {
            order.setRazorpayOrderId("order_sim_" + UUID.randomUUID().toString().substring(0, 10));
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order verifyPayment(PaymentVerifyRequest request) {
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderNumber()));

        try {
            if (!razorpayKeySecret.contains("placeholder")) {
                JSONObject options = new JSONObject();
                options.put("razorpay_order_id", request.getRazorpayOrderId());
                options.put("razorpay_payment_id", request.getRazorpayPaymentId());
                options.put("razorpay_signature", request.getRazorpaySignature());
                boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
                if (!isValid) {
                    order.setPaymentStatus(Order.PaymentStatus.FAILED);
                    orderRepository.save(order);
                    throw new IllegalArgumentException("Invalid Razorpay payment signature");
                }
            }
            order.setRazorpayPaymentId(request.getRazorpayPaymentId());
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setOrderStatus(Order.OrderStatus.CONFIRMED);
            return orderRepository.save(order);
        } catch (Exception ex) {
            throw new IllegalStateException("Payment verification failed: " + ex.getMessage());
        }
    }

    public List<Order> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public OrderTrackingResponse trackOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        List<Order.OrderStatus> stages = List.of(
                Order.OrderStatus.PLACED,
                Order.OrderStatus.CONFIRMED,
                Order.OrderStatus.PACKED,
                Order.OrderStatus.SHIPPED,
                Order.OrderStatus.OUT_FOR_DELIVERY,
                Order.OrderStatus.DELIVERED
        );

        int currentIdx = stages.indexOf(order.getOrderStatus());
        List<TrackingStep> timeline = new ArrayList<>();

        for (int i = 0; i < stages.size(); i++) {
            Order.OrderStatus stage = stages.get(i);
            timeline.add(TrackingStep.builder()
                    .status(stage.name())
                    .label(stage.name().replace("_", " "))
                    .completed(currentIdx >= i)
                    .current(currentIdx == i)
                    .build());
        }

        return OrderTrackingResponse.builder()
                .orderNumber(order.getOrderNumber())
                .currentStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .trackingNumber(order.getTrackingNumber())
                .courierPartner(order.getCourierPartner())
                .shippingAddress(order.getShippingAddress())
                .totalAmount(order.getTotalAmount())
                .orderedAt(order.getCreatedAt())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .timeline(timeline)
                .build();
    }

    @Transactional
    public Order updateOrderStatus(String orderNumber, Order.OrderStatus newStatus) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        order.setOrderStatus(newStatus);
        return orderRepository.save(order);
    }
}

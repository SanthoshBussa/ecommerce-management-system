package com.santhosh.ecommerce;

import com.santhosh.ecommerce.dto.OrderDtos.OrderTrackingResponse;
import com.santhosh.ecommerce.entity.Order;
import com.santhosh.ecommerce.repository.OrderRepository;
import com.santhosh.ecommerce.repository.ProductRepository;
import com.santhosh.ecommerce.repository.UserRepository;
import com.santhosh.ecommerce.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcommerceApplicationTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void testTrackOrderTimelineProgress() {
        Order mockOrder = Order.builder()
                .orderNumber("ORD-TEST101")
                .orderStatus(Order.OrderStatus.SHIPPED)
                .paymentStatus(Order.PaymentStatus.PAID)
                .trackingNumber("TRK-998877")
                .courierPartner("BlueDart Express")
                .shippingAddress("Hitech City, Hyderabad, Telangana")
                .totalAmount(new BigDecimal("4999.00"))
                .createdAt(LocalDateTime.now().minusDays(1))
                .estimatedDeliveryDate(LocalDateTime.now().plusDays(2))
                .build();

        when(orderRepository.findByOrderNumber("ORD-TEST101")).thenReturn(Optional.of(mockOrder));

        OrderTrackingResponse response = orderService.trackOrder("ORD-TEST101");

        assertEquals("ORD-TEST101", response.getOrderNumber());
        assertEquals(Order.OrderStatus.SHIPPED, response.getCurrentStatus());
        assertEquals(6, response.getTimeline().size());
        assertTrue(response.getTimeline().get(0).isCompleted());
        assertTrue(response.getTimeline().get(3).isCurrent());
        assertFalse(response.getTimeline().get(5).isCompleted());
    }
}

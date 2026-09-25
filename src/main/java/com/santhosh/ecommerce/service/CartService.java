package com.santhosh.ecommerce.service;

import com.santhosh.ecommerce.entity.Cart;
import com.santhosh.ecommerce.entity.CartItem;
import com.santhosh.ecommerce.entity.Product;
import com.santhosh.ecommerce.entity.User;
import com.santhosh.ecommerce.repository.CartRepository;
import com.santhosh.ecommerce.repository.ProductRepository;
import com.santhosh.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Cart getOrCreateCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .user(user)
                        .totalPrice(BigDecimal.ZERO)
                        .build()));
    }

    @Transactional
    public Cart addItemToCart(String userEmail, Long productId, int quantity) {
        Cart cart = getOrCreateCart(userEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (product.getStockQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + product.getName());
        }

        BigDecimal unitPrice = product.getDiscountPrice() != null
                ? product.getDiscountPrice()
                : product.getPrice();

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int updatedQty = item.getQuantity() + quantity;
            item.setQuantity(updatedQty);
            item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(updatedQty)));
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                    .build();
            cart.getItems().add(newItem);
        }

        recalculateCartTotal(cart);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItemFromCart(String userEmail, Long productId) {
        Cart cart = getOrCreateCart(userEmail);
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        recalculateCartTotal(cart);
        return cartRepository.save(cart);
    }

    private void recalculateCartTotal(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalPrice(total);
    }
}

package com.example.ecommerce;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public CartController(CartRepository cartRepository,
                          ProductRepository productRepository,
                          OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{userId}")
    public Cart getCart(@PathVariable Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUserId(userId);
            return cartRepository.save(cart);
        });
    }

    @PostMapping("/{userId}/add")
    public Cart addItem(@PathVariable Long userId, @RequestParam Long productId, @RequestParam int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            return cartRepository.save(newCart);
        });

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }

        return cartRepository.save(cart);
    }

    @GetMapping("/{userId}/total")
    public Map<String, BigDecimal> getTotal(@PathVariable Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        BigDecimal total = cartRepository.calculateCartTotal(cart.getId());
        return Map.of("totalAmount", total);
    }

    // Checkout: Stock check + Order generate + Cart clear
    @PostMapping("/{userId}/checkout")
    @Transactional
    public Order checkout(@PathVariable Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty! Cannot checkout.");
        }

        Order order = new Order();
        order.setUserId(userId);

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            int updatedRows = productRepository.decreaseStock(item.getProduct().getId(), item.getQuantity());
            if (updatedRows == 0) {
                throw new RuntimeException("Stock ran out for: " + item.getProduct().getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(item.getProduct().getId());
            orderItem.setProductName(item.getProduct().getName());
            orderItem.setPriceAtPurchase(item.getProduct().getPrice());
            orderItem.setQuantity(item.getQuantity());

            order.getItems().add(orderItem);

            BigDecimal lineTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);
        }

        order.setTotalAmount(total);

        // 1. Order save karo
        Order savedOrder = orderRepository.save(order);

        // 2. Cart empty karo
        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }

    // User ki order history dekhne ke liye
    @GetMapping("/{userId}/orders")
    public List<Order> getOrders(@PathVariable Long userId) {
        return orderRepository.findByUserId(userId);
    }
}
package com.example.ecommerce;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String rawPassword = request.get("password");

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already in use!");
        }

        User user = new User(email, passwordEncoder.encode(rawPassword), "CUSTOMER");
        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(email);

        return Map.of(
            "message", "User registered successfully!",
            "userId", savedUser.getId(),
            "token", token
        );
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String rawPassword = request.get("password");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        String token = jwtUtil.generateToken(email);

        return Map.of(
            "message", "Login successful!",
            "userId", user.getId(),
            "token", token
        );
    }
}
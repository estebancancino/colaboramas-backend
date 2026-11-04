package com.agora.auth.controller;

import com.agora.auth.dto.AuthCreateUserRequest;
import com.agora.auth.dto.AuthLoginRequest;
import com.agora.auth.dto.AuthResponse;
import com.agora.auth.service.AuthService;
import com.agora.user.dto.UserResponseDTO;
import com.agora.user.model.User;
import com.agora.user.repository.UserRepository;
import com.agora.user.service.UserService;
import com.agora.util.JwtUtils;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    // Este archivo actúa como controlador para endpoints de autenticación.
    // La configuración de seguridad real está en `com.agora.config.SecurityConfig`.

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthLoginRequest userRequest) {
        try {
            AuthResponse response = authService.loginUser(userRequest);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, ex.getMessage(), null, false));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse(null, ex.getMessage(), null, false));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid AuthCreateUserRequest request) {
        AuthResponse response = authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No authenticated user");
        }

        String username = auth.getName();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No user information");
        }

        // usar userRepository existente (devuelve Optional<User>)
        var userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        return ResponseEntity.ok(userOpt.get());
    }

    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.replace("Bearer ", "");
            DecodedJWT decodedJWT = jwtUtils.validateToken(jwt);
            String email = jwtUtils.extractUsername(decodedJWT);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!user.getIsEnabled()) {
                throw new RuntimeException("User is disabled");
            }

            Claim authoritiesClaim = jwtUtils.getSpecificClaim(decodedJWT, "authorities");
            String authorities = authoritiesClaim.asString();

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("email", email);
            response.put("authorities", Arrays.asList(authorities.split(",")));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}
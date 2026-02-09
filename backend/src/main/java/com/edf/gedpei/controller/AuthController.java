package com.edf.gedpei.controller;

import com.edf.gedpei.dto.AuthDTO;
import com.edf.gedpei.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controleur d'authentification.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API d'authentification")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "Authentification et generation de token JWT")
    public ResponseEntity<AuthDTO.LoginResponse> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(userService.authenticate(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Enregistrement d'un nouvel utilisateur")
    public ResponseEntity<AuthDTO.UserInfo> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Informations de l'utilisateur connecte")
    public ResponseEntity<AuthDTO.UserInfo> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(AuthDTO.UserInfo.builder()
                .username(userDetails.getUsername())
                .roles(userDetails.getAuthorities().stream()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .collect(java.util.stream.Collectors.toSet()))
                .build());
    }
}

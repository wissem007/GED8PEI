package com.edf.gedpei.service;

import com.edf.gedpei.dto.AuthDTO;
import com.edf.gedpei.entity.User;
import com.edf.gedpei.repository.UserRepository;
import com.edf.gedpei.security.JwtService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service de gestion des utilisateurs.
 */
@Service
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Autowired
    public void setPasswordEncoder(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true, true, true,
                user.getRolesSet().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList())
        );
    }

    /**
     * Authentifie un utilisateur et retourne un token JWT.
     */
    public AuthDTO.LoginResponse authenticate(AuthDTO.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve"));

        // Mettre a jour la date de derniere connexion
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        return AuthDTO.LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(user.getRolesSet())
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    /**
     * Enregistre un nouvel utilisateur.
     */
    public AuthDTO.UserInfo register(AuthDTO.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Ce nom d'utilisateur existe deja");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .roles("USER")
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("Utilisateur cree: {}", user.getUsername());

        return AuthDTO.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRolesSet())
                .enabled(user.getEnabled())
                .build();
    }

    /**
     * Initialise les utilisateurs par defaut.
     */
    @PostConstruct
    public void initDefaultUsers() {
        if (userRepository.count() == 0) {
            // Creer un admin par defaut
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@edf.fr")
                    .fullName("Administrateur")
                    .roles("ADMIN,USER")
                    .enabled(true)
                    .build();
            userRepository.save(admin);

            // Creer un utilisateur standard
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .email("user@edf.fr")
                    .fullName("Utilisateur")
                    .roles("USER")
                    .enabled(true)
                    .build();
            userRepository.save(user);

            log.info("Utilisateurs par defaut crees (admin/admin123, user/user123)");
        }
    }
}

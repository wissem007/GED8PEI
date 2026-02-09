package com.edf.gedpei.config;

import com.edf.gedpei.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de la securite.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, @Lazy UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                        // Lecture autorisee pour tous (consultation publique)
                        .requestMatchers(HttpMethod.GET, "/api/servers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/server-software/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/export/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/software-versions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/migrations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/server-obsolescence/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/alerts/**").permitAll()
                        // Alertes - actions (temporairement public)
                        .requestMatchers("/api/alerts/generate").permitAll()
                        .requestMatchers("/api/alerts/*/acknowledge").permitAll()
                        .requestMatchers("/api/alerts/*/resolve").permitAll()
                        .requestMatchers("/api/alerts/*/ignore").permitAll()
                        .requestMatchers("/api/alerts/clear-resolved").permitAll()
                        .requestMatchers("/api/alerts/clear-all").permitAll()
                        // Import CSV et gestion des versions logiciels (temporairement public)
                        .requestMatchers("/api/software-versions/import").permitAll()
                        .requestMatchers("/api/server-obsolescence/import").permitAll()
                        .requestMatchers("/api/server-obsolescence/clear-all").permitAll()
                        .requestMatchers("/api/server-obsolescence/recreate-table").permitAll()
                        .requestMatchers("/api/software-versions/clear-all").permitAll()
                        .requestMatchers("/api/software-versions/update-schema").permitAll()

                        // Ecriture necessite une authentification
                        .requestMatchers(HttpMethod.POST, "/api/servers/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/servers/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/servers/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/software-versions/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/software-versions/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/software-versions/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/migrations/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/migrations/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/migrations/**").authenticated()
                        .requestMatchers("/api/import/**").authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())  // Pour H2 console
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:3002",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

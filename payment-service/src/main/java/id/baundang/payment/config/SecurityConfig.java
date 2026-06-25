package id.baundang.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderFilter gatewayHeaderFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        // Midtrans webhook is public — signature validates authenticity.
                        // Covers base, recurring and pay-account notification URLs.
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook/**").permitAll()
                        // Internal charge endpoint: called by consumer thread (no JWT), must be on internal net
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/charge").permitAll()
                        // Snap token is fetched by buyers (may be guests without JWT)
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/snap-token/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

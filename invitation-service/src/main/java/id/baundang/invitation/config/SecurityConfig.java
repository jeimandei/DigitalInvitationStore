package id.baundang.invitation.config;

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
                        // Public invitation page
                        .requestMatchers(HttpMethod.GET, "/i/**").permitAll()
                        // Public RSVP + guestbook reads/submissions
                        .requestMatchers(HttpMethod.GET,  "/api/v1/invitations/*/guestbook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/invitations/*/rsvp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/invitations/*/guestbook").permitAll()
                        // Public guest check-in (door staff scan QR, no login)
                        .requestMatchers(HttpMethod.POST, "/api/v1/invitations/*/checkin/*").permitAll()
                        // Expiring list consumed internally by notification-service scheduler
                        .requestMatchers(HttpMethod.GET, "/api/v1/invitations/expiring").permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

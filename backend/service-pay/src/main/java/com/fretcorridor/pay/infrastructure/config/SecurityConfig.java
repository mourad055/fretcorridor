package com.fretcorridor.pay.infrastructure.config;

import com.fretcorridor.pay.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Bloquant audit CDC §Transverse ("8 services sans authentification") :
 * PaiementController n'avait aucune protection propre — un POST direct sur
 * le port 8088 pouvait libérer un séquestre ou forger une écriture
 * comptable arbitraire. Même pattern que geo/mat/opt/trk/exe/cap (PR
 * #81/#95) — même secret partagé service-ida.
 *
 * /api/v1/pay/webhooks/** reste permitAll() : le prestataire de paiement
 * externe n'a et ne peut avoir aucun JWT FretCorridor — son authentification
 * propre est la signature HMAC vérifiée par HmacSha256SignatureVerifierAdapter,
 * pas Spring Security.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/pay/webhooks/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

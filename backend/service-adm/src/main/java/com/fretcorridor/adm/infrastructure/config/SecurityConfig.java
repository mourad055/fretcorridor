package com.fretcorridor.adm.infrastructure.config;

import com.fretcorridor.adm.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Bloquant audit CDC §Transverse ("8 services sans authentification") :
 * DossierController/ConfigurationController/JournalAuditController/
 * TenantController n'avaient aucune protection propre — repro audit :
 * "Création de tenant sans authentification" (TenantController.java:28).
 * Même pattern que geo/mat/opt/trk/exe/cap/pay — même secret partagé
 * service-ida. Aucun appelant HTTP hors gateway (les autres services
 * consomment service-adm uniquement via Kafka) : pas d'exception
 * permitAll nécessaire au-delà de /actuator/health.
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
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

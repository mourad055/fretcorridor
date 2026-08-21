package com.fretcorridor.opt.config;

import com.fretcorridor.opt.security.JwtAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Correctif audit CDC 2026-08-20 (constat #7, priorite 1) : PermitAll total
 * retire. AffectationL1Controller/FiltrageL0Controller sont des endpoints
 * de test manuel (Sprint 5) jamais appeles avec JWT en flux nominal - le
 * vrai declenchement passe par Kafka (EtapeExecuteeListener, etc.), donc
 * cette restriction ne casse aucun flux de production (ENF-SEC-01).
 *
 * CORRECTIF (regression evitee avant qu'elle ne soit deployee) :
 * GET /api/opt/affectations/{missionId} (AffectationController) EST appele
 * en synchrone interne par TRK (ServiceOptClient, meme principe que
 * ServiceMatClient cote MAT) - ce client ne transporte aucun Authorization
 * header aujourd'hui. Le proteger sans exemption aurait coupe
 * silencieusement tout calcul d'ETA (degrade gracieusement, cf ENF-DIS-04
 * dans ServiceOptClient - pas de crash, juste plus jamais de PositionETA
 * publie). Meme exemption ciblee que service-mat/calculer-lot : jamais un
 * retour a permitAll() global, un seul endpoint precis, avec justification.
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
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/opt/affectations/*").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

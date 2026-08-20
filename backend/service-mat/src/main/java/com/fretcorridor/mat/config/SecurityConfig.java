package com.fretcorridor.mat.config;

import com.fretcorridor.mat.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Correctif audit CDC 2026-08-20 (constat #7, priorite 1) : le PermitAll
 * total precedent laissait tout endpoint HTTP de service-mat accessible
 * sans JWT a quiconque atteint le reseau interne - une hypothese de flux
 * reseau n'est pas un controle technique (ENF-SEC-01, moindre privilege).
 *
 * Seul CoutController.calculerLot (POST /api/mat/couts/calculer-lot) reste
 * exempte : il est appele en synchrone interne par ServiceMatClient
 * (OPT -> MAT, meme porteur, budget L0/L1 ~50ms) et ce client ne transporte
 * aucun Authorization header aujourd'hui - le proteger casserait le cycle
 * L1 en production. C'est le seul endpoint HTTP reel expose par ce service
 * (confirme par grep sur les annotations @*Mapping, aucun autre).
 *
 * Tout le reste (endpoints futurs inclus) exige desormais un JWT valide.
 * Si un jour ServiceMatClient porte un secret/token service-a-service,
 * cette exemption pourra etre retiree.
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
                .requestMatchers(HttpMethod.POST, "/api/mat/couts/calculer-lot").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

package com.fretcorridor.mat.config;

import com.fretcorridor.mat.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Contrairement a service-geo (endpoints d'administration proteges par
 * ROLE_ADMINISTRATION), service-mat n'expose aucun endpoint destine a un
 * appel humain avec JWT : CoutController.calculerCoutsLot est appele en
 * synchrone interne par ServiceMatClient (OPT -> MAT, meme porteur, budget
 * L0/L1 ~50ms), sans jamais transporter de token (cf README moteur,
 * "MAT<->OPT<->GEO<->TRK = synchrone interne").
 *
 * PermitAll total ici : ajouter hasRole(...) sur /calculer-lot casserait le
 * cycle L1 en production, puisque ServiceMatClient n'envoie aucun
 * Authorization header aujourd'hui. Le JwtAuthenticationFilter reste en
 * place (dependances ajoutees par coherence entre les 4 modules du
 * perimetre Moteur) mais ne bloque jamais rien tant qu'aucun endpoint
 * n'est explicitement restreint ici.
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
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

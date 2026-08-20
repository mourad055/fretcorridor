package com.flysoft.fretcorridor.cap.config;

import com.flysoft.fretcorridor.cap.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Corrige le bloquant "0 authentification + IDOR" de l'audit CDC du
 * 19 aout (service-cap etait l'un des 8 services entierement ouverts,
 * CapaciteController.java:27,32,42).
 *
 * POST /api/cap/capacites (declaration) et POST /{id}/decrement exigent
 * desormais un JWT - IDOR corrige cote CapaciteController (verification
 * tenantId, meme principe que missionAppartenantA/notificationAppartenantA
 * ailleurs dans ce depot).
 *
 * GET /{id} reste permitAll au niveau Spring Security (consomme en
 * synchrone interne par service-not, PropositionRetourAVideListener,
 * declenche par Kafka -- AUCUN JWT utilisateur disponible dans ce flux,
 * contrairement a une requete HTTP entrante), MAIS n'est plus ouvert sans
 * controle depuis l'audit de suivi du 20 aout : CapaciteController.obtenir
 * verifie desormais une cle interne partagee (X-Internal-Service-Key,
 * ENF-SEC-05, meme mecanisme de rotation par variable d'environnement que
 * fretcorridor.jwt.secret) -- seul service-not (le seul appelant legitime,
 * Plan d'Execution §4.3 : appel synchrone autorise entre deux services du
 * meme porteur Mobile) connait la valeur configuree.
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
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cap/capacites/*").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

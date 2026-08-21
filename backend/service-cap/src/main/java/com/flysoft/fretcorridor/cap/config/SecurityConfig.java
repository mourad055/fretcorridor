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
 *
 * POST /{id}/decrement : meme principe applique au pont cross-tenant
 * EF-MKT-08 (audit de suivi Mobile, 21 aout) -- service-mkt appelle cet
 * endpoint avec la cle interne (le chargeur qui accepte une proposition
 * n'a jamais le JWT du transporteur proprietaire de la capacite, tenant
 * different). BUG CORRIGE : laisser ce chemin sous .anyRequest().authenticated()
 * le faisait rejeter en 403 par Spring Security AVANT meme d'atteindre
 * CapaciteController#decrementer, qui pourtant sait deja accepter soit un
 * JWT (chemin transporteur existant), soit cette cle interne -- double
 * controle, l'un des deux etait redondant et bloquant. permitAll ici au
 * niveau filtre, l'autorisation reelle restant entierement geree dans le
 * controller (meme design que le GET juste au-dessus).
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
                .requestMatchers(HttpMethod.POST, "/api/cap/capacites/*/decrement").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

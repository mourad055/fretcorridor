package com.fretcorridor.geo.config;

import com.fretcorridor.geo.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ENF-SEC-01 (CDC) : authentification forte sur les endpoints sensibles.
 * Corrige le trou releve par l'audit du 2026-08-19 (aucune securite sur
 * service-geo/mat/opt/trk - patch identique a repliquer sur les 3 autres
 * services une fois valide ici).
 *
 * Principe applique, coherent avec le README moteur ("MAT<->OPT<->GEO<->TRK
 * = synchrone interne, budget L0 ~50ms") :
 *  - GET /api/geo/**  : permitAll() - consomme en synchrone interne par
 *    OPT/MAT/TRK (meme porteur) ET en lecture par le Web (carte Bureau) ;
 *    jamais de JWT entre microservices du meme perimetre, ajouter une
 *    verification ici casserait le budget de latence L0 pour rien.
 *  - POST/PATCH sensibles (creation d'axe, bascule d'etat, risque
 *    securitaire, convention de repartition) : restreints a
 *    ROLE_ADMINISTRATION - ce sont des actions de configuration versionnee
 *    et auditee (EF-ADM-06, "console de configuration versionnee et
 *    auditee"), pas des consultations.
 *
 * IMPORTANT : HttpMethod.GET (enum), jamais la chaine "GET" - la surcharge
 * requestMatchers(String...) traiterait "GET" comme un CHEMIN litteral, pas
 * comme une restriction de methode HTTP (piege deja rencontre une fois,
 * cf commit precedent corrige).
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
                // Lecture toujours ouverte : synchrone interne (OPT/MAT/TRK)
                // + cartes Web, cf javadoc classe ci-dessus.
                .requestMatchers(HttpMethod.GET, "/api/geo/**").permitAll()
                // Actions de configuration versionnee/auditee : ADMINISTRATION uniquement.
                .requestMatchers(HttpMethod.POST, "/api/geo/axes").hasRole("ADMINISTRATION")
                // FIX audit 21/08 : la creation de hub est elle aussi une
                // action de configuration referentielle (meme nature que la
                // creation d'axe) - elle tombait jusque-la dans
                // anyRequest().authenticated(), accessible a tout role.
                .requestMatchers(HttpMethod.POST, "/api/geo/hubs").hasRole("ADMINISTRATION")
                .requestMatchers(HttpMethod.PATCH, "/api/geo/axes/*/etats/*").hasRole("ADMINISTRATION")
                .requestMatchers(HttpMethod.PATCH, "/api/geo/axes/*/risque-securitaire").hasRole("ADMINISTRATION")
                .requestMatchers(HttpMethod.PATCH, "/api/geo/axes/*/convention-repartition").hasRole("ADMINISTRATION")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

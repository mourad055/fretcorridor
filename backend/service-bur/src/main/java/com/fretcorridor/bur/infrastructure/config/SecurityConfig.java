package com.fretcorridor.bur.infrastructure.config;

import com.fretcorridor.bur.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Bloquant audit CDC §Transverse ("8 services sans authentification") :
 * dernier des 4 services Web (gateway/pay/adm/bur) a recevoir l'authentification
 * JWT. Meme pattern que geo/mat/opt/trk/exe/cap/pay/adm - meme secret partage
 * service-ida. Aucun appelant HTTP hors gateway (AlerteSeuilController,
 * BureauAgregatController, MissionAppparieeController, PositionController -
 * tous consommes exclusivement par la gateway), pas d'exception permitAll
 * necessaire au-dela de /actuator/health.
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

package com.flysoft.fretcorridor.flt.config;

import com.flysoft.fretcorridor.flt.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                // Sans cette regle, toute reponse d'erreur (404, 500...) sur
                // une route par ailleurs permitAll declenche un forward
                // interne Servlet vers /error, lui-meme re-filtre par la
                // securite et bloque (anyRequest().authenticated()) - le vrai
                // code d'erreur est alors masque par un 403 trompeur.
                .requestMatchers("/error").permitAll()
                // "/mes" doit rester authentifié malgré le pattern {id} plus
                // permissif ci-dessous — Spring Security prend la première
                // règle qui matche, pas la plus spécifique (contrairement au
                // routage MVC), d'où l'ordre explicite ici.
                .requestMatchers(HttpMethod.GET, "/api/flt/vehicules/mes").authenticated()
                // IDOR corrige (audit CDC du 19 aout, bloquant §3) : cette
                // route etait permitAll() pour l'appel interne service-cap
                // -> service-flt (ServiceFltClient) ; ce dernier transmet
                // desormais son propre JWT (cf VehiculeController.consulter),
                // donc plus besoin de derogation ici -- retombe sur
                // anyRequest().authenticated() ci-dessous, meme regle que le
                // reste du service.
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*", "http://127.0.0.1:*", "http://192.168.*.*:*", "http://10.*.*.*:*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

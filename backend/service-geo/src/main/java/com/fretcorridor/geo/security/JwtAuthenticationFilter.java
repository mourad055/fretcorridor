package com.fretcorridor.geo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Meme mecanisme que service-mkt (JwtAuthenticationFilter), avec deux
 * differences volontaires :
 *
 * 1) Logging SLF4J (log.warn) plutot que System.out.println - coherent avec
 *    le reste du module GEO (ENF-OBS-01/02, journalisation structuree), pas
 *    juste une sortie console perdue en production.
 *
 * 2) Rejet explicite (401) quand un token est present mais invalide/expire,
 *    plutot que de laisser passer la requete sans authentification et de
 *    compter uniquement sur SecurityConfig.anyRequest()/hasRole(...) en aval.
 *    Un GET public (permitAll()) sans aucun token continue normalement -
 *    seule l'ABSENCE de token est traitee en anonyme ; un token PRESENT mais
 *    invalide est toujours un rejet net, jamais une degradation silencieuse
 *    vers l'anonymat.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pas de token du tout : requete anonyme, laissee passer telle
            // quelle - SecurityConfig decide ensuite si la route exige une
            // authentification (permitAll() vs authenticated()/hasRole(...)).
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            var acteurId = jwtService.extraireActeurId(token);
            List<String> roles = jwtService.extraireRoles(token);

            var authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();

            var authentication = new UsernamePasswordAuthenticationToken(acteurId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception exception) {
            log.warn("JWT invalide ou expire sur {} {} - requete rejetee : {}",
                    request.getMethod(), request.getRequestURI(), exception.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }
}

package com.fretcorridor.gateway.infrastructure.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Valide le JWT extrait par {@link JwtServerAuthenticationConverter} et construit
 * l'Authentication réactive avec l'autorité ROLE_&lt;role&gt;, seule source d'habilitation
 * (RG-002) — jamais un rôle transmis par le client en clair.
 */
@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;

    public JwtReactiveAuthenticationManager(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        Optional<Claims> claims = jwtService.parse(token);
        if (claims.isEmpty()) {
            return Mono.error(new BadCredentialsException("Jeton invalide ou expiré"));
        }
        Claims c = claims.get();
        String role = c.get("role", String.class);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authenticated = new UsernamePasswordAuthenticationToken(c.getSubject(), token, authorities);
        authenticated.setDetails(c);
        return Mono.just(authenticated);
    }
}

package com.fretcorridor.gateway.infrastructure.security;

import com.fretcorridor.gateway.domain.Role;
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
        // FIX 21/08 : passe par JwtService.roleOf - accepte le claim singulier
        // "role" (tokens gateway) ET le tableau "roles" (tokens service-ida),
        // cf javadoc roleOf. Role.valueOf direct = NPE sur token ida.
        Role role = JwtService.roleOf(c);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var principal = new AuthenticatedActor(c.getSubject(), role, c.get("tenantId", String.class),
                JwtService.delegationTokenOf(c), c.get("phone", String.class));
        var authenticated = new UsernamePasswordAuthenticationToken(principal, token, authorities);
        return Mono.just(authenticated);
    }
}

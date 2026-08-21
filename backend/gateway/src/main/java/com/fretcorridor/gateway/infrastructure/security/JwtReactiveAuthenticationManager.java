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
        Role role = Role.valueOf(extraireRole(c));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var principal = new AuthenticatedActor(c.getSubject(), role, c.get("tenantId", String.class), JwtService.delegationTokenOf(c));
        var authenticated = new UsernamePasswordAuthenticationToken(principal, token, authorities);
        return Mono.just(authenticated);
    }

    // Les jetons emis par la gateway elle-meme portent une claim "role" au
    // singulier (String) ; ceux emis directement par service-ida portent
    // "roles" au pluriel (List<String>, cf JwtService.java:37 cote ida) --
    // jusqu'ici jamais interoperables (audit CDC, la gateway plantait en
    // NullPointerException sur un jeton ida brut). Tolerance des deux
    // formats : "role" prioritaire si present, sinon premier element de
    // "roles".
    @SuppressWarnings("unchecked")
    private String extraireRole(Claims c) {
        String roleSingulier = c.get("role", String.class);
        if (roleSingulier != null) {
            return roleSingulier;
        }
        List<String> roles = c.get("roles", List.class);
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }
        throw new BadCredentialsException("Jeton sans claim role/roles exploitable");
    }
}

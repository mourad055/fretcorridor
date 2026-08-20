package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.AuthDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Bug corrigé (audit de suivi du 20 août, périmètre Mobile) : rafraichir()
 * ne vérifiait jamais acteur.getActif(), contrairement à login() -- un
 * compte verrouillé après 3 échecs de PIN pouvait continuer à rafraîchir
 * son token indéfiniment tant qu'il détenait un refresh token émis avant
 * le blocage.
 */
class AuthServiceTest {

    @Mock private ActeurRepository acteurRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService service;
    private UUID acteurId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AuthService(acteurRepository, passwordEncoder, jwtService);
        acteurId = UUID.randomUUID();
    }

    private Acteur acteur(boolean actif) {
        return Acteur.builder()
                .id(acteurId)
                .telephone("+237600000001")
                .codePin("hash")
                .actif(actif)
                .build();
    }

    @Test
    void refreshing_a_locked_account_is_refused() {
        when(jwtService.extraireActeurId("un-refresh-token")).thenReturn(acteurId);
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur(false)));

        assertThatThrownBy(() -> service.rafraichir("un-refresh-token"))
                .hasMessage("COMPTE_BLOQUE");
    }

    @Test
    void refreshing_an_active_account_succeeds() {
        when(jwtService.extraireActeurId("un-refresh-token")).thenReturn(acteurId);
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur(true)));
        when(jwtService.genererAccessToken(any())).thenReturn("nouveau-access");
        when(jwtService.genererRefreshToken(any())).thenReturn("nouveau-refresh");

        AuthDto.AuthResponse reponse = service.rafraichir("un-refresh-token");

        assertThat(reponse.getAccessToken()).isEqualTo("nouveau-access");
        assertThat(reponse.getRefreshToken()).isEqualTo("nouveau-refresh");
    }
}

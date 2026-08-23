package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.AuthDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.AffiliationTenant;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.repository.AffiliationTenantRepository;
import com.flysoft.fretcorridor.ida.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * S18 (Sprint 18, "Second tenant institutionnel", audit de suivi 23 aout) :
 * règle produit choisie explicitement par l'utilisatrice - c'est le second
 * bureau (jamais le transporteur) qui invite/valide, l'invitation EST la
 * validation, aucun flux d'acceptation côté transporteur.
 */
class AffiliationServiceTest {

    @Mock private ActeurRepository acteurRepository;
    @Mock private AffiliationTenantRepository affiliationTenantRepository;
    @Mock private JwtService jwtService;

    private AffiliationService service;
    private UUID acteurId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AffiliationService(acteurRepository, affiliationTenantRepository, jwtService);
        acteurId = UUID.randomUUID();
    }

    private Acteur transporteur() {
        return Acteur.builder()
                .id(acteurId)
                .telephone("+237690000001")
                .tenantId("tenant-bgft-douala")
                .roles(Set.of(RoleActeur.TRANSPORTEUR))
                .build();
    }

    @Test
    void inviter_un_transporteur_cree_une_affiliation() {
        when(acteurRepository.findByTelephone("+237690000001")).thenReturn(Optional.of(transporteur()));
        when(affiliationTenantRepository.existsByActeurIdAndTenantId(acteurId, "tenant-bureau-2")).thenReturn(false);

        service.inviter("tenant-bureau-2", "+237690000001");

        ArgumentCaptor<AffiliationTenant> captor = ArgumentCaptor.forClass(AffiliationTenant.class);
        verify(affiliationTenantRepository).save(captor.capture());
        assertThat(captor.getValue().getActeurId()).isEqualTo(acteurId);
        assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-bureau-2");
    }

    @Test
    void inviter_vers_le_tenant_d_origine_ne_cree_rien() {
        when(acteurRepository.findByTelephone("+237690000001")).thenReturn(Optional.of(transporteur()));

        service.inviter("tenant-bgft-douala", "+237690000001");

        verify(affiliationTenantRepository, never()).save(any());
    }

    @Test
    void inviter_deux_fois_le_meme_bureau_est_idempotent() {
        when(acteurRepository.findByTelephone("+237690000001")).thenReturn(Optional.of(transporteur()));
        when(affiliationTenantRepository.existsByActeurIdAndTenantId(acteurId, "tenant-bureau-2")).thenReturn(true);

        service.inviter("tenant-bureau-2", "+237690000001");

        verify(affiliationTenantRepository, never()).save(any());
    }

    @Test
    void inviter_un_chargeur_est_refuse() {
        Acteur chargeur = Acteur.builder().id(acteurId).telephone("+237600000002")
                .tenantId("MARKETPLACE_CM").roles(Set.of(RoleActeur.CHARGEUR)).build();
        when(acteurRepository.findByTelephone("+237600000002")).thenReturn(Optional.of(chargeur));

        assertThatThrownBy(() -> service.inviter("tenant-bureau-2", "+237600000002"))
                .hasMessage("ROLE_NON_AFFILIABLE");
    }

    @Test
    void mes_tenants_inclut_toujours_le_tenant_d_origine_en_premier() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(transporteur()));
        when(affiliationTenantRepository.findByActeurId(acteurId)).thenReturn(List.of(
                AffiliationTenant.builder().acteurId(acteurId).tenantId("tenant-bureau-2").build()));

        List<AuthDto.TenantDisponible> tenants = service.mesTenants(acteurId);

        assertThat(tenants).hasSize(2);
        assertThat(tenants.get(0).tenantId()).isEqualTo("tenant-bgft-douala");
        assertThat(tenants.get(0).origine()).isTrue();
        assertThat(tenants.get(1).tenantId()).isEqualTo("tenant-bureau-2");
        assertThat(tenants.get(1).origine()).isFalse();
    }

    @Test
    void selectionner_un_tenant_non_affilie_est_refuse() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(transporteur()));
        when(affiliationTenantRepository.existsByActeurIdAndTenantId(acteurId, "tenant-inconnu")).thenReturn(false);

        assertThatThrownBy(() -> service.selectionner(acteurId, "tenant-inconnu"))
                .hasMessage("TENANT_NON_AFFILIE");
    }

    @Test
    void selectionner_un_tenant_affilie_emet_un_token_scope() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(transporteur()));
        when(affiliationTenantRepository.existsByActeurIdAndTenantId(acteurId, "tenant-bureau-2")).thenReturn(true);
        when(jwtService.genererAccessToken(any(), eq("tenant-bureau-2"))).thenReturn("access-scope-bureau-2");
        when(jwtService.genererRefreshToken(any())).thenReturn("refresh-token");

        AuthDto.AuthResponse reponse = service.selectionner(acteurId, "tenant-bureau-2");

        assertThat(reponse.getAccessToken()).isEqualTo("access-scope-bureau-2");
        assertThat(reponse.getTenantId()).isEqualTo("tenant-bureau-2");
    }

    @Test
    void selectionner_le_tenant_d_origine_est_toujours_autorise() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(transporteur()));
        when(jwtService.genererAccessToken(any(), eq("tenant-bgft-douala"))).thenReturn("access-origine");
        when(jwtService.genererRefreshToken(any())).thenReturn("refresh-token");

        AuthDto.AuthResponse reponse = service.selectionner(acteurId, "tenant-bgft-douala");

        assertThat(reponse.getTenantId()).isEqualTo("tenant-bgft-douala");
        verify(affiliationTenantRepository, never()).existsByActeurIdAndTenantId(any(), any());
    }
}

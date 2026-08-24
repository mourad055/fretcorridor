package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Gestion des comptes par un Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.1).
 */
class GestionCompteServiceTest {

    @Mock private ActeurRepository acteurRepository;

    private GestionCompteService service;
    private UUID acteurId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GestionCompteService(acteurRepository);
        acteurId = UUID.randomUUID();
    }

    private Acteur acteur() {
        return Acteur.builder()
                .id(acteurId)
                .telephone("+237690000001")
                .tenantId("tenant-bgft-douala")
                .roles(Set.of(RoleActeur.BUREAU))
                .actif(true)
                .build();
    }

    @Test
    void liste_les_comptes_du_tenant_demande() {
        when(acteurRepository.findByTenantId("tenant-bgft-douala")).thenReturn(List.of(acteur()));

        List<Acteur> comptes = service.listerParTenant("tenant-bgft-douala");

        assertThat(comptes).extracting(Acteur::getId).containsExactly(acteurId);
    }

    @Test
    void desactive_un_compte_de_son_propre_tenant() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur()));
        when(acteurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Acteur mis_a_jour = service.changerStatut(acteurId, "tenant-bgft-douala", false);

        assertThat(mis_a_jour.getActif()).isFalse();
        ArgumentCaptor<Acteur> captor = ArgumentCaptor.forClass(Acteur.class);
        verify(acteurRepository).save(captor.capture());
        assertThat(captor.getValue().getActif()).isFalse();
    }

    @Test
    void changer_le_statut_d_un_compte_d_un_autre_tenant_est_refuse_comme_introuvable() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur()));

        assertThatThrownBy(() -> service.changerStatut(acteurId, "tenant-bnft-ndjamena", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("COMPTE_INTROUVABLE");
    }

    @Test
    void changer_le_statut_d_un_compte_inexistant_est_refuse() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changerStatut(acteurId, "tenant-bgft-douala", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("COMPTE_INTROUVABLE");
    }

    @Test
    void change_les_roles_d_un_compte_de_son_propre_tenant() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur()));
        when(acteurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Acteur mis_a_jour = service.changerRoles(acteurId, "tenant-bgft-douala", Set.of(RoleActeur.ADMINISTRATION));

        assertThat(mis_a_jour.getRoles()).containsExactly(RoleActeur.ADMINISTRATION);
    }
}

package com.fretcorridor.opt.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Diffusion-course (timeout) : verifie la tache qui passe EXPIREE les
 * propositions depassees puis remet la demande en file quand personne n'a
 * accepte a temps. Couvre la regression "demande confirmee remise en file".
 */
class ExpirationPropositionServiceTest {

    @Mock
    private AffectationRepository affectationRepository;
    @Mock
    private DemandeEnAttenteRepository demandeEnAttenteRepository;

    private ExpirationPropositionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ExpirationPropositionService(affectationRepository, demandeEnAttenteRepository);
    }

    @Test
    void aucune_proposition_a_expirer_retourne_sans_rien_faire() {
        when(affectationRepository.findByStatutAndExpireALessThan(any(), any()))
                .thenReturn(List.of());

        service.expirerPropositionsDepassees();

        verify(affectationRepository, never()).saveAll(any());
        verify(demandeEnAttenteRepository, never()).findByDemandeId(any());
    }

    @Test
    void proposition_depassee_passe_expiree_et_demande_remise_en_file() {
        UUID demandeId = UUID.randomUUID();
        Affectation a = affectationProposee(demandeId);
        when(affectationRepository.findByStatutAndExpireALessThan(eq(StatutAffectation.PROPOSEE), any()))
                .thenReturn(List.of(a));
        // Plus aucune PROPOSEE ni CONFIRMEE sur cette demande.
        when(affectationRepository.findByDemandeIdAndStatut(eq(demandeId), eq(StatutAffectation.PROPOSEE)))
                .thenReturn(List.of());
        when(affectationRepository.findByDemandeIdAndStatut(eq(demandeId), eq(StatutAffectation.CONFIRMEE)))
                .thenReturn(List.of());
        DemandeEnAttente demande = new DemandeEnAttente(
                demandeId, UUID.randomUUID(), UUID.randomUUID(), java.util.Map.of(),
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null);
        demande.marquerTraitee();
        when(demandeEnAttenteRepository.findByDemandeId(demandeId)).thenReturn(Optional.of(demande));

        service.expirerPropositionsDepassees();

        assertEquals(StatutAffectation.EXPIREE, a.getStatut());
        verify(affectationRepository).saveAll(List.of(a));
        // La demande n'a plus de proposition active ni confirmee : remise en file.
        assertFalse(demande.isTraitee());
        verify(demandeEnAttenteRepository).save(demande);
    }

    @Test
    void demande_avec_proposition_active_pas_remise_en_file() {
        UUID demandeId = UUID.randomUUID();
        Affectation a = affectationProposee(demandeId);
        when(affectationRepository.findByStatutAndExpireALessThan(eq(StatutAffectation.PROPOSEE), any()))
                .thenReturn(List.of(a));
        // Il reste une autre PROPOSEE active sur cette demande : pas de remise en file.
        when(affectationRepository.findByDemandeIdAndStatut(eq(demandeId), eq(StatutAffectation.PROPOSEE)))
                .thenReturn(List.of(affectationProposee(demandeId)));

        service.expirerPropositionsDepassees();

        verify(demandeEnAttenteRepository, never()).findByDemandeId(any());
    }

    @Test
    void demande_confirmee_pas_remise_en_file() {
        UUID demandeId = UUID.randomUUID();
        Affectation a = affectationProposee(demandeId);
        when(affectationRepository.findByStatutAndExpireALessThan(eq(StatutAffectation.PROPOSEE), any()))
                .thenReturn(List.of(a));
        // Plus de PROPOSEE active mais la demande a deja ete confirmee.
        when(affectationRepository.findByDemandeIdAndStatut(eq(demandeId), eq(StatutAffectation.PROPOSEE)))
                .thenReturn(List.of());
        when(affectationRepository.findByDemandeIdAndStatut(eq(demandeId), eq(StatutAffectation.CONFIRMEE)))
                .thenReturn(List.of(affectationProposee(demandeId)));

        service.expirerPropositionsDepassees();

        verify(demandeEnAttenteRepository, never()).findByDemandeId(any());
    }

    private Affectation affectationProposee(UUID demandeId) {
        Affectation affectation = new Affectation(
                demandeId, UUID.randomUUID(), null, null, UUID.randomUUID(),
                java.math.BigDecimal.valueOf(200),
                0.0, 0.0, 100.0, 200.0,
                null, null, null, null,
                java.math.BigDecimal.TEN,
                null, null, null,
                null, null, null, null,
                null, null,
                null, null, null,
                false,
                null, null, null, null, null, null, null, null, null,
                "Origine", "Destination", Instant.now().plusSeconds(900)
        );
        reflectionSet(affectation, "id", UUID.randomUUID());
        return affectation;
    }

    private void reflectionSet(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de fixer le champ de test " + fieldName, e);
        }
    }
}

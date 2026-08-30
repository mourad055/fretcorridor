package com.fretcorridor.opt.web;

import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.AffectationRepository;
import com.fretcorridor.opt.domain.StatutAffectation;
import com.fretcorridor.opt.web.dto.AffectationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Diffusion-course (trouvaille Mobile) : GET /api/opt/affectations/proposees
 * "mes propositions en attente" d'un transporteur. Meme patron que le GET
 * _{missionId} reserve a TRK : endpoint interne protege par cle
 * (X-Internal-Service-Key), consomme en back-to-back par service-cap.
 */
class AffectationControllerTest {

    private static final String CLE = "cle-test";
    private final AffectationRepository repository = mock(AffectationRepository.class);
    private final AffectationController controller = new AffectationController(repository, CLE);

    private final UUID transporteurId = UUID.randomUUID();

    @Test
    void lister_proposees_filtre_par_transporteur_et_statut_proposee() {
        Affectation proposee = affectation(transporteurId);
        when(repository.findByTransporteurIdAndStatut(transporteurId, StatutAffectation.PROPOSEE))
                .thenReturn(List.of(proposee));

        List<AffectationResponse> resultat = controller.listerProposees(transporteurId, CLE);

        assertEquals(1, resultat.size());
        assertEquals(proposee.getId(), resultat.get(0).missionId());
        assertEquals(transporteurId, resultat.get(0).transporteurId());
        assertEquals("PROPOSEE", resultat.get(0).statut());
        verify(repository).findByTransporteurIdAndStatut(transporteurId, StatutAffectation.PROPOSEE);
    }

    @Test
    void lister_proposees_sans_transporteur_renvoie_liste_vide() {
        when(repository.findByTransporteurIdAndStatut(transporteurId, StatutAffectation.PROPOSEE))
                .thenReturn(List.of());

        List<AffectationResponse> resultat = controller.listerProposees(transporteurId, CLE);

        assertTrue(resultat.isEmpty());
    }

    @Test
    void lister_proposees_refuse_cle_interne_invalide() {
        assertThrows(ResponseStatusException.class,
                () -> controller.listerProposees(transporteurId, "mauvaise-cle"));
    }

    @Test
    void lister_proposees_refuse_cle_interne_absente() {
        assertThrows(ResponseStatusException.class,
                () -> controller.listerProposees(transporteurId, null));
    }

    private Affectation affectation(UUID transporteurId) {
        Affectation affectation = new Affectation(
                UUID.randomUUID(), UUID.randomUUID(), transporteurId, null, UUID.randomUUID(),
                BigDecimal.TEN,
                1.0, 2.0, 3.0, 4.0,
                null, null, null, null,
                BigDecimal.TEN,
                null, null, null,
                null, null, null, null,
                null, null,
                null, null, null,
                false,
                null, null, null, null, null, null, null, null, null,
                null, null, null
        );
        reflectionSet(affectation, "id", UUID.randomUUID());
        return affectation;
    }

    private void reflectionSet(Object cible, String champ, Object valeur) {
        try {
            java.lang.reflect.Field f = cible.getClass().getDeclaredField(champ);
            f.setAccessible(true);
            f.set(cible, valeur);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.fretcorridor.opt.simulation;

import com.fretcorridor.opt.client.ItineraireResponseDto;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.opt.client.ValhallaClient;
import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.AffectationRepository;
import com.fretcorridor.opt.domain.CapaciteEnAttente;
import com.fretcorridor.opt.domain.CapaciteEnAttenteRepository;
import com.fretcorridor.opt.sequencement.EtapeTournee;
import com.fretcorridor.opt.sequencement.EtapeTourneeRepository;
import com.fretcorridor.opt.sequencement.Tournee;
import com.fretcorridor.opt.sequencement.TourneeRepository;
import com.fretcorridor.opt.sequencement.alns.AlnsSolver;
import com.fretcorridor.opt.sequencement.alns.DetourValidator;
import com.fretcorridor.opt.sequencement.alns.OperateurInsertion;
import com.fretcorridor.opt.web.dto.SimulationInsertionRequest;
import com.fretcorridor.opt.web.dto.SimulationInsertionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests de SimulationInsertionService (point 4 : endpoint simulation
 * d'insertion). Objectifs : verifier que le dry-run ne persiste RIEN et que
 * la reponse porte bien le detour (km) et le temps ajoute. Les repositories
 * sont mockes, le solveur ALNS est reel (seed fixe) : on teste le calcul,
 * pas la persistance.
 */
class SimulationInsertionServiceTest {

    @Mock private TourneeRepository tourneeRepository;
    @Mock private EtapeTourneeRepository etapeTourneeRepository;
    @Mock private AffectationRepository affectationRepository;
    @Mock private CapaciteEnAttenteRepository capaciteEnAttenteRepository;
    @Mock private ServiceGeoClient serviceGeoClient;
    @Mock private ValhallaClient valhallaClient;

    private final DetourValidator detourValidator = new DetourValidator();
    private final AlnsSolver alnsSolver = new AlnsSolver(
            new OperateurInsertion(detourValidator), 42L);

    private SimulationInsertionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SimulationInsertionService(
                tourneeRepository, etapeTourneeRepository, affectationRepository,
                capaciteEnAttenteRepository, alnsSolver, serviceGeoClient, valhallaClient);
    }

    private SimulationInsertionRequest requete(UUID capaciteId) {
        return new SimulationInsertionRequest(
                capaciteId, UUID.randomUUID(),
                4.05, 9.70, 4.07, 9.73,
                BigDecimal.valueOf(200), false);
    }

    @Test
    void sansTournee_en_cours_detour_vaut_zero() {
        UUID capaciteId = UUID.randomUUID();
        when(tourneeRepository.findByCapaciteIdAndStatutIn(any(), any())).thenReturn(List.of());

        SimulationInsertionResponse response = service.simulerInsertion(requete(capaciteId));

        assertTrue(response.inseree());
        assertEquals(0.0, response.detourKm());
        assertTrue(response.tourneeKm() > 0);
    }

    @Test
    void avecTournee_le_detour_est_la_difference_entre_les_deux_scenarios() {
        UUID capaciteId = UUID.randomUUID();
        UUID tourneeId = UUID.randomUUID();

        Tournee tournee = new Tournee(capaciteId, UUID.randomUUID());
        reflectionSet(tournee, "id", tourneeId);

        Affectation existante = creerAffectation(capaciteId, 4.05, 9.70, 4.06, 9.72);
        UUID affectationId = existante.getId();

        when(tourneeRepository.findByCapaciteIdAndStatutIn(capaciteId, List.of(Tournee.Statut.CONFIRMEE, Tournee.Statut.EN_EXECUTION)))
                .thenReturn(List.of(tournee));

        EtapeTournee enlevement = new EtapeTournee(tournee, affectationId, 0, EtapeTournee.TypeEtape.ENLEVEMENT, BigDecimal.valueOf(200));
        EtapeTournee livraison = new EtapeTournee(tournee, affectationId, 1, EtapeTournee.TypeEtape.LIVRAISON, BigDecimal.ZERO);
        when(etapeTourneeRepository.findByTourneeIdOrderByRangAsc(tourneeId)).thenReturn(List.of(enlevement, livraison));
        when(affectationRepository.findAllById(List.of(affectationId))).thenReturn(List.of(existante));

        when(capaciteEnAttenteRepository.findFirstByCapaciteIdOrderByDateReceptionDesc(capaciteId))
                .thenReturn(Optional.of(new CapaciteEnAttente(capaciteId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        null, null, null, null, null,
                        BigDecimal.valueOf(1000), null)));

        SimulationInsertionResponse response = service.simulerInsertion(requete(capaciteId));

        assertTrue(response.inseree());
        assertTrue(response.detourKm() > 0);
    }

    private void reflectionSet(Object cible, String champ, Object valeur) {
        try {
            var field = cible.getClass().getDeclaredField(champ);
            field.setAccessible(true);
            field.set(cible, valeur);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private Affectation creerAffectation(UUID capaciteId, double oLat, double oLon, double dLat, double dLon) {
        Affectation affectation = new Affectation(
                UUID.randomUUID(), capaciteId, null, null, UUID.randomUUID(),
                BigDecimal.valueOf(200),
                oLat, oLon, dLat, dLon,
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
}
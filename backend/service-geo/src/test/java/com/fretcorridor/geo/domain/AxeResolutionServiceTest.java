package com.fretcorridor.geo.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AxeResolutionServiceTest {

    private static final double DOUALA_LAT = 4.0511;
    private static final double DOUALA_LON = 9.7679;
    private static final double YAOUNDE_LAT = 3.8480;
    private static final double YAOUNDE_LON = 11.5021;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private HubRepository hubRepository;

    @Mock
    private AxeRepository axeRepository;

    @Mock
    private ZonageH3Service zonageH3Service;

    @InjectMocks
    private AxeResolutionService axeResolutionService;

    @Test
    void resoutAxeEntreLesHubsLesPlusProches() {
        Hub hubDouala = hub(UUID.randomUUID(), DOUALA_LAT, DOUALA_LON);
        Hub hubYaounde = hub(UUID.randomUUID(), YAOUNDE_LAT, YAOUNDE_LON);
        Axe axe = mock(Axe.class);

        stubHubProche(DOUALA_LAT, DOUALA_LON, "cellDouala", hubDouala);
        stubHubProche(YAOUNDE_LAT, YAOUNDE_LON, "cellYaounde", hubYaounde);
        when(axeRepository.findFirstByHubOrigineIdAndHubDestinationIdAndMatchingActifTrue(
                hubDouala.getId(), hubYaounde.getId())).thenReturn(Optional.of(axe));

        Optional<Axe> resultat = axeResolutionService.resoudre(
                DOUALA_LAT, DOUALA_LON, YAOUNDE_LAT, YAOUNDE_LON, null);

        assertThat(resultat).containsSame(axe);
    }

    @Test
    void choisitLeHubLePlusProcheParmiLesVoisins() {
        Hub hubLoin = hub(UUID.randomUUID(), YAOUNDE_LAT, YAOUNDE_LON);
        Hub hubProche = hub(UUID.randomUUID(), DOUALA_LAT + 0.001, DOUALA_LON + 0.001);
        Axe axe = mock(Axe.class);

        when(zonageH3Service.indexPourPoint(DOUALA_LAT, DOUALA_LON)).thenReturn("cellDouala");
        when(zonageH3Service.indexPourPoint(YAOUNDE_LAT, YAOUNDE_LON)).thenReturn("cellYaounde");
        when(zonageH3Service.kRing("cellDouala", 3)).thenReturn(List.of("cellDouala"));
        when(zonageH3Service.kRing("cellYaounde", 3)).thenReturn(List.of("cellYaounde"));
        when(hubRepository.findByH3IndexIn(List.of("cellDouala")))
                .thenReturn(List.of(hubLoin, hubProche));
        when(hubRepository.findByH3IndexIn(List.of("cellYaounde")))
                .thenReturn(List.of(hubLoin));
        when(axeRepository.findFirstByHubOrigineIdAndHubDestinationIdAndMatchingActifTrue(
                hubProche.getId(), hubLoin.getId())).thenReturn(Optional.of(axe));

        Optional<Axe> resultat = axeResolutionService.resoudre(
                DOUALA_LAT, DOUALA_LON, YAOUNDE_LAT, YAOUNDE_LON, null);

        assertThat(resultat).containsSame(axe);
    }

    @Test
    void retourneVideQuandAucunHubDansLeRayonH3() {
        when(zonageH3Service.indexPourPoint(anyDouble(), anyDouble())).thenReturn("celluleVide");
        when(zonageH3Service.kRing(anyString(), anyInt())).thenReturn(List.of("celluleVide"));
        when(hubRepository.findByH3IndexIn(List.of("celluleVide"))).thenReturn(List.of());

        Optional<Axe> resultat = axeResolutionService.resoudre(
                DOUALA_LAT, DOUALA_LON, YAOUNDE_LAT, YAOUNDE_LON, null);

        assertThat(resultat).isEmpty();
        verify(axeRepository, never())
                .findFirstByHubOrigineIdAndHubDestinationIdAndMatchingActifTrue(any(), any());
    }

    @Test
    void retourneVideQuandOrigineEtDestinationResolventSurLeMemeHub() {
        Hub hubDouala = hub(UUID.randomUUID(), DOUALA_LAT, DOUALA_LON);

        stubHubProche(DOUALA_LAT, DOUALA_LON, "cellDouala", hubDouala);
        stubHubProche(YAOUNDE_LAT, YAOUNDE_LON, "cellYaounde", hubDouala);

        Optional<Axe> resultat = axeResolutionService.resoudre(
                DOUALA_LAT, DOUALA_LON, YAOUNDE_LAT, YAOUNDE_LON, null);

        assertThat(resultat).isEmpty();
        verify(axeRepository, never())
                .findFirstByHubOrigineIdAndHubDestinationIdAndMatchingActifTrue(any(), any());
    }

    @Test
    void retourneVideQuandAucunAxeActifNeRelieLesHubs() {
        Hub hubDouala = hub(UUID.randomUUID(), DOUALA_LAT, DOUALA_LON);
        Hub hubYaounde = hub(UUID.randomUUID(), YAOUNDE_LAT, YAOUNDE_LON);

        stubHubProche(DOUALA_LAT, DOUALA_LON, "cellDouala", hubDouala);
        stubHubProche(YAOUNDE_LAT, YAOUNDE_LON, "cellYaounde", hubYaounde);
        when(axeRepository.findFirstByHubOrigineIdAndHubDestinationIdAndMatchingActifTrue(
                hubDouala.getId(), hubYaounde.getId())).thenReturn(Optional.empty());

        Optional<Axe> resultat = axeResolutionService.resoudre(
                DOUALA_LAT, DOUALA_LON, YAOUNDE_LAT, YAOUNDE_LON, null);

        assertThat(resultat).isEmpty();
    }

    @Test
    void retourneVideQuandLAxeAppartientAUnAutreTenant() {
        Hub hubDouala = hub(UUID.randomUUID(), DOUALA_LAT, DOUALA_LON);
        Hub hubYaounde = hub(UUID.randomUUID(), YAOUNDE_LAT, YAOUNDE_LON);
        Axe axe = mock(Axe.class);
        when(axe.getTenantId()).thenReturn("tenant-tchad");

        stubHubProche(DOUALA_LAT, DOUALA_LON, "cellDouala", hubDouala);
        stubHubProche(YAOUNDE_LAT, YAOUNDE_LON, "cellYaounde", hubYaounde);
        when(axeRepository.findFirstByHubOrigineIdAndHubDestinationIdAndMatchingActifTrue(
                hubDouala.getId(), hubYaounde.getId())).thenReturn(Optional.of(axe));

        Optional<Axe> resultat = axeResolutionService.resoudre(
                DOUALA_LAT, DOUALA_LON, YAOUNDE_LAT, YAOUNDE_LON, "tenant-douala");

        assertThat(resultat).isEmpty();
    }

    private void stubHubProche(double latitude, double longitude, String cellule, Hub hub) {
        when(zonageH3Service.indexPourPoint(latitude, longitude)).thenReturn(cellule);
        when(zonageH3Service.kRing(cellule, 3)).thenReturn(List.of(cellule));
        when(hubRepository.findByH3IndexIn(List.of(cellule))).thenReturn(List.of(hub));
    }

    private Hub hub(UUID id, double latitude, double longitude) {
        Hub hub = mock(Hub.class);
        when(hub.getId()).thenReturn(id);
        // Lenient : getPosition() n'est appele par le comparateur que si la
        // cellule contient plus d'un hub (Stream.min ne compare pas un
        // singleton) - le stub reste correct pour les deux cas.
        lenient().when(hub.getPosition()).thenReturn(
                GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude)));
        return hub;
    }
}

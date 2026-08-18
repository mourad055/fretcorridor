package com.flysoft.fretcorridor.flt.service;

import com.flysoft.fretcorridor.flt.dto.VehiculeDto;
import com.flysoft.fretcorridor.flt.entity.Vehicule;
import com.flysoft.fretcorridor.flt.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehiculeServiceTest {

    @Mock private VehiculeRepository vehiculeRepository;
    private VehiculeService service;
    private UUID proprietaireId;
    private static final String TENANT = "tenant-bgft-douala";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new VehiculeService(vehiculeRepository);
        proprietaireId = UUID.randomUUID();
        when(vehiculeRepository.save(any(Vehicule.class))).thenAnswer(inv -> {
            Vehicule v = inv.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });
    }

    @Test
    void declaring_a_vehicle_stores_the_owner_and_tenant() {
        var requete = new VehiculeDto.DeclarerRequest();
        requete.setTypeVehicule("Camion 10T");
        requete.setImmatriculation("LT 1234 AB");

        var reponse = service.declarer(proprietaireId, TENANT, requete);

        assertThat(reponse.getTypeVehicule()).isEqualTo("Camion 10T");

        ArgumentCaptor<Vehicule> captor = ArgumentCaptor.forClass(Vehicule.class);
        verify(vehiculeRepository).save(captor.capture());
        assertThat(captor.getValue().getProprietaireActeurId()).isEqualTo(proprietaireId);
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT);
    }

    @Test
    void a_driver_only_sees_their_own_vehicles() {
        when(vehiculeRepository.findByProprietaireActeurIdAndTenantIdOrderByDateCreationDesc(proprietaireId, TENANT))
                .thenReturn(List.of(Vehicule.builder().id(UUID.randomUUID()).proprietaireActeurId(proprietaireId)
                        .typeVehicule("Fourgon 3T").tenantId(TENANT).build()));

        var vehicules = service.listerMesVehicules(proprietaireId, TENANT);

        assertThat(vehicules).hasSize(1);
        assertThat(vehicules.get(0).getTypeVehicule()).isEqualTo("Fourgon 3T");
    }
}

package com.fretcorridor.bur.infrastructure.config;

import com.fretcorridor.bur.domain.AgregationMissionsService;
import com.fretcorridor.bur.domain.MissionAppparieeRepositoryPort;
import com.fretcorridor.bur.domain.MissionAppparieeService;
import com.fretcorridor.bur.domain.MissionRepositoryPort;
import com.fretcorridor.bur.domain.ObservatoireService;
import com.fretcorridor.bur.domain.PositionRepositoryPort;
import com.fretcorridor.bur.domain.PositionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Câblage du domaine, seul point du code à connaître à la fois le port et la config Spring. */
@Configuration
public class DomainConfig {

    @Bean
    public AgregationMissionsService agregationMissionsService(
            MissionRepositoryPort repository,
            @Value("${fretcorridor.bur.seuil-agregation:3}") long seuilAgregation
    ) {
        return new AgregationMissionsService(repository, seuilAgregation);
    }

    @Bean
    public MissionAppparieeService missionAppparieeService(MissionAppparieeRepositoryPort repository) {
        return new MissionAppparieeService(repository);
    }

    @Bean
    public ObservatoireService observatoireService(
            MissionAppparieeRepositoryPort repository,
            @Value("${fretcorridor.bur.seuil-agregation:3}") long seuilAgregation
    ) {
        return new ObservatoireService(repository, seuilAgregation);
    }

    @Bean
    public PositionService positionService(PositionRepositoryPort repository) {
        return new PositionService(repository);
    }
}

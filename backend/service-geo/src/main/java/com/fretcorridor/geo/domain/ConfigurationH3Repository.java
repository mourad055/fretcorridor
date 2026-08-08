package com.fretcorridor.geo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigurationH3Repository extends JpaRepository<ConfigurationH3, String> {

    // findById(String cle) est deja fourni par JpaRepository (cle = @Id) -
    // pas besoin de le redeclarer, ce commentaire documente juste l'usage attendu :
    // ZonageH3Service.resolutionActuelle() l'appelle avec la cle "resolution_defaut".
    Optional<ConfigurationH3> findByCle(String cle);
}

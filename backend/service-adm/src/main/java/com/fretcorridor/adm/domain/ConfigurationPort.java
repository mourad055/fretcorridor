package com.fretcorridor.adm.domain;

import java.util.List;
import java.util.Optional;

public interface ConfigurationPort {
    void sauvegarder(ConfigurationVersionnee configuration);

    List<ConfigurationVersionnee> historique(String cle, String perimetre);

    Optional<ConfigurationVersionnee> versionCourante(String cle, String perimetre);
}

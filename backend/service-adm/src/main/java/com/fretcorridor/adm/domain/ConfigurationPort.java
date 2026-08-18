package com.fretcorridor.adm.domain;

import java.util.List;
import java.util.Optional;

public interface ConfigurationPort {
    void sauvegarder(ConfigurationVersionnee configuration);

    List<ConfigurationVersionnee> historique(String cle, String perimetre);

    Optional<ConfigurationVersionnee> versionCourante(String cle, String perimetre);

    /** EF-ADM-06 : une entrée par (clé, périmètre) déjà configuré, valeur courante (dernière version). */
    List<ConfigurationVersionnee> toutesLesVersionsCourantes();
}

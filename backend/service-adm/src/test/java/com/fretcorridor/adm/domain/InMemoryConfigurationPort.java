package com.fretcorridor.adm.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryConfigurationPort implements ConfigurationPort {

    private final List<ConfigurationVersionnee> configurations = new ArrayList<>();

    @Override
    public void sauvegarder(ConfigurationVersionnee configuration) {
        configurations.add(configuration);
    }

    @Override
    public List<ConfigurationVersionnee> historique(String cle, String perimetre) {
        return configurations.stream()
                .filter(c -> c.cle().equals(cle) && c.perimetre().equals(perimetre))
                .toList();
    }

    @Override
    public Optional<ConfigurationVersionnee> versionCourante(String cle, String perimetre) {
        List<ConfigurationVersionnee> historique = historique(cle, perimetre);
        return historique.isEmpty() ? Optional.empty() : Optional.of(historique.get(historique.size() - 1));
    }
}

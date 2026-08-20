package com.fretcorridor.adm.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public List<ConfigurationVersionnee> toutesLesVersionsCourantes() {
        return configurations.stream()
                .collect(Collectors.toMap(
                        c -> c.cle() + " " + c.perimetre(),
                        c -> c,
                        (a, b) -> a.version() >= b.version() ? a : b))
                .values().stream()
                .sorted(Comparator.comparing(ConfigurationVersionnee::cle).thenComparing(ConfigurationVersionnee::perimetre))
                .toList();
    }
}

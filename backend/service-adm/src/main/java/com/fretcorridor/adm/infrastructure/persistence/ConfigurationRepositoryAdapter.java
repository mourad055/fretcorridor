package com.fretcorridor.adm.infrastructure.persistence;

import com.fretcorridor.adm.domain.ConfigurationPort;
import com.fretcorridor.adm.domain.ConfigurationVersionnee;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ConfigurationRepositoryAdapter implements ConfigurationPort {

    private final ConfigurationJpaRepository repository;

    public ConfigurationRepositoryAdapter(ConfigurationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sauvegarder(ConfigurationVersionnee configuration) {
        repository.save(new ConfigurationEntity(configuration.id(), configuration.cle(), configuration.perimetre(),
                configuration.valeur(), configuration.auteur(), configuration.version(), configuration.creeLe()));
    }

    @Override
    public List<ConfigurationVersionnee> historique(String cle, String perimetre) {
        return repository.findByCleAndPerimetreOrderByVersionAsc(cle, perimetre).stream()
                .map(this::versDomaine)
                .toList();
    }

    @Override
    public Optional<ConfigurationVersionnee> versionCourante(String cle, String perimetre) {
        List<ConfigurationEntity> historique = repository.findByCleAndPerimetreOrderByVersionAsc(cle, perimetre);
        return historique.isEmpty() ? Optional.empty() : Optional.of(versDomaine(historique.get(historique.size() - 1)));
    }

    private ConfigurationVersionnee versDomaine(ConfigurationEntity entity) {
        return new ConfigurationVersionnee(entity.getId(), entity.getCle(), entity.getPerimetre(),
                entity.getValeur(), entity.getAuteur(), entity.getVersion(), entity.getCreeLe());
    }
}

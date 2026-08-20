package com.fretcorridor.adm.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** FE-ADM-03 : console de configuration versionnée et auditée des paramètres métier. */
public class ConfigurationService {

    private final ConfigurationPort configurationPort;
    private final JournalAuditPort journalAuditPort;

    public ConfigurationService(ConfigurationPort configurationPort, JournalAuditPort journalAuditPort) {
        this.configurationPort = configurationPort;
        this.journalAuditPort = journalAuditPort;
    }

    public ConfigurationVersionnee definir(String cle, String perimetre, String valeur, String auteur) {
        int prochaineVersion = configurationPort.historique(cle, perimetre).size() + 1;
        ConfigurationVersionnee configuration = new ConfigurationVersionnee(UUID.randomUUID().toString(), cle,
                perimetre, valeur, auteur, prochaineVersion, Instant.now());
        configurationPort.sauvegarder(configuration);
        journalAuditPort.enregistrer(new EntreeJournalAudit(UUID.randomUUID().toString(),
                ConfigurationVersionnee.PERIMETRE_GLOBAL.equals(perimetre) ? null : perimetre, auteur,
                "CONFIGURATION_MODIFIEE", "configuration:" + cle, Instant.now()));
        return configuration;
    }

    public Optional<ConfigurationVersionnee> valeurCourante(String cle, String perimetre) {
        return configurationPort.versionCourante(cle, perimetre);
    }

    public List<ConfigurationVersionnee> historique(String cle, String perimetre) {
        return configurationPort.historique(cle, perimetre);
    }

    /** EF-ADM-06 : catalogue de tous les paramètres métier déjà configurés, pour la console (pas besoin de connaître la clé à l'avance). */
    public List<ConfigurationVersionnee> catalogue() {
        return configurationPort.toutesLesVersionsCourantes();
    }
}

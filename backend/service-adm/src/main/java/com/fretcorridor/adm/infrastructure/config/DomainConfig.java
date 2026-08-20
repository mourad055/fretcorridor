package com.fretcorridor.adm.infrastructure.config;

import com.fretcorridor.adm.domain.ConfigurationPort;
import com.fretcorridor.adm.domain.ConfigurationService;
import com.fretcorridor.adm.domain.DecisionService;
import com.fretcorridor.adm.domain.DossierEventPort;
import com.fretcorridor.adm.domain.DossierPort;
import com.fretcorridor.adm.domain.EscaladeService;
import com.fretcorridor.adm.domain.FileTravailService;
import com.fretcorridor.adm.domain.JournalAuditPort;
import com.fretcorridor.adm.domain.JournalAuditService;
import com.fretcorridor.adm.domain.TenantPort;
import com.fretcorridor.adm.domain.TenantService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Câblage du domaine, seul point du code à connaître à la fois les ports et Spring. */
@Configuration
public class DomainConfig {

    @Bean
    public FileTravailService fileTravailService(DossierPort dossierPort, JournalAuditPort journalAuditPort, DossierEventPort dossierEventPort) {
        return new FileTravailService(dossierPort, journalAuditPort, dossierEventPort);
    }

    @Bean
    public DecisionService decisionService(DossierPort dossierPort, JournalAuditPort journalAuditPort,
                                            DossierEventPort dossierEventPort, ConfigurationPort configurationPort) {
        return new DecisionService(dossierPort, journalAuditPort, dossierEventPort, configurationPort);
    }

    @Bean
    public EscaladeService escaladeService(DossierPort dossierPort, JournalAuditPort journalAuditPort) {
        return new EscaladeService(dossierPort, journalAuditPort);
    }

    @Bean
    public ConfigurationService configurationService(ConfigurationPort configurationPort,
                                                       JournalAuditPort journalAuditPort) {
        return new ConfigurationService(configurationPort, journalAuditPort);
    }

    @Bean
    public TenantService tenantService(TenantPort tenantPort, JournalAuditPort journalAuditPort) {
        return new TenantService(tenantPort, journalAuditPort);
    }

    @Bean
    public JournalAuditService journalAuditService(JournalAuditPort journalAuditPort) {
        return new JournalAuditService(journalAuditPort);
    }
}

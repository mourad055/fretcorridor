package com.fretcorridor.opt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * BUG CORRIGE (2026-08-20, veille de presentation) : sans cette config,
 * @EnableScheduling utilise un ThreadPoolTaskScheduler par defaut a UN SEUL
 * thread. Ce service porte deux taches @Scheduled independantes
 * (MatchingCycleService.executerCycle a 15s, SequencementDeclencheur a 30s) -
 * avec un seul thread, l'une peut bloquer indefiniment l'autre si un appel
 * synchrone interne (service-geo/service-mat/Valhalla) tarde a repondre.
 * Symptome observe : silence total des deux logs periodiques apres un
 * enchainement de cycles avec dependances lentes/injoignables.
 *
 * Pool de 4 threads : large marge pour 2 taches actuelles + croissance
 * future (Phase 2+), cout memoire negligeable a cette echelle.
 */
@Configuration
public class SchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("opt-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}

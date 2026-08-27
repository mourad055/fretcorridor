package com.fretcorridor.bur.infrastructure.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Demo / dev uniquement : la vue Bureau (public.positions) est normalement
 * alimentée par Kafka position-eta (TRK → BUR). En environnement de démo,
 * une panne Kafka côté FLT (bootstrap localhost) ou des positions ingérées
 * avant correction laissent le portail Web sur des seeds figés — cet
 * alignement périodique sur service_flt.positions garantit la cohérence
 * visible avec l'app Chauffeur/Client (EF-TRK-04, RG-043), sans contourner
 * le pipeline événementiel en prod (profil dev seulement).
 */
@Component
@Profile("dev")
public class DevPositionFltSync {

    private static final Logger log = LoggerFactory.getLogger(DevPositionFltSync.class);

    /**
     * Dernière position FLT par mission, enrichie du vehicule affecté côté EXE.
     * ON CONFLICT (mission_id) : mise à jour en place, jamais de doublon.
     */
    private static final String SQL_SYNC = """
            INSERT INTO public.positions (id, mission_id, tenant_id, vehicule_id, latitude, longitude, captured_le)
            SELECT gen_random_uuid(), p.mission_id, p.tenant_id, m.vehicule_id, p.latitude, p.longitude, p.horodatage AT TIME ZONE 'UTC'
            FROM (
                SELECT DISTINCT ON (mission_id) mission_id, tenant_id, latitude, longitude, horodatage
                FROM service_flt.positions
                ORDER BY mission_id, horodatage DESC
            ) p
            LEFT JOIN service_exe.missions m ON m.id = p.mission_id
            ON CONFLICT (mission_id) DO UPDATE SET
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                captured_le = EXCLUDED.captured_le,
                vehicule_id = COALESCE(EXCLUDED.vehicule_id, public.positions.vehicule_id),
                tenant_id = EXCLUDED.tenant_id
            WHERE public.positions.captured_le < EXCLUDED.captured_le
            """;

    private final JdbcTemplate jdbcTemplate;

    public DevPositionFltSync(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchroniserAuDemarrage() {
        synchroniser();
    }

    @Scheduled(fixedRateString = "${fretcorridor.bur.dev-position-flt-sync-ms:15000}")
    public void synchroniserPeriodiquement() {
        synchroniser();
    }

    private void synchroniser() {
        try {
            int lignes = jdbcTemplate.update(SQL_SYNC);
            if (lignes > 0) {
                log.debug("Sync demo FLT→BUR : {} position(s) mise(s) à jour", lignes);
            }
        } catch (Exception e) {
            log.warn("Sync demo FLT→BUR ignorée ce tour : {}", e.getMessage());
        }
    }
}

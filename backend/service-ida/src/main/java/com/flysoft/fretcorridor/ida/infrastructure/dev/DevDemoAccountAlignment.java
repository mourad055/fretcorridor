package com.flysoft.fretcorridor.ida.infrastructure.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Profil dev uniquement : réaligne les comptes démo après création mobile
 * (PIN différent de 1234, tenant MARKETPLACE_CM, etc.) pour que le portail
 * web et l'app mobile partagent la même identité métier (PRD §5.3).
 */
@Component
@Profile("dev")
public class DevDemoAccountAlignment {

    private static final Logger log = LoggerFactory.getLogger(DevDemoAccountAlignment.class);

    /** PIN démo "1234" — identique à data-dev.sql. */
    private static final String PIN_DEMO_BCRYPT = "$2y$10$Ly67HRqbsix1/e8/MUcpzO35Y63be65zdMLJHoiTWWMkjb6Sg6.xK";

    private final JdbcTemplate jdbcTemplate;

    public DevDemoAccountAlignment(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alignerComptesDemo() {
        int chauffeur = jdbcTemplate.update("""
                UPDATE service_ida.acteurs SET
                    tenant_id = 'tenant-bgft-douala',
                    code_pin = ?,
                    nom = COALESCE(NULLIF(nom, ''), 'Kamga'),
                    prenom = COALESCE(NULLIF(prenom, ''), 'Jean'),
                    raison_sociale = COALESCE(NULLIF(raison_sociale, ''), 'Transport Étoile SARL'),
                    niveau_kyc = 'NIVEAU_2'
                WHERE telephone = '+237696000001'
                """, PIN_DEMO_BCRYPT);
        int roleTransporteur = jdbcTemplate.update("""
                INSERT INTO service_ida.acteur_roles (acteur_id, role)
                SELECT a.id, 'TRANSPORTEUR'
                FROM service_ida.acteurs a
                WHERE a.telephone = '+237696000001'
                  AND NOT EXISTS (
                    SELECT 1 FROM service_ida.acteur_roles ar
                    WHERE ar.acteur_id = a.id AND ar.role = 'TRANSPORTEUR'
                  )
                """);
        int chauffeurMobile = jdbcTemplate.update("""
                UPDATE service_ida.acteurs SET
                    tenant_id = 'tenant-bgft-douala',
                    code_pin = ?
                WHERE telephone = '+237600000010'
                """, PIN_DEMO_BCRYPT);
        int rolesCorriges = jdbcTemplate.update("""
                DELETE FROM service_ida.acteur_roles ar
                USING service_ida.acteurs a
                WHERE ar.acteur_id = a.id
                  AND a.telephone = '+237600000010'
                  AND ar.role IN ('TRANSPORTEUR', 'CHARGEUR')
                """);
        int roleChauffeur = jdbcTemplate.update("""
                INSERT INTO service_ida.acteur_roles (acteur_id, role)
                SELECT a.id, 'CHAUFFEUR'
                FROM service_ida.acteurs a
                WHERE a.telephone = '+237600000010'
                  AND NOT EXISTS (
                    SELECT 1 FROM service_ida.acteur_roles ar
                    WHERE ar.acteur_id = a.id AND ar.role = 'CHAUFFEUR'
                  )
                """);
        log.info(
                "Alignement comptes demo IDA : chauffeur={}, roleTransporteur={}, chauffeurMobile={}, rolesCorriges={}, roleChauffeur={}",
                chauffeur, roleTransporteur, chauffeurMobile, rolesCorriges, roleChauffeur);
    }
}

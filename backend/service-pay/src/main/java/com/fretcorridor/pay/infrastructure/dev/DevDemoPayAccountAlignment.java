package com.fretcorridor.pay.infrastructure.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Profil dev : garantit des écritures de paiement pour le transporteur mobile
 * (+237696000001) même si service-pay démarre avant service-ida ou si le compte
 * a été créé via inscription mobile (UUID non fixe).
 */
@Component
@Profile("dev")
public class DevDemoPayAccountAlignment {

    private static final Logger log = LoggerFactory.getLogger(DevDemoPayAccountAlignment.class);
    private static final String TELEPHONE_MOBILE_LIVE = "+237696000001";

    private final JdbcTemplate jdbcTemplate;

    public DevDemoPayAccountAlignment(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alignerPaiementsDemoMobile() {
        int sequestres = jdbcTemplate.update("""
                INSERT INTO sequestres (mission_id, etat, declenche_le, libere_le, tenant_id, transporteur_id, preuve_livraison_reference)
                SELECT v.mission_id, 'LIBERE', v.declenche_le, v.libere_le, 'tenant-bgft-douala', a.id, v.preuve
                FROM service_ida.acteurs a
                CROSS JOIN (VALUES
                    ('mission-demo-mobile-live-1', now() - interval '5 hours', now() - interval '4 hours', 'POD-DEMO-MOBILE-LIVE-1'),
                    ('mission-demo-mobile-live-2', now() - interval '2 days', now() - interval '2 days' + interval '2 hours', 'POD-DEMO-MOBILE-LIVE-2')
                ) AS v(mission_id, declenche_le, libere_le, preuve)
                WHERE a.telephone = ?
                ON CONFLICT (mission_id) DO NOTHING
                """, TELEPHONE_MOBILE_LIVE);

        int ecritures = jdbcTemplate.update("""
                INSERT INTO ecritures_miroir (id, tenant_id, mission_id, type_compte, beneficiaire_id, sens, nature, mode_paiement, montant, reference_prestataire, cree_le, statut)
                SELECT v.id, 'tenant-bgft-douala', v.mission_id, v.type_compte,
                       CASE WHEN v.type_compte = 'COMPTE_TRANSPORTEUR' THEN a.id ELSE NULL END,
                       v.sens, v.nature, v.mode_paiement, v.montant, v.reference_prestataire, v.cree_le, 'VALIDE'
                FROM service_ida.acteurs a
                CROSS JOIN (VALUES
                    ('60000000-0000-0000-0000-00000000000b', 'mission-demo-mobile-live-1', 'COMPTE_SEQUESTRE_PRESTATAIRE', 'CREDIT', 'ENCAISSEMENT', 'MONNAIE_ELECTRONIQUE', 165000.00, 'PRESTA-DEMO-MOBILE-LIVE-1', now() - interval '4 hours'),
                    ('60000000-0000-0000-0000-00000000000c', 'mission-demo-mobile-live-1', 'COMPTE_TRANSPORTEUR',          'DEBIT',  'REVERSEMENT',  NULL,                    165000.00, 'PRESTA-DEMO-MOBILE-LIVE-1', now() - interval '3 hours'),
                    ('60000000-0000-0000-0000-00000000000d', 'mission-demo-mobile-live-2', 'COMPTE_SEQUESTRE_PRESTATAIRE', 'CREDIT', 'ENCAISSEMENT', 'MONNAIE_ELECTRONIQUE', 92000.00,  'PRESTA-DEMO-MOBILE-LIVE-2', now() - interval '2 days' + interval '1 hour'),
                    ('60000000-0000-0000-0000-00000000000e', 'mission-demo-mobile-live-2', 'COMPTE_TRANSPORTEUR',          'DEBIT',  'REVERSEMENT',  NULL,                    92000.00,  'PRESTA-DEMO-MOBILE-LIVE-2', now() - interval '2 days' + interval '2 hours')
                ) AS v(id, mission_id, type_compte, sens, nature, mode_paiement, montant, reference_prestataire, cree_le)
                WHERE a.telephone = ?
                ON CONFLICT (id) DO NOTHING
                """, TELEPHONE_MOBILE_LIVE);

        log.info("Alignement paiements demo mobile : sequestres={}, ecritures={}", sequestres, ecritures);
    }
}

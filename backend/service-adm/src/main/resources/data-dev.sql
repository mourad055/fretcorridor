-- Données de démonstration pour service-adm — chargées UNIQUEMENT sous le
-- profil "dev" (cf. application-dev.yml), jamais en production.
--
-- Tenants institutionnels + dossiers en retard pour alimenter le centre de
-- notifications Admin (FE-ADM, audit UX 2026-08-23 §2.6).
-- Idempotent (ON CONFLICT DO NOTHING).

INSERT INTO public.adm_tenant (id, nom, pays, actif)
VALUES
    ('tenant-bgft-douala',   'Bureau de fret BGFT Douala', 'Cameroun', true),
    ('tenant-bnft-ndjamena', 'BNFT N''Djamena',            'Tchad',    true),
    ('tenant-flysoft',       'Flysoft Engineering',        'Cameroun', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.adm_dossier (
    id, tenant_id, type, priorite, statut, mission_id, motif, description,
    ouvert_le, delai_traitement, prise_en_charge_par_acteur_id, decision,
    motif_decision, decide_par, decide_le, grille_version_appliquee, recours_de_dossier_id
)
VALUES
    (
        '60000000-0000-0000-0000-000000000001',
        'tenant-bgft-douala',
        'LITIGE',
        'HAUTE',
        'OUVERT',
        '30000000-0000-0000-0000-000000000001',
        'Contestation tarif transport',
        'Litige ouvert sur une mission Douala → Yaoundé — délai de traitement dépassé (démo).',
        now() - interval '5 days',
        now() - interval '2 days',
        NULL, NULL, NULL, NULL, NULL, NULL, NULL
    ),
    (
        '60000000-0000-0000-0000-000000000002',
        'tenant-bnft-ndjamena',
        'INCIDENT',
        'NORMALE',
        'EN_COURS',
        NULL,
        'Retard de livraison signalé',
        'Incident corridor transfrontalier — délai dépassé (démo).',
        now() - interval '4 days',
        now() - interval '1 day',
        'a0000000-0000-0000-0000-000000000003',
        NULL, NULL, NULL, NULL, NULL, NULL
    )
ON CONFLICT (id) DO NOTHING;

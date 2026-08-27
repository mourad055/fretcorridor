-- Données de démonstration pour service-pay — chargées UNIQUEMENT sous le
-- profil "dev" (cf. application-dev.yml), jamais en production.
--
-- Reproduit exactement ce que produirait le cycle réel prise-en-charge →
-- clôture → reversement (cf. PaiementController) pour 2 missions par tenant,
-- afin que "Rapport financier" (Bureau/Administration) et "Paiement"
-- (Transporteur) ne soient jamais vides en environnement de démo.
--
-- transporteur_id/beneficiaire_id reprennent les comptes de
-- backend/service-ida/src/main/resources/data-dev.sql.
--
-- Idempotent (ON CONFLICT DO NOTHING).

INSERT INTO sequestres (mission_id, etat, declenche_le, libere_le, tenant_id, transporteur_id, preuve_livraison_reference)
VALUES
    ('mission-demo-bgft-1', 'LIBERE', now() - interval '2 hours', now() - interval '1 hour', 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000002', 'POD-DEMO-BGFT-1'),
    ('mission-demo-bgft-2', 'LIBERE', now() - interval '3 hours', now() - interval '2 hours', 'tenant-bgft-douala',   'a0000000-0000-0000-0000-000000000005', 'POD-DEMO-BGFT-2'),
    ('mission-demo-bnft-1', 'LIBERE', now() - interval '2 hours', now() - interval '1 hour', 'tenant-bnft-ndjamena', 'a0000000-0000-0000-0000-000000000006', 'POD-DEMO-BNFT-1'),
    ('mission-demo-bnft-2', 'LIBERE', now() - interval '3 hours', now() - interval '2 hours', 'tenant-bnft-ndjamena', 'a0000000-0000-0000-0000-000000000006', 'POD-DEMO-BNFT-2')
ON CONFLICT (mission_id) DO NOTHING;

INSERT INTO ecritures_miroir (id, tenant_id, mission_id, type_compte, beneficiaire_id, sens, nature, mode_paiement, montant, reference_prestataire, cree_le, statut)
VALUES
    ('60000000-0000-0000-0000-000000000001', 'tenant-bgft-douala',   'mission-demo-bgft-1', 'COMPTE_SEQUESTRE_PRESTATAIRE', NULL,                                       'CREDIT', 'ENCAISSEMENT', 'MONNAIE_ELECTRONIQUE', 175000.00, 'PRESTA-DEMO-BGFT-1', now() - interval '2 hours', 'VALIDE'),
    ('60000000-0000-0000-0000-000000000002', 'tenant-bgft-douala',   'mission-demo-bgft-1', 'COMPTE_TRANSPORTEUR',          'a0000000-0000-0000-0000-000000000002',    'DEBIT',  'REVERSEMENT',  NULL,                    175000.00, 'PRESTA-DEMO-BGFT-1', now() - interval '1 hour',  'VALIDE'),
    ('60000000-0000-0000-0000-000000000003', 'tenant-bgft-douala',   'mission-demo-bgft-2', 'COMPTE_SEQUESTRE_PRESTATAIRE', NULL,                                       'CREDIT', 'ENCAISSEMENT', 'MONNAIE_ELECTRONIQUE', 200000.00, 'PRESTA-DEMO-BGFT-2', now() - interval '3 hours', 'VALIDE'),
    ('60000000-0000-0000-0000-000000000004', 'tenant-bgft-douala',   'mission-demo-bgft-2', 'COMPTE_TRANSPORTEUR',          'a0000000-0000-0000-0000-000000000005',    'DEBIT',  'REVERSEMENT',  NULL,                    200000.00, 'PRESTA-DEMO-BGFT-2', now() - interval '2 hours', 'VALIDE'),
    ('60000000-0000-0000-0000-000000000005', 'tenant-bnft-ndjamena', 'mission-demo-bnft-1', 'COMPTE_SEQUESTRE_PRESTATAIRE', NULL,                                       'CREDIT', 'ENCAISSEMENT', 'MONNAIE_ELECTRONIQUE', 210000.00, 'PRESTA-DEMO-BNFT-1', now() - interval '2 hours', 'VALIDE'),
    ('60000000-0000-0000-0000-000000000006', 'tenant-bnft-ndjamena', 'mission-demo-bnft-1', 'COMPTE_TRANSPORTEUR',          'a0000000-0000-0000-0000-000000000006',    'DEBIT',  'REVERSEMENT',  NULL,                    210000.00, 'PRESTA-DEMO-BNFT-1', now() - interval '1 hour',  'VALIDE'),
    ('60000000-0000-0000-0000-000000000007', 'tenant-bnft-ndjamena', 'mission-demo-bnft-2', 'COMPTE_SEQUESTRE_PRESTATAIRE', NULL,                                       'CREDIT', 'ENCAISSEMENT', 'MONNAIE_ELECTRONIQUE', 240000.00, 'PRESTA-DEMO-BNFT-2', now() - interval '3 hours', 'VALIDE'),
    ('60000000-0000-0000-0000-000000000008', 'tenant-bnft-ndjamena', 'mission-demo-bnft-2', 'COMPTE_TRANSPORTEUR',          'a0000000-0000-0000-0000-000000000006',    'DEBIT',  'REVERSEMENT',  NULL,                    240000.00, 'PRESTA-DEMO-BNFT-2', now() - interval '2 hours', 'VALIDE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO declarations_especes (id, tenant_id, mission_id, montant, declaree_le)
VALUES
    ('60000000-0000-0000-0000-000000000009', 'tenant-bgft-douala',   'mission-demo-bgft-3', 85000.00, now() - interval '30 minutes'),
    ('60000000-0000-0000-0000-00000000000a', 'tenant-bnft-ndjamena', 'mission-demo-bnft-3', 60000.00, now() - interval '30 minutes')
ON CONFLICT (mission_id) DO NOTHING;

-- Compte mobile live (+237696000001) — portail web Transporteur (PRD §5.3).
-- beneficiaire_id résolu par téléphone service-ida (UUID réel, pas a0000000-...).
INSERT INTO sequestres (mission_id, etat, declenche_le, libere_le, tenant_id, transporteur_id, preuve_livraison_reference)
SELECT
    'mission-demo-mobile-live-1',
    'LIBERE',
    now() - interval '5 hours',
    now() - interval '4 hours',
    'tenant-bgft-douala',
    a.id,
    'POD-DEMO-MOBILE-LIVE-1'
FROM service_ida.acteurs a
WHERE a.telephone = '+237696000001'
ON CONFLICT (mission_id) DO NOTHING;

INSERT INTO ecritures_miroir (id, tenant_id, mission_id, type_compte, beneficiaire_id, sens, nature, mode_paiement, montant, reference_prestataire, cree_le, statut)
SELECT
    '60000000-0000-0000-0000-00000000000b',
    'tenant-bgft-douala',
    'mission-demo-mobile-live-1',
    'COMPTE_SEQUESTRE_PRESTATAIRE',
    NULL,
    'CREDIT',
    'ENCAISSEMENT',
    'MONNAIE_ELECTRONIQUE',
    165000.00,
    'PRESTA-DEMO-MOBILE-LIVE-1',
    now() - interval '4 hours',
    'VALIDE'
FROM service_ida.acteurs a
WHERE a.telephone = '+237696000001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO ecritures_miroir (id, tenant_id, mission_id, type_compte, beneficiaire_id, sens, nature, mode_paiement, montant, reference_prestataire, cree_le, statut)
SELECT
    '60000000-0000-0000-0000-00000000000c',
    'tenant-bgft-douala',
    'mission-demo-mobile-live-1',
    'COMPTE_TRANSPORTEUR',
    a.id,
    'DEBIT',
    'REVERSEMENT',
    NULL,
    165000.00,
    'PRESTA-DEMO-MOBILE-LIVE-1',
    now() - interval '3 hours',
    'VALIDE'
FROM service_ida.acteurs a
WHERE a.telephone = '+237696000001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO sequestres (mission_id, etat, declenche_le, libere_le, tenant_id, transporteur_id, preuve_livraison_reference)
SELECT
    'mission-demo-mobile-live-2',
    'LIBERE',
    now() - interval '2 days',
    now() - interval '2 days' + interval '2 hours',
    'tenant-bgft-douala',
    a.id,
    'POD-DEMO-MOBILE-LIVE-2'
FROM service_ida.acteurs a
WHERE a.telephone = '+237696000001'
ON CONFLICT (mission_id) DO NOTHING;

INSERT INTO ecritures_miroir (id, tenant_id, mission_id, type_compte, beneficiaire_id, sens, nature, mode_paiement, montant, reference_prestataire, cree_le, statut)
SELECT
    '60000000-0000-0000-0000-00000000000d',
    'tenant-bgft-douala',
    'mission-demo-mobile-live-2',
    'COMPTE_SEQUESTRE_PRESTATAIRE',
    NULL,
    'CREDIT',
    'ENCAISSEMENT',
    'MONNAIE_ELECTRONIQUE',
    92000.00,
    'PRESTA-DEMO-MOBILE-LIVE-2',
    now() - interval '2 days' + interval '1 hour',
    'VALIDE'
FROM service_ida.acteurs a
WHERE a.telephone = '+237696000001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO ecritures_miroir (id, tenant_id, mission_id, type_compte, beneficiaire_id, sens, nature, mode_paiement, montant, reference_prestataire, cree_le, statut)
SELECT
    '60000000-0000-0000-0000-00000000000e',
    'tenant-bgft-douala',
    'mission-demo-mobile-live-2',
    'COMPTE_TRANSPORTEUR',
    a.id,
    'DEBIT',
    'REVERSEMENT',
    NULL,
    92000.00,
    'PRESTA-DEMO-MOBILE-LIVE-2',
    now() - interval '2 days' + interval '2 hours',
    'VALIDE'
FROM service_ida.acteurs a
WHERE a.telephone = '+237696000001'
ON CONFLICT (id) DO NOTHING;

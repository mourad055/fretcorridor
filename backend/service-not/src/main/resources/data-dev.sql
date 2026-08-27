-- Données de démonstration pour service-not — profil dev uniquement.
-- Notifications in-app pour les comptes Bureau démo (résolution par téléphone IDA).

INSERT INTO service_not.notifications (
    id, destinataire_acteur_id, titre, corps, type, tenant_id, lue, date_creation
)
SELECT
    '70000000-0000-0000-0000-000000000001'::uuid,
    a.id,
    'Nouvelle mission appariée',
    'La mission Douala → Yaoundé a été appariée à un transporteur.',
    'STATUT_MISSION',
    'tenant-bgft-douala',
    false,
    now() - interval '2 hours'
FROM service_ida.acteurs a
WHERE a.telephone = '+237600000001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_not.notifications (
    id, destinataire_acteur_id, titre, corps, type, tenant_id, lue, date_creation
)
SELECT
    '70000000-0000-0000-0000-000000000002'::uuid,
    a.id,
    'Écart de réconciliation détecté',
    'Une alerte de réconciliation a été levée sur la mission mission-demo-bgft-1.',
    'ALERTE_ECART',
    'tenant-bgft-douala',
    false,
    now() - interval '1 day'
FROM service_ida.acteurs a
WHERE a.telephone = '+237600000001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_not.notifications (
    id, destinataire_acteur_id, titre, corps, type, tenant_id, lue, date_creation
)
SELECT
    '70000000-0000-0000-0000-000000000003'::uuid,
    a.id,
    'Dossier KYC validé',
    'Le dossier KYC d''un transporteur de votre territoire a été validé.',
    'INFO_GENERALE',
    'tenant-bnft-ndjamena',
    false,
    now() - interval '3 hours'
FROM service_ida.acteurs a
WHERE a.telephone = '+235600000004'
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_not.notifications (
    id, destinataire_acteur_id, titre, corps, type, tenant_id, lue, date_creation
)
SELECT
    '70000000-0000-0000-0000-000000000004'::uuid,
    a.id,
    'Nouvelle mission appariée',
    'La mission N''Djamena → Sarh a été appariée à un transporteur.',
    'STATUT_MISSION',
    'tenant-bnft-ndjamena',
    false,
    now() - interval '45 minutes'
FROM service_ida.acteurs a
WHERE a.telephone = '+235600000004'
ON CONFLICT (id) DO NOTHING;

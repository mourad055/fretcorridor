-- Mission de démonstration pour le compte transporteur web (+237600000002).
-- transporteur_id résolu par téléphone service-ida (UUID réel, pas a0000000-...).
-- Idempotent : ON CONFLICT (id) DO NOTHING.

INSERT INTO service_exe.missions (
    id, demande_id, transporteur_id, vehicule_id, axe_id,
    origine_nom, destination_nom, type_emballage_nom, quantite,
    poids_taxable_kg, tenant_id, statut, date_creation
)
SELECT
    '60000000-0000-0000-0000-000000000010'::uuid,
    '60000000-0000-0000-0000-000000000011'::uuid,
    a.id,
    '50000000-0000-0000-0000-000000000001'::uuid,
    '20000000-0000-0000-0000-000000000001'::uuid,
    'Douala',
    'Yaoundé',
    'Palette',
    12,
    9500.00,
    'tenant-bgft-douala',
    'PRISE_EN_CHARGE',
    now()
FROM service_ida.acteurs a
WHERE a.telephone = '+237600000002'
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_exe.etapes_mission (
    id, mission_id, type, libelle, horodatage_capture, horodatage_transmission
)
SELECT
    '60000000-0000-0000-0000-000000000012'::uuid,
    '60000000-0000-0000-0000-000000000010'::uuid,
    'PRISE_EN_CHARGE',
    'Enlèvement Douala',
    now() - interval '1 hour',
    now() - interval '55 minutes'
WHERE EXISTS (
    SELECT 1 FROM service_exe.missions WHERE id = '60000000-0000-0000-0000-000000000010'::uuid
)
AND NOT EXISTS (
    SELECT 1 FROM service_exe.etapes_mission WHERE id = '60000000-0000-0000-0000-000000000012'::uuid
);

-- Mission clôturée pour le transporteur mobile live (+237696000001).
INSERT INTO service_exe.missions (
    id, demande_id, transporteur_id, vehicule_id, axe_id,
    origine_nom, destination_nom, type_emballage_nom, quantite,
    poids_taxable_kg, tenant_id, statut, date_creation
)
SELECT
    '60000000-0000-0000-0000-000000000020'::uuid,
    '60000000-0000-0000-0000-000000000021'::uuid,
    a.id,
    '50000000-0000-0000-0000-000000000001'::uuid,
    '20000000-0000-0000-0000-000000000003'::uuid,
    'Douala',
    'Yaoundé',
    'Palette',
    8,
    8500.00,
    'tenant-bgft-douala',
    'LIVREE',
    now() - interval '3 hours'
FROM service_ida.acteurs a
WHERE a.telephone = '+237696000001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO service_exe.etapes_mission (
    id, mission_id, type, libelle, horodatage_capture, horodatage_transmission
)
SELECT
    '60000000-0000-0000-0000-000000000022'::uuid,
    '60000000-0000-0000-0000-000000000020'::uuid,
    'LIVRAISON',
    'Livraison Yaoundé',
    now() - interval '3 hours',
    now() - interval '2 hours' + interval '45 minutes'
WHERE EXISTS (
    SELECT 1 FROM service_exe.missions WHERE id = '60000000-0000-0000-0000-000000000020'::uuid
)
AND NOT EXISTS (
    SELECT 1 FROM service_exe.etapes_mission WHERE id = '60000000-0000-0000-0000-000000000022'::uuid
);

-- Capacités de démonstration pour les comptes transporteur web (+237600000002,
-- +237600000005). transporteur_id résolu par téléphone service-ida pour rester
-- cohérent même si l'UUID réel diffère du seed a0000000-... historique.
--
-- axe_id reprend backend/service-geo/src/main/resources/data-dev.sql.
-- Idempotent : ON CONFLICT (id) DO NOTHING.

INSERT INTO cap.capacite (
    id, vehicule_id, axe_id, tenant_id, transporteur_id, mode_declaration,
    poids_kg, poids_taxable_kg, capacite_residuelle_kg,
    origine_latitude, origine_longitude, type_vehicule,
    date_depart, expiree, publiee, date_creation, version
)
SELECT
    v.id,
    v.vehicule_id,
    v.axe_id,
    v.tenant_id,
    a.id,
    v.mode_declaration,
    v.poids_kg,
    v.poids_taxable_kg,
    v.capacite_residuelle_kg,
    v.origine_latitude,
    v.origine_longitude,
    v.type_vehicule,
    v.date_depart,
    false,
    true,
    now(),
    0
FROM (VALUES
    (
        '60000000-0000-0000-0000-000000000001'::uuid,
        '50000000-0000-0000-0000-000000000001'::uuid,
        '20000000-0000-0000-0000-000000000001'::uuid,
        'tenant-bgft-douala',
        '+237600000002',
        'TOTALE',
        9500.00,
        9500.00,
        9500.00,
        4.0511,
        9.7679,
        'Camion 10T',
        now() + interval '1 day'
    ),
    (
        '60000000-0000-0000-0000-000000000002'::uuid,
        '50000000-0000-0000-0000-000000000001'::uuid,
        '20000000-0000-0000-0000-000000000003'::uuid,
        'tenant-bgft-douala',
        '+237600000002',
        'PRECISE',
        9500.00,
        6200.00,
        6200.00,
        3.8480,
        11.5021,
        'Camion 10T',
        now() + interval '3 days'
    ),
    (
        '60000000-0000-0000-0000-000000000003'::uuid,
        '50000000-0000-0000-0000-000000000002'::uuid,
        '20000000-0000-0000-0000-000000000002'::uuid,
        'tenant-bgft-douala',
        '+237600000005',
        'TOTALE',
        2800.00,
        2800.00,
        2800.00,
        4.0511,
        9.7679,
        'Fourgon 3T',
        now() + interval '2 days'
    )
) AS v(id, vehicule_id, axe_id, tenant_id, telephone, mode_declaration,
       poids_kg, poids_taxable_kg, capacite_residuelle_kg,
       origine_latitude, origine_longitude, type_vehicule, date_depart)
JOIN service_ida.acteurs a ON a.telephone = v.telephone
ON CONFLICT (id) DO NOTHING;

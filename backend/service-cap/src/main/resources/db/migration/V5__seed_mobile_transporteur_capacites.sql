-- Capacités pour le chauffeur mobile (+237696000001) — même acteurId que les
-- missions live service-exe, visible côté portail web transporteur.
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
        '60000000-0000-0000-0000-000000000004'::uuid,
        '50000000-0000-0000-0000-000000000001'::uuid,
        '20000000-0000-0000-0000-000000000003'::uuid,
        'tenant-bgft-douala',
        '+237696000001',
        'TOTALE',
        8500.00,
        8500.00,
        8500.00,
        3.8480,
        11.5021,
        'Camion 10T',
        now() + interval '2 days'
    )
) AS v(id, vehicule_id, axe_id, tenant_id, telephone, mode_declaration,
       poids_kg, poids_taxable_kg, capacite_residuelle_kg,
       origine_latitude, origine_longitude, type_vehicule, date_depart)
JOIN service_ida.acteurs a ON a.telephone = v.telephone
ON CONFLICT (id) DO NOTHING;

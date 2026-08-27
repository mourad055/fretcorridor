-- Barèmes de démonstration (CDC §8.9 / EF-GEO-02 / RG-112).
-- Sans au moins un barème actif, TarificationL4Service passe en mode dégradé
-- (prixTransport=null) et l'app Client affiche « Prix en cours de calcul ».
-- Idempotent : les UUID sont stables, ON CONFLICT DO NOTHING.
-- Valeurs de démo uniquement — à remplacer par les barèmes métier en exploitation.

INSERT INTO opt.bareme_tarification (
    id, axe_id, version, actif, regime,
    cout_base_par_km, cout_socle_forfaitaire, cout_unitaire_poids_taxable,
    prix_plancher_actif, prix_plancher,
    tension_min_fraction, tension_max_fraction,
    commission_taux_fraction, type_vehicule, description, date_creation
)
VALUES
    (
        '30000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        1, true, 'POIDS_TAXABLE',
        750.0000, NULL, 12.5000,
        true, 25000.0000,
        -0.1000, 0.3000,
        0.1500, 'FOURGON',
        'Barème de démonstration Douala -> Yaoundé / Fourgon',
        now()
    ),
    (
        '30000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000001',
        1, true, 'POIDS_TAXABLE',
        700.0000, NULL, 12.0000,
        true, 24000.0000,
        -0.1000, 0.3000,
        0.1500, 'CAMION_10T',
        'Barème de démonstration Douala -> Yaoundé / Camion 10T',
        now()
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        NULL,
        1, true, 'POIDS_TAXABLE',
        650.0000, NULL, 11.5000,
        true, 22000.0000,
        -0.1000, 0.3000,
        0.1500, NULL,
        'Barème global de démonstration pour les axes non spécifiques',
        now()
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO opt.composant_cout_service (id, bareme_id, code_service, montant)
VALUES
    ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'MANUTENTION', 1800.0000),
    ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 'ATTENTE', 900.0000),
    ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', 'GARDE', 1200.0000),
    ('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000002', 'MANUTENTION', 1800.0000),
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000002', 'ATTENTE', 900.0000),
    ('40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000002', 'GARDE', 1200.0000),
    ('40000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000003', 'MANUTENTION', 1700.0000),
    ('40000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000003', 'ATTENTE', 850.0000),
    ('40000000-0000-0000-0000-000000000009', '30000000-0000-0000-0000-000000000003', 'GARDE', 1100.0000)
ON CONFLICT (id) DO NOTHING;

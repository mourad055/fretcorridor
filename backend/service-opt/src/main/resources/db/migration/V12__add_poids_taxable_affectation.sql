-- EF-MAT-05/07 (Sprint 11, capacite dynamique CDC S8.6.1 point 3) - L1
-- calcule deja poidsTaxableKg (DemandeAvecCandidats) mais ne le persistait
-- jamais dans Affectation : sans lui, L2 ne peut pas verifier la charge
-- tout au long d'une tournee consolidee. Corrige avant d'ecrire l'ALNS
-- plutot qu'apres (consigne : corriger directement les manques Phase 1
-- decouverts en codant la Phase 2).
ALTER TABLE opt.affectation ADD COLUMN poids_taxable_kg NUMERIC(12,3);

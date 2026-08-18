-- Sprint 12 (EF-MAT-06/RG-107, CDC S8.6.3) : fenetre temporelle souple de
-- collecte/livraison. Nullable : absent tant que Mobile ne publie pas ce
-- champ dans DemandePubliee (mode permissif, meme principe que
-- rayonAppariementKm - jamais une valeur par defaut inventee).
ALTER TABLE opt.demande_en_attente
    ADD COLUMN fenetre_debut TIMESTAMPTZ,
    ADD COLUMN fenetre_fin   TIMESTAMPTZ;

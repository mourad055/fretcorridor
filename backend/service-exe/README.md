# service-exe

Porté par : Mobile (Personne 1).
Exécution de mission, étapes, preuves, incidents.

Consomme `AffectationConfirmee` (service-opt) pour créer la Mission,
`TourneeConstituee` (service-opt, S11) pour la rattacher à une tournée
consolidée (LTL). Publie `MissionLivree` (déclenche la libération du
séquestre côté `service-pay`) et `EtapeExecutee` (S12, ferme EF-MAT-09 et
conditionne le retour à vide côté `service-opt`) à chaque étape confirmée
par le chauffeur.

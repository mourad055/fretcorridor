# Plan de travail — 22/08/2026 (session soir)

Basé sur tes images collées à la racine du projet (3 captures d'écran + 5 mockups) et ton message du 22/08 ~12:25.

## Ce que j'ai compris de chaque mockup

- **Mes missions** (`mockupmesmissions.jpeg`, 1 seul mockup) : carte enrichie avec icône camion, ID de mission, marchandise, disponibilité/collecte, destinataire, poids total, type, date de publication, badge "Grande valeur".
- **Suivi de ma livraison** (2 mockups) : `suividemalivraisonun.jpeg` a la mise en page que tu préfères (lignes simples icône + texte) ; `mockupsuividemalivraison2.jpeg` a les icônes que tu préfères (badges circulaires colorés). → je combine : mise en page du 1er, style d'icônes du 2e.
- **Propositions** (2 mockups) : même logique — mise en page identique dans les deux, mais `mockuppropositions2.jpeg` a des icônes sur chaque ligne (pin rouge, horloge, personne, calendrier) que `mockuppropositionsun.jpeg` n'a pas. → j'utilise la version avec icônes.
- **Plan de chargement** (capture d'écran) : ce n'est pas un bug — c'est un écran de démonstration explicitement marqué "Démonstration — plan simulé en attendant l'oracle de chargement 3D côté service-opt V2". Le vrai calcul de répartition par essieu n'existe pas encore côté moteur ; l'écran affiche une simulation pour donner une idée du rendu final. Je ne le modifie pas sauf si tu me dis le contraire.

## Ce que je vais faire

### Backend (propagation d'infos, même schéma que déjà fait pour marchandise/destinataire)
- [x] Propager `modeCollecte`, `typeDisponibilite`, `poidsTotalKg`, `grandeValeur` depuis `Demande` (service-mkt) jusqu'à `Mission` (service-exe), en passant par service-opt — nécessaire pour que "Mes missions" (app Chauffeur) affiche tout ce que le mockup demande.
- [x] Migration Flyway service-opt pour les nouvelles colonnes sur `demande_en_attente`.

### App Chauffeur/Transporteur
- [x] Refaire la carte de `missions_screen.dart` selon `mockupmesmissions.jpeg` : icône camion dans un encart, ID de mission, ligne marchandise, ligne disponibilité/collecte, ligne destinataire, ligne poids total, ligne type, ligne date de publication, badge "Grande valeur" si applicable.

### App Client (Marketplace)
- [x] `suivi_screen.dart` — carte demande : ajouter les lignes manquantes (disponibilité/collecte, publiée le) + badges d'icônes circulaires colorées (style mockup 2), en gardant la mise en page compacte du mockup 1.
- [x] `propositions_screen.dart` — carte demande en tête : ajouter les lignes manquantes (disponibilité/collecte, publiée le) + icônes sur chaque ligne (pin/horloge/personne/calendrier), comme `mockuppropositions2.jpeg`.

### Scénario de test paiement (à suivre point par point une fois les apps relancées)
1. App Marketplace : publier une nouvelle demande (axe Yaoundé → Douala ou Douala → Yaoundé).
2. App Chauffeur : déclarer une capacité sur le **même axe**, avec un poids/volume suffisant.
3. Attendre le prochain cycle de matching (jusqu'à ~30s) — une proposition doit apparaître dans "Propositions" côté Marketplace.
4. App Marketplace : accepter la proposition.
5. App Chauffeur : ouvrir "Mes missions" → la mission doit apparaître avec statut "En attente", puis taper dessus → "Prise en charge" (photo + signature obligatoires).
6. App Marketplace : ouvrir "Suivi" sur la demande → la section paiement ("Choisir le moyen de paiement") doit maintenant être visible sous la chronologie.
7. Choisir un moyen de paiement (MoMo / Orange Money / Espèces) → confirmer que le choix est enregistré (message de confirmation vert).

## Notes
- Le "Plan de chargement" n'est PAS dans le scope de ce correctif (voir explication ci-dessus) — dis-moi si tu veux que je le retire du menu ou que je le laisse en l'état.
- `service-pay` n'avait jamais été démarré avant ce soir — c'est fait, mais son flux (choix du moyen de paiement) n'a pas encore été testé de bout en bout ; c'est justement l'objet du scénario ci-dessus.

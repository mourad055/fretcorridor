# ADR 0010 — Tenant de démonstration Tchad renommé BGFT → BNFT

**Statut** : Accepté

## Contexte

Depuis le Sprint 3, un second tenant de démonstration existe uniquement pour
prouver l'isolation multi-tenant (ENF-MUL-01) : un Bureau A ne doit jamais
voir les données d'un Bureau B. Ce tenant portait l'identifiant
`tenant-bgft-tchad` — réutilisant l'acronyme **BGFT** (Bureau de Gestion du
Fret Terrestre), qui désigne en réalité l'institution camerounaise de
Douala, pour une entité tchadienne. Incohérence de nommage, sans impact
fonctionnel tant que ce tenant ne servait qu'aux tests d'isolation, mais
gênante dès qu'on veut le présenter comme un compte de démonstration à part
entière.

Ce tenant était par ailleurs volontairement minimal (1 seule entrée par
mock : axe, mission, notification, position) contre 2-3 pour le tenant
Douala, et son compte Bureau (`+235600000004`) n'était pas exposé comme
bouton de connexion rapide sur `/login`.

## Décision

- Renommage mécanique de `tenant-bgft-tchad` en `tenant-bnft-ndjamena` dans
  les 6 `Mock*Adapter` du gateway (auth, geo, opt, exe, not, trk), leurs
  tests, et les listes de tenants codées en dur côté Admin (`dossiers`,
  `rapport-financier-admin`). **BNFT** = Bureau National du Fret Terrestre,
  l'institution tchadienne, symétrique de BGFT côté Cameroun.
- Enrichissement des données mockées pour ce tenant (un second axe, une
  seconde mission, une seconde notification, une seconde position) afin
  qu'il soit démontrable au même niveau que BGFT, pas seulement une fixture
  de test.
- Ajout d'un bouton de connexion rapide « Bureau de fret (Tchad) » sur
  `/login`, réutilisant le compte existant (`+235600000004`).
- **Périmètre volontairement limité au rôle Bureau** : pas de compte
  Transporteur symétrique créé pour ce tenant — demande explicite de
  l'utilisateur, pour ne pas élargir la distinction de rôle au-delà de ce
  qui a été demandé. La mission `mission-c`/`mission-d` de `MockExeAdapter`
  continue de référencer un `transporteurId` (`actor-transporteur-tchad-1`
  puis `-2`) sans acteur authentifiable associé — incohérence préexistante,
  non corrigée ici car hors scope.

## Conséquences

- Aucun changement de comportement pour le tenant Douala ni pour l'écran
  "Gestion des tenants" (FE-ADM-04) : ces identifiants restent de pures
  conventions de chaînes partagées entre mocks et front, la table `Tenant`
  réelle (service-adm) demeure vide par défaut pour tous les tenants,
  BGFT comme BNFT — hors scope de ce ticket.
- Si un vrai `service-geo`/`service-opt`/etc. remplace ces mocks (cf. TODO
  dans chaque adaptateur), l'identifiant `tenant-bnft-ndjamena` devient la
  référence à reprendre côté données réelles.

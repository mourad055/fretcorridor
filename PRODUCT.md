# Product

## Register

product

## Users

- **Bureau de fret** (institution, tenant) : supervise les flux de son territoire, consulte l'observatoire, exporte des rapports. Contexte d'usage : poste de travail bureau, sessions longues, besoin de vue d'ensemble et de fiabilité des chiffres.
- **Transporteur** (personne morale ou physique) : consulte sa flotte, ses missions, son grand livre, configure ses connecteurs. Contexte d'usage : usage terrain/mobile possible, tâches ponctuelles et répétées (vérifier une mission, un paiement).
- **Administrateur** (interne Flysoft) : modère, arbitre les litiges, configure la plateforme, consulte le journal d'audit. Contexte d'usage : opérations transverses à tous les tenants, chaque action tracée.

Corridor CEMAC Cameroun–Tchad (Douala–N'Djamena). Utilisateurs B2B professionnels, pas grand public.

## Product Purpose

Portail web à trois rôles (une seule application Angular, feature modules par rôle) pour la mise en relation fiable et traçable entre bureaux de fret, transporteurs et administration, avec un service de paiement en séquestre logique (jamais dépositaire réel des fonds). Le succès se mesure à la confiance que les institutions et transporteurs accordent aux chiffres affichés et à la rapidité avec laquelle chaque rôle accomplit sa tâche (superviser, exécuter une mission, arbitrer un litige).

## Brand Personality

Moderne, rassurant, professionnel. Look contemporain (proche d'une fintech B2B) sans perdre le sérieux institutionnel attendu d'un outil utilisé par des bureaux de fret et une administration publique/parapublique CEMAC. La confiance se gagne par la clarté des chiffres et la cohérence visuelle, pas par l'expression graphique.

## Anti-references

Pas de référence négative précise identifiée. Éviter par principe : tout ce qui ferait ressembler l'outil à une landing page marketing (gros hero, dégradés décoratifs, ton "startup fun") — ce n'est pas le registre d'un outil institutionnel utilisé en production par des tiers de confiance.

## Design Principles

1. **Clarté avant décoration** — chaque écran sert une tâche précise (superviser, exécuter, arbitrer) ; aucune fioriture qui n'aide pas à lire un chiffre ou un statut plus vite.
2. **La confiance se voit dans les détails** — statuts et libellés toujours explicites (jamais de code brut à l'écran), traçabilité visible (qui a décidé quoi, quand) partout où c'est pertinent (Admin notamment).
3. **Aucune ambiguïté sur l'argent** — l'UI ne doit jamais suggérer que FretCorridor détient des fonds (ENF-FIN-01/02) ; vocabulaire et affichages du grand livre restent stricts sur ce point.
4. **Un rôle, un périmètre, une vue** — chaque rôle ne voit et n'agit que sur son périmètre de données (RBAC) ; l'UI reflète cette frontière sans jamais donner l'impression d'un accès plus large qu'autorisé.
5. **Sobriété qui n'exclut pas le soin** — look contemporain et professionnel, pas minimaliste par paresse : hiérarchie, espacement et cohérence méritent autant d'attention qu'un produit grand public.

## Accessibility & Inclusion

WCAG AA (ENF-A11Y-01 du CDC) : contraste ≥4.5:1 texte normal / ≥3:1 texte large, cibles tactiles ≥48pt, navigation clavier complète. Pas encore auditée systématiquement à ce stade (Sprint UI en cours) — c'est un axe prioritaire de la roadmap, pas un acquis.

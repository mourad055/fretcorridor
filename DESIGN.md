---
name: FretCorridor
description: Portail web à trois rôles (Bureau de fret, Transporteur, Administration) pour le corridor CEMAC Douala–N'Djamena
colors:
  corridor-red: "#d40f16"
  corridor-red-hover: "#b80d13"
  corridor-red-soft: "#fcecec"
  surface: "#ffffff"
  neutral-bg: "#f5f5f6"
  ink: "#0a0a0a"
  ink-muted: "#52525b"
  border: "#e4e4e7"
  border-strong: "#86868c"
  danger: "#b42318"
  danger-soft: "#fef3f2"
  success: "#067647"
  success-soft: "#ecfdf3"
  warning: "#b54708"
typography:
  title:
    fontFamily: "Montserrat, system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "clamp(1.125rem, 2.5vw, 1.375rem)"
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: "-0.02em"
  body:
    fontFamily: "Montserrat, system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "normal"
  label:
    fontFamily: "Montserrat, system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "0.75rem"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "0.02em"
rounded:
  sm: "8px"
  md: "10px"
  pill: "999px"
spacing:
  1: "4px"
  2: "8px"
  3: "12px"
  4: "16px"
  5: "20px"
  6: "24px"
  8: "32px"
components:
  button-primary:
    backgroundColor: "{colors.corridor-red}"
    textColor: "#ffffff"
    rounded: "{rounded.sm}"
    padding: "0.7rem 1rem"
  button-primary-hover:
    backgroundColor: "{colors.corridor-red-hover}"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.sm}"
    padding: "0.7rem 1rem"
  panel:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.md}"
    padding: "{spacing.4}"
  field-input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.sm}"
    padding: "0.75rem 0.85rem"
  badge:
    rounded: "{rounded.pill}"
    padding: "0.15rem 0.5rem"
---

# Design System: FretCorridor

## 1. Overview

**Creative North Star: "Le Poste de Contrôle"**

FretCorridor est le poste de contrôle du corridor de fret CEMAC Douala–N'Djamena : un opérateur y supervise des flux, pas un visiteur qu'on cherche à séduire. Chaque écran répond à une seule question opérationnelle — où en est ce dossier, ce paiement, cette mission — et le système visuel s'efface derrière cette réponse plutôt que de la décorer. Le rouge de marque (Rouge Corridor) fonctionne comme une couleur de signalisation officielle : rare, réservée aux actions primaires et aux accents d'état actif, jamais étalée en fond ou en dégradé.

La personnalité voulue — moderne, rassurante, professionnelle — se traduit par des composants **feutrés et rassurants** : transitions courtes mais jamais sèches (160ms, courbe `cubic-bezier(0.16, 1, 0.3, 1)`), coins arrondis généreux et constants (8–10px), ombres quasi imperceptibles réservées aux surfaces de contenu. Le système rejette explicitement tout ce qui ressemblerait à une landing page marketing : pas de hero, pas de dégradé décoratif, pas de ton "startup fun" — ce n'est pas le registre d'un outil institutionnel utilisé en production par des bureaux de fret et une administration CEMAC.

**Key Characteristics:**
- Rouge Corridor réservé aux actions et états actifs, jamais en fond de page ou en dégradé.
- Densité assumée : tableaux et listes priment sur les cartes, l'espace n'est pas gonflé artificiellement.
- Plat par défaut : l'élévation (ombre légère) signale exclusivement qu'une zone est une surface de contenu distincte, jamais un effet décoratif.
- Statuts toujours doublés d'un libellé texte lisible, jamais d'une couleur seule (badges `app-status-badge`).

## 2. Colors

Palette restreinte à un seul accent institutionnel sur fond neutre froid — pas de palette étendue, la confiance vient de la constance, pas de la variété.

### Primary
- **Rouge Corridor** (#d40f16) : actions primaires (`fc-btn--primary`), liens actifs de navigation, focus ring, bordure de sélection. Utilisé sur une fraction minime de chaque écran — c'est un signal, pas une ambiance.
- **Rouge Corridor Hover** (#b80d13) : état hover des actions primaires uniquement.
- **Rouge Corridor Doux** (#fcecec, `color-mix(in srgb, Rouge Corridor 8%, blanc)`) : fond des éléments sélectionnés/actifs (nav active, ligne de liste active), jamais utilisé pour du texte.

### Neutral
- **Surface** (#ffffff) : fond de tous les panels, tableaux, champs — la surface de travail.
- **Fond Neutre** (#f5f5f6) : fond de page derrière les panels, neutre froid volontairement choisi (pas de crème/beige — voir Do's and Don'ts).
- **Encre** (#0a0a0a) : texte principal, quasi-noir plutôt que noir pur.
- **Encre Atténuée** (#52525b) : texte secondaire, libellés de colonnes, méta-information.
- **Bordure** (#e4e4e7) : séparateurs, contours de panels/tableaux (surfaces non interactives).
- **Bordure Renforcée** (#86868c) : contour par défaut de tout composant interactif (champs, select, boutons ghost, éléments de liste cliquables) — seule valeur du système qui atteint 3:1 contre blanc, requis par la WCAG 1.4.11 pour les limites de composants interactifs.

### Named Rules
**La Règle du Signal Rare.** Le Rouge Corridor n'apparaît que sur une action primaire, un état actif ou un focus — jamais en fond de section, jamais en dégradé. S'il devient décoratif, il perd sa fonction de signal.

**La Règle de la Couleur Non Seule.** Aucune information d'état (statut de mission, de dossier, de paiement) ne repose sur la couleur seule : chaque badge de statut porte toujours un libellé texte explicite à côté de sa couleur.

**La Règle des Deux Bordures.** Bordure sert les surfaces (panels, tableaux, séparateurs) ; Bordure Renforcée sert exclusivement les composants interactifs (tout élément cliquable ou éditable). Ne jamais utiliser Bordure sur un champ, un bouton ou une ligne cliquable — sous 3:1 de contraste, sa limite devient invisible pour un utilisateur malvoyant (Sprint 17, corrigé après audit).

## 3. Typography

**Body & UI Font:** Montserrat (avec repli `system-ui, -apple-system, 'Segoe UI', sans-serif`)

**Character:** Une seule famille, plusieurs graisses — pas de duo display/body. Montserrat en 700 pour les titres et libellés donne la fermeté institutionnelle, en 400 pour le corps donne la lisibilité longue durée dont un opérateur en session de plusieurs heures a besoin.

### Hierarchy
- **Title** (700, `clamp(1.125rem, 2.5vw, 1.375rem)`, line-height 1.25, letter-spacing -0.02em) : titres de page et de panel (`fc-page__title`, h1–h3). Pas d'échelle "display" plus grande — ce produit ne fait jamais de grand geste typographique, la hiérarchie se joue sur des écarts modestes.
- **Body** (400, 0.9375rem/15px, line-height 1.5) : texte courant, cellules de tableau, contenu de formulaire.
- **Label** (700, 0.75rem, letter-spacing 0.02em, souvent uppercase) : en-têtes de tableau, libellés de filtre, titres de panel (`fc-panel__title`), badges de statut.

### Named Rules
**La Règle de l'Écart Modeste.** Le titre de page le plus grand du système (1.375rem) ne dépasse jamais 1.5× la taille du corps de texte. Aucun écran ne doit "crier" — la hiérarchie vient du poids et de la couleur atténuée, pas de sauts de taille spectaculaires.

## 4. Elevation

Plat par défaut. L'ombre n'est jamais décorative ; elle sert un seul rôle, détacher une surface de contenu (panel, tableau) du fond de page neutre derrière elle. Aucune ombre sur les boutons, badges, liens de navigation ou éléments de liste au repos.

### Shadow Vocabulary
- **Élévation Panel** (`box-shadow: 0 1px 2px color-mix(in srgb, #0a0a0a 6%, transparent)`) : ombre quasi imperceptible sur `fc-panel` et `fc-table`, signale "ceci est une surface de contenu".
- **Élévation Flottante** (`box-shadow: 0 8px 24px color-mix(in srgb, #0a0a0a 8%, transparent)`) : réservée aux éléments qui se détachent physiquement du flux (menus flottants, futurs popovers) — pas encore utilisée par un composant existant au moment de la rédaction.

### Named Rules
**La Règle du Plat par Défaut.** Toute surface est plate au repos. L'élévation n'apparaît que pour distinguer une zone de contenu du fond — jamais en réponse à un état d'interaction (le hover change une couleur de fond ou de bordure, jamais une ombre).

## 5. Components

### Buttons
- **Shape:** coins à 8px (`--fc-radius-sm`), jamais pilule sauf les badges.
- **Primary:** fond Rouge Corridor, texte blanc, padding `0.7rem 1rem`, poids 700.
- **Hover / Focus:** primary passe à Rouge Corridor Hover (#b80d13) ; focus visible = anneau 2px Rouge Corridor avec 2px d'offset sur tout élément interactif, jamais supprimé.
- **Ghost:** fond transparent, bordure neutre, texte encre ; au survol la bordure et le texte basculent vers le Rouge Corridor et le fond prend le Rouge Corridor Doux.
- **Disabled:** opacité 0.65, curseur `wait` (l'action est en cours, pas simplement indisponible).

### Badges (statut)
- **Style:** forme pilule, texte 0.6875rem tout-capitales, poids 700 — composant `app-status-badge` partagé.
- **Variantes:** success (vert #067647 sur fond `success-soft`), warning (ambre #b54708), danger (rouge #b42318 sur `danger-soft`), primary (Rouge Corridor sur Rouge Corridor Doux), neutral (encre atténuée, bordure visible).
- **Règle:** le libellé passé au badge est toujours un texte traduit lisible (ex. "Confirmée"), jamais le code brut de l'enum backend (ex. jamais "CONFIRMEE").

### Cards / Panels
- **Corner Style:** 10px (`--fc-radius`).
- **Background:** Surface blanche sur fond page neutre.
- **Shadow Strategy:** Élévation Panel uniquement (voir §4).
- **Border:** 1px Bordure au repos.
- **Internal Padding:** 16px desktop, 12px sous 720px.

### Inputs / Fields
- **Style:** fond Surface, bordure 1px Bordure, coins 8px, label toujours visible au-dessus (jamais placeholder-seul).
- **Focus:** bordure Rouge Corridor + halo `0 0 0 3px` Rouge Corridor à 18% d'opacité — pas de changement de fond.
- **Error:** message d'erreur en dessous du champ, couleur danger, poids 700.

### Navigation
- **Style:** onglets pilule horizontaux (`shell-nav__link`), texte 0.8125rem poids 600, couleur encre atténuée au repos.
- **Active:** couleur Rouge Corridor + fond Rouge Corridor Doux — même paire couleur/fond que les badges primary, pour une cohérence "ce qui est actif est rouge doux" dans tout le système.
- **Header:** sticky, fond Surface à 92% d'opacité + `backdrop-filter: blur(10px)` — seul usage de flou du système, justifié par le maintien de lisibilité du contenu qui défile en dessous.
- **Mobile:** onglets en défilement horizontal sous 720px plutôt qu'empilement, pour garder la navigation accessible d'un geste.

## 6. Do's and Don'ts

### Do:
- **Do** garder le Rouge Corridor (#d40f16) réservé aux actions primaires, états actifs et focus — jamais en fond de section.
- **Do** traduire chaque statut backend en libellé français lisible avant affichage (badge ou texte simple), avec repli sur la valeur brute uniquement si le libellé n'existe pas encore.
- **Do** utiliser l'ombre panel (`fc-shadow-sm`) exclusivement pour distinguer une surface de contenu du fond, jamais comme décoration ou réponse à un hover.
- **Do** respecter le rythme d'espacement 4/8/12/16/20/24/32px (`--fc-space-*`) partout — pas de valeurs arbitraires.
- **Do** garder un point de focus visible (anneau 2px Rouge Corridor) sur tout élément interactif.

### Don't:
- **Don't** utiliser de fond crème/beige/parchemin — le fond neutre est délibérément froid (#f5f5f6), jamais chaud "pour l'élégance".
- **Don't** construire un hero, un dégradé décoratif ou un ton "startup fun" — ce n'est pas une landing page, c'est un outil institutionnel CEMAC.
- **Don't** afficher un code d'enum brut (ex. "PORTE_A_PORTE", "TRANSPORTEUR_PERSONNE_MORALE") directement dans un template — toujours passer par une fonction de libellé.
- **Don't** faire reposer une information d'état sur la couleur seule — toujours un libellé texte à côté.
- **Don't** utiliser de bordure colorée en `border-left`/`border-right` comme accent décoratif sur une carte ou une ligne de liste.
- **Don't** ajouter d'ombre portée sur les boutons, badges ou liens de navigation au repos — le plat par défaut est la règle, pas l'exception.

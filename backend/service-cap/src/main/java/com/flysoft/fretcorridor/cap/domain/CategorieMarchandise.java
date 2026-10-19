package com.flysoft.fretcorridor.cap.domain;

/**
 * Categories retenues pour la tarification par chauffeur
 * (spec-tarification-par-chauffeur.md §1) : un tarif a plat en F/kg par
 * axe et par categorie, jamais un continuum F/kg/km.
 */
public enum CategorieMarchandise {
    STANDARD, FRAGILE, ULTRA_FRAGILE
}

package com.fretcorridor.opt.domain;

/**
 * Cycle de vie d'une Affectation (diffusion-course, plan de reorientation) :
 * PROPOSEE tant qu'aucun chauffeur n'a accepte, puis une seule transition vers
 * CONFIRMEE (le premier arrive) ou EXPIREE (perdant de la course sur la meme
 * demande, ou refus explicite). CONFIRMEE et EXPIREE sont terminales.
 *
 * Extraite dans son propre fichier pour etre publique : consommee hors du
 * package domain (web.dto, web) pour le GET "/proposees" d'un transporteur.
 */
public enum StatutAffectation {
    PROPOSEE, CONFIRMEE, EXPIREE
}

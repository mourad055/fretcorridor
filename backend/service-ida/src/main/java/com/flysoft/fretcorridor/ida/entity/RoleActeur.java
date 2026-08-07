package com.flysoft.fretcorridor.ida.entity;

// Rôles couverts par l'ensemble du système (mobile + web) — cf. Feuille de route v4 §5.1
// service-ida est la source d'identité unique pour tous les acteurs, quel que soit
// le client (app mobile, portail web) qui les consomme ensuite.
public enum RoleActeur {
    CHAUFFEUR,
    TRANSPORTEUR,
    CHAUFFEUR_PROPRIETAIRE, // cumul chauffeur + transporteur
    AGENT,                  // enrôlement assisté terrain, mode intégré à l'app Chauffeur/Transporteur
    CHARGEUR,
    BUREAU,                 // Bureau de fret — portail web
    ADMINISTRATION           // back-office Flysoft — portail web
}

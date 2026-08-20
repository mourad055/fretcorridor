package com.fretcorridor.gateway.domain.cap;

/** RG-028 (CDC) : une capacité publiée expire à l'heure de départ si non appariée. */
public enum CapaciteEtat {
    PUBLIEE,
    APPARIEE,
    EXPIREE
}

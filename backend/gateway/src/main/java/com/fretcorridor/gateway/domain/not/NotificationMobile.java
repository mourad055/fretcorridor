package com.fretcorridor.gateway.domain.not;

/** S9 : notification réelle de l'acteur mobile (pas la vue Bureau, mockée séparément). */
public record NotificationMobile(String id, String titre, String corps, String type, String referenceId,
                                  boolean lue, String dateCreation) {
}

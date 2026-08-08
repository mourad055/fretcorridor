package com.fretcorridor.gateway.infrastructure.rest.opt.dto;

import com.fretcorridor.gateway.domain.opt.MissionAppariee;

import java.util.List;

/** EF-BUR-02 : export des flux supervisés — CSV simple, sans dépendance externe. */
public final class MissionAppparieeCsvExporter {

    private static final String EN_TETE = "id,axeId,transporteurNom,origine,destination,enlevementLe,statut";

    private MissionAppparieeCsvExporter() {
    }

    public static String versCsv(List<MissionAppariee> missions) {
        StringBuilder csv = new StringBuilder(EN_TETE).append('\n');
        for (MissionAppariee mission : missions) {
            csv.append(champ(mission.id())).append(',')
                    .append(champ(mission.axeId())).append(',')
                    .append(champ(mission.transporteurNom())).append(',')
                    .append(champ(mission.origine())).append(',')
                    .append(champ(mission.destination())).append(',')
                    .append(champ(mission.enlevementLe().toString())).append(',')
                    .append(champ(mission.statut().name()))
                    .append('\n');
        }
        return csv.toString();
    }

    private static String champ(String valeur) {
        if (valeur == null) {
            return "";
        }
        boolean doitEtreEchappe = valeur.contains(",") || valeur.contains("\"") || valeur.contains("\n");
        String echappe = valeur.replace("\"", "\"\"");
        return doitEtreEchappe ? "\"" + echappe + "\"" : echappe;
    }
}

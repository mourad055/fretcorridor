package com.fretcorridor.trk.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {

    @Query("SELECT p FROM Position p WHERE p.missionId = :missionId ORDER BY p.horodatageCapture ASC")
    List<Position> findByMissionIdOrderByHorodatageCaptureAsc(@Param("missionId") UUID missionId);

    /**
     * Derniere position connue pour CHAQUE vehicule d'une liste, en une seule
     * requete SQL (fenetrage ROW_NUMBER partitionne par vehicule) - remplace
     * N appels findFirstByVehiculeIdOrderByHorodatageCaptureDesc() sequentiels
     * (plan de reorientation, position GPS temps reel dans le matching :
     * appeler ca en boucle depuis OPT violerait le budget L0 ~50ms des que le
     * nombre de vehicules candidats grandit).
     *
     * native = true : ROW_NUMBER() OVER (PARTITION BY ...) n'a pas
     * d'equivalent direct simple en JPQL portable. Un vehicule absent de la
     * liste retournee = aucune position jamais recue pour lui - meme
     * semantique que le 404 de l'endpoint unitaire, a gerer cote appelant.
     */
    @Query(value = """
            SELECT * FROM (
                SELECT p.*, ROW_NUMBER() OVER (
                    PARTITION BY p.vehicule_id ORDER BY p.horodatage_capture DESC
                ) AS rn
                FROM trk.position p
                WHERE p.vehicule_id IN (:vehiculeIds)
            ) ranked
            WHERE ranked.rn = 1
            """, nativeQuery = true)
    List<Position> findDernieresPositionsPourVehicules(@Param("vehiculeIds") List<UUID> vehiculeIds);

    /**
     * Derniere position connue d'un vehicule, TOUTES missions confondues.
     *
     * Necessaire pour le matching en position temps reel (plan de
     * reorientation post-demo, "Gestion des axes avec les positions gps...
     * prendre en compte la position gps du chauffeur") : au moment du
     * matching, aucune mission n'existe encore pour ce vehicule - la
     * recherche ne peut donc jamais passer par missionId (cf
     * findByMissionIdOrderByHorodatageCaptureAsc ci-dessus, inutilisable ici).
     */
    Optional<Position> findFirstByVehiculeIdOrderByHorodatageCaptureDesc(UUID vehiculeId);

    // Point 6 (suivi "colis recupere = position chauffeur") : derniere
    // capture GPS d'une mission pour afficher la position du chauffeur des
    // que le colis est a bord. findFirstByMissionIdOrderByHorodatageCaptureDesc
    // est derivee du nom (pas de JPQL) : retourne la position la plus recente
    // pour la mission donnee, ou vide si aucune capture n'est jamais arrivee.
    Optional<Position> findFirstByMissionIdOrderByHorodatageCaptureDesc(UUID missionId);
}

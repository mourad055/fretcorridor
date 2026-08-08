package com.fretcorridor.opt.tarification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolution du barème en cascade (CDC S8.9.1 + EF-GEO-02) : le plus
 * specifique gagne, repli progressif vers le plus generique. Ordre de
 * priorite implemente dans TarificationL4Service.resoudreBareme :
 *   1. axe specifique + type de vehicule specifique
 *   2. axe specifique + type de vehicule generique (NULL)
 *   3. axe par defaut (NULL) + type de vehicule specifique
 *   4. axe par defaut (NULL) + type de vehicule generique (NULL)
 */
public interface BaremeTarificationRepository extends JpaRepository<BaremeTarification, UUID> {

    Optional<BaremeTarification> findFirstByAxeIdAndTypeVehiculeAndActifTrue(UUID axeId, String typeVehicule);

    Optional<BaremeTarification> findFirstByAxeIdAndTypeVehiculeIsNullAndActifTrue(UUID axeId);

    Optional<BaremeTarification> findFirstByAxeIdIsNullAndTypeVehiculeAndActifTrue(String typeVehicule);

    Optional<BaremeTarification> findFirstByAxeIdIsNullAndTypeVehiculeIsNullAndActifTrue();
}

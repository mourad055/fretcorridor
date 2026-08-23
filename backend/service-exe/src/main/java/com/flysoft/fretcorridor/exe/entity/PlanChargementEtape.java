package com.flysoft.fretcorridor.exe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

// S16/EF-MAT-13 (audit de suivi, 23 août) : charge par essieu (approximation
// uniforme, cf javadoc service-opt/OracleChargementService) à un état
// intermédiaire d'une Tournée, tel que publié par PlanChargementConfirmeEvent
// (service-opt) - jusqu'ici publié mais jamais consommé.
//
// Corrélation avec EtapeTournee (cette même classe, service-exe) par
// (tourneeId, rang) UNIQUEMENT : EtatChargementDto.etapeTourneeId référence
// l'id interne de l'EtapeTournee côté service-opt (package
// com.fretcorridor.opt.sequencement), une entité et un id DIFFÉRENTS de
// EtapeTournee côté service-exe (package com.flysoft.fretcorridor.exe.entity,
// alimentée séparément par TourneeConstitueeEvent) - rang est la seule clé
// partagée entre les deux modèles pour une même Tournée.
@Entity
@Table(name = "plans_chargement_etape", uniqueConstraints = @UniqueConstraint(columnNames = {"tournee_id", "rang"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanChargementEtape {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tournee_id", nullable = false)
    private UUID tourneeId;

    @Column(nullable = false)
    private int rang;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "charges_par_essieu", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> chargesParEssieu;

    @Column(name = "date_generation", nullable = false)
    private LocalDateTime dateGeneration;
}

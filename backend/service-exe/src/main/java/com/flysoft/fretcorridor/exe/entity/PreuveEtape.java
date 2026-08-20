package com.flysoft.fretcorridor.exe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RG-070/EF-EXE-03 (CDC, UC-EXE-03) : preuve minimale d'un enlèvement ou
 * d'une livraison — une ou plusieurs photographies, plus une validation par
 * un tiers. Cette version couvre la signature tactile ; le code à usage
 * unique reçu par SMS (autre mode prévu par le CDC) est différé, le numéro
 * du destinataire n'étant pas propagé jusqu'à service-exe aujourd'hui (cf.
 * claude.md).
 *
 * RG-072/EF-EXE-05 : immuable par construction -- PreuveEtapeRepository
 * n'expose aucune méthode de mise à jour ou de suppression, même principe
 * que JournalAuditRepositoryAdapter (service-adm).
 */
@Entity
@Table(name = "preuves_etape")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreuveEtape {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "etape_mission_id", nullable = false, unique = true)
    private EtapeMission etapeMission;

    @ElementCollection
    @CollectionTable(name = "preuves_etape_photos", joinColumns = @JoinColumn(name = "preuve_etape_id"))
    private List<PhotoPreuve> photos;

    @Column(nullable = false)
    private String signatureObjectKey;

    @Column(nullable = false)
    private String signatureEmpreinteSha256;

    @Builder.Default
    private LocalDateTime dateDepot = LocalDateTime.now();
}

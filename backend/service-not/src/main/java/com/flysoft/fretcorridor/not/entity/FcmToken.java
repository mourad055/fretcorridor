package com.flysoft.fretcorridor.not.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// Préparé pour le vrai push Firebase — enregistré dès maintenant côté mobile
// pour éviter un aller-retour plus tard, mais jamais utilisé pour envoyer
// quoi que ce soit tant que google-services.json n'est pas disponible.
@Entity
@Table(name = "fcm_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID acteurId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private LocalDateTime dateEnregistrement = LocalDateTime.now();
}

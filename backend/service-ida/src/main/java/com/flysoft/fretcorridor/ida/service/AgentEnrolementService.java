package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.EnrolementDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.EnrolementAgent;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.repository.EnrolementAgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * UC-IDA-03 / EF-IDA-06 : enrôlement assisté par agent de terrain.
 * RG-019 : le compte appartient à la personne enrôlée — l'OTP part par SMS
 * vers son téléphone, jamais exposé à l'agent ; l'activation ne renvoie
 * jamais les tokens du compte créé à la session de l'agent.
 */
@Service
@RequiredArgsConstructor
public class AgentEnrolementService {

    private static final Set<RoleActeur> ROLES_ENROLABLES =
            Set.of(RoleActeur.CHAUFFEUR, RoleActeur.TRANSPORTEUR, RoleActeur.CHAUFFEUR_PROPRIETAIRE);
    private static final int OTP_VALIDITE_MINUTES = 10;
    private static final int MAX_TENTATIVES_OTP = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ActeurRepository acteurRepository;
    private final EnrolementAgentRepository enrolementAgentRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsPort smsPort;

    @Transactional
    public EnrolementDto.EnrolementResponse initier(UUID agentUserId, EnrolementDto.InitierRequest request) {
        Acteur agent = acteurRepository.findById(agentUserId)
                .orElseThrow(() -> new RuntimeException("AGENT_INTROUVABLE"));
        if (!agent.getRoles().contains(RoleActeur.AGENT)) {
            throw new RuntimeException("ACCES_REFUSE");
        }

        // Idempotence : une même requête rejouée après coupure réseau (file
        // offline de l'agent) ne doit jamais créer un second enrôlement.
        var existant = enrolementAgentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existant.isPresent()) {
            return EnrolementDto.EnrolementResponse.fromEntity(existant.get());
        }

        RoleActeur typeActeur;
        try {
            typeActeur = RoleActeur.valueOf(request.getTypeActeur());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("TYPE_ACTEUR_INVALIDE");
        }
        if (!ROLES_ENROLABLES.contains(typeActeur)) {
            throw new RuntimeException("TYPE_ACTEUR_INVALIDE");
        }

        if (acteurRepository.existsByTelephone(request.getTelephone())) {
            throw new RuntimeException("TELEPHONE_DEJA_UTILISE");
        }

        String otp = genererOtp();
        EnrolementAgent enrolement = EnrolementAgent.builder()
                .agent(agent)
                .telephoneEnrole(request.getTelephone())
                .typeActeur(typeActeur)
                .codeOtpHash(passwordEncoder.encode(otp))
                .otpExpiration(LocalDateTime.now().plusMinutes(OTP_VALIDITE_MINUTES))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .idempotencyKey(request.getIdempotencyKey())
                .tenantId(agent.getTenantId())
                .build();
        enrolement = enrolementAgentRepository.save(enrolement);

        smsPort.envoyer(request.getTelephone(),
                "FretCorridor : votre code d'activation est " + otp + " (valable " + OTP_VALIDITE_MINUTES + " min).");

        return EnrolementDto.EnrolementResponse.fromEntity(enrolement);
    }

    @Transactional
    public EnrolementDto.EnrolementResponse activer(UUID enrolementId, EnrolementDto.ActiverRequest request) {
        EnrolementAgent enrolement = enrolementAgentRepository.findById(enrolementId)
                .orElseThrow(() -> new RuntimeException("ENROLEMENT_INTROUVABLE"));

        if (enrolement.getStatut() == EnrolementAgent.StatutEnrolement.ACTIVE) {
            // Rejeu idempotent après activation déjà réussie (retry offline).
            return EnrolementDto.EnrolementResponse.fromEntity(enrolement);
        }
        if (enrolement.getOtpExpiration().isBefore(LocalDateTime.now())) {
            enrolement.setStatut(EnrolementAgent.StatutEnrolement.EXPIRE);
            enrolementAgentRepository.save(enrolement);
            throw new RuntimeException("OTP_EXPIRE");
        }
        if (!passwordEncoder.matches(request.getOtp(), enrolement.getCodeOtpHash())) {
            enrolement.setTentativesOtpEchouees(enrolement.getTentativesOtpEchouees() + 1);
            if (enrolement.getTentativesOtpEchouees() >= MAX_TENTATIVES_OTP) {
                enrolement.setStatut(EnrolementAgent.StatutEnrolement.EXPIRE);
            }
            enrolementAgentRepository.save(enrolement);
            throw new RuntimeException("OTP_INCORRECT");
        }

        Acteur acteur = Acteur.builder()
                .telephone(enrolement.getTelephoneEnrole())
                .codePin(passwordEncoder.encode(request.getCodePin()))
                .roles(Set.of(enrolement.getTypeActeur()))
                .tenantId(enrolement.getTenantId())
                .build();
        acteur = acteurRepository.save(acteur);

        enrolement.setStatut(EnrolementAgent.StatutEnrolement.ACTIVE);
        enrolement.setActeurCreeId(acteur.getId());
        enrolement = enrolementAgentRepository.save(enrolement);

        // Volontairement pas d'accessToken/refreshToken ici (RG-019) : la
        // personne se connecte plus tard, depuis son propre appareil, avec
        // le téléphone et le PIN qu'elle vient de saisir.
        return EnrolementDto.EnrolementResponse.fromEntity(enrolement);
    }

    private String genererOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}

package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.AuthDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TENANT_MARKETPLACE_PUBLIC = "MARKETPLACE_CM";
    private static final int MAX_TENTATIVES = 3;

    private final ActeurRepository acteurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        Acteur acteur = acteurRepository.findByTelephone(request.getTelephone())
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));

        if (!acteur.getActif()) {
            throw new RuntimeException("COMPTE_BLOQUE");
        }

        if (!passwordEncoder.matches(request.getCodePin(), acteur.getCodePin())) {
            acteur.setTentativesEchouees(acteur.getTentativesEchouees() + 1);
            if (acteur.getTentativesEchouees() >= MAX_TENTATIVES) {
                acteur.setActif(false);
            }
            acteurRepository.save(acteur);
            throw new RuntimeException("PIN_INCORRECT:" + (MAX_TENTATIVES - acteur.getTentativesEchouees()));
        }

        acteur.setTentativesEchouees(0);
        acteurRepository.save(acteur);

        String access = jwtService.genererAccessToken(acteur);
        String refresh = jwtService.genererRefreshToken(acteur);
        return AuthDto.AuthResponse.of(access, refresh, acteur);
    }

    // EF-MKT-01 : le chargeur s'inscrit en < 90s, sans KYC bloquant (S1 — inscription légère)
    @Transactional
    public AuthDto.AuthResponse inscrireChargeur(AuthDto.InscriptionChargeurRequest request) {
        if (acteurRepository.existsByTelephone(request.getTelephone())) {
            throw new RuntimeException("TELEPHONE_DEJA_UTILISE");
        }

        Acteur acteur = Acteur.builder()
                .telephone(request.getTelephone())
                .codePin(passwordEncoder.encode(request.getCodePin()))
                .roles(Set.of(RoleActeur.CHARGEUR))
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .raisonSociale(normaliser(request.getRaisonSociale()))
                .tenantId(TENANT_MARKETPLACE_PUBLIC)
                .build();

        acteur = acteurRepository.save(acteur);

        String access = jwtService.genererAccessToken(acteur);
        String refresh = jwtService.genererRefreshToken(acteur);
        return AuthDto.AuthResponse.of(access, refresh, acteur);
    }

    // Meme principe que inscrireChargeur (S1, sans KYC bloquant) - le tenant
    // fixe correspond au meme mono-tenant Phase 1 (ADR 0011) utilise par
    // service-exe/service-not/service-mkt pour ce corridor.
    private static final String TENANT_BGFT_PHASE1 = "tenant-bgft-douala";

    @Transactional
    public AuthDto.AuthResponse inscrireTransporteur(AuthDto.InscriptionTransporteurRequest request) {
        if (acteurRepository.existsByTelephone(request.getTelephone())) {
            throw new RuntimeException("TELEPHONE_DEJA_UTILISE");
        }

        RoleActeur role;
        try {
            role = RoleActeur.valueOf(request.getType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("TYPE_INVALIDE");
        }
        if (role != RoleActeur.CHAUFFEUR && role != RoleActeur.TRANSPORTEUR
                && role != RoleActeur.CHAUFFEUR_PROPRIETAIRE) {
            throw new RuntimeException("TYPE_INVALIDE");
        }

        Acteur acteur = Acteur.builder()
                .telephone(request.getTelephone())
                .codePin(passwordEncoder.encode(request.getCodePin()))
                .roles(Set.of(role))
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .raisonSociale(normaliser(request.getRaisonSociale()))
                .tenantId(TENANT_BGFT_PHASE1)
                .build();

        acteur = acteurRepository.save(acteur);

        String access = jwtService.genererAccessToken(acteur);
        String refresh = jwtService.genererRefreshToken(acteur);
        return AuthDto.AuthResponse.of(access, refresh, acteur);
    }

    // Le gateway convertit systematiquement une raisonSociale absente en ""
    // avant de relayer l'inscription (ServiceIdaAuthenticationAdapter.register,
    // Map.of n'accepte pas de valeur null) - sans cette normalisation, toute
    // inscription Chauffeur (sans raison sociale) etait persistee avec ""
    // plutot que null, et "" != null faisait passer le compte pour une
    // Entreprise partout ou le code teste juste != null (KycDto, evaluation
    // du niveau KYC).
    private String normaliser(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur;
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse rafraichir(String refreshToken) {
        if (refreshToken == null) throw new RuntimeException("REFRESH_TOKEN_MANQUANT");
        var acteurId = jwtService.extraireActeurId(refreshToken);
        Acteur acteur = acteurRepository.findById(acteurId)
                .orElseThrow(() -> new RuntimeException("ACTEUR_INTROUVABLE"));

        // BUG CORRIGE (audit de suivi du 20 aout, perimetre Mobile) :
        // rafraichir() ne verifiait jamais acteur.getActif(), contrairement a
        // login() ci-dessus -- un compte verrouille apres MAX_TENTATIVES
        // echecs de PIN pouvait continuer a rafraichir indefiniment tant
        // qu'il detenait un refresh token emis AVANT le blocage. Meme garde
        // que login().
        if (!acteur.getActif()) {
            throw new RuntimeException("COMPTE_BLOQUE");
        }

        String access = jwtService.genererAccessToken(acteur);
        String refresh = jwtService.genererRefreshToken(acteur);
        return AuthDto.AuthResponse.of(access, refresh, acteur);
    }
}

package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.KycAdminDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.PieceJustificative;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.repository.PieceJustificativeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Revue KYC admin : filtrage des dossiers en attente et décisions VALIDE/REJETE.
 */
class KycAdminServiceTest {

    @Mock private ActeurRepository acteurRepository;
    @Mock private PieceJustificativeRepository pieceJustificativeRepository;
    @Mock private DocumentStorageService documentStorageService;

    private KycAdminService service;
    private UUID acteurId;
    private static final String TENANT = "tenant-bgft-douala";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new KycAdminService(acteurRepository, pieceJustificativeRepository, documentStorageService);
        acteurId = UUID.randomUUID();
        when(acteurRepository.save(any(Acteur.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Acteur chauffeurNiveau1() {
        return Acteur.builder()
                .id(acteurId)
                .telephone("+237690000001")
                .tenantId(TENANT)
                .roles(Set.of(RoleActeur.CHAUFFEUR))
                .nom("Ngono")
                .prenom("Awa")
                .niveauKyc(Acteur.NiveauKyc.NIVEAU_1)
                .build();
    }

    @Test
    void liste_pending_uniquement_les_acteurs_kyc_avec_pieces_et_sans_niveau_2() {
        Acteur enAttente = chauffeurNiveau1();
        Acteur dejaValide = Acteur.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .roles(Set.of(RoleActeur.TRANSPORTEUR))
                .niveauKyc(Acteur.NiveauKyc.NIVEAU_2)
                .build();
        Acteur sansPiece = Acteur.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .roles(Set.of(RoleActeur.CHAUFFEUR))
                .niveauKyc(Acteur.NiveauKyc.NIVEAU_1)
                .build();
        Acteur bureau = Acteur.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .roles(Set.of(RoleActeur.BUREAU))
                .niveauKyc(Acteur.NiveauKyc.NIVEAU_1)
                .build();

        when(acteurRepository.findByTenantId(TENANT))
                .thenReturn(List.of(enAttente, dejaValide, sansPiece, bureau));
        when(pieceJustificativeRepository.findByActeurId(enAttente.getId()))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k1").build()));
        when(pieceJustificativeRepository.findByActeurId(dejaValide.getId()))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k2").build()));
        when(pieceJustificativeRepository.findByActeurId(sansPiece.getId())).thenReturn(List.of());
        when(pieceJustificativeRepository.findByActeurId(bureau.getId()))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k3").build()));

        List<KycAdminDto.ActeurSummary> pending = service.listerEnAttente(TENANT);

        assertThat(pending).extracting(KycAdminDto.ActeurSummary::acteurId).containsExactly(acteurId);
    }

    @Test
    void liste_n1_exclut_les_dossiers_en_attente_admin() {
        UUID enAttenteId = UUID.randomUUID();
        UUID profilSansPieceId = UUID.randomUUID();
        Acteur enAttente = Acteur.builder()
                .id(enAttenteId)
                .tenantId(TENANT)
                .roles(Set.of(RoleActeur.CHAUFFEUR))
                .nom("Ngono")
                .prenom("Awa")
                .niveauKyc(Acteur.NiveauKyc.NIVEAU_1)
                .build();
        Acteur profilSansPiece = Acteur.builder()
                .id(profilSansPieceId)
                .tenantId(TENANT)
                .roles(Set.of(RoleActeur.TRANSPORTEUR))
                .nom("Mbarga")
                .prenom("Paul")
                .niveauKyc(Acteur.NiveauKyc.NIVEAU_1)
                .build();

        when(acteurRepository.findByTenantId(TENANT)).thenReturn(List.of(enAttente, profilSansPiece));
        when(acteurRepository.findByTenantIdAndNiveauKyc(TENANT, Acteur.NiveauKyc.NIVEAU_1))
                .thenReturn(List.of(enAttente, profilSansPiece));
        when(pieceJustificativeRepository.findByActeurId(enAttenteId))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k1").build()));
        when(pieceJustificativeRepository.findByActeurId(profilSansPieceId)).thenReturn(List.of());

        assertThat(service.listerEnAttente(TENANT)).extracting(KycAdminDto.ActeurSummary::acteurId)
                .containsExactly(enAttenteId);
        assertThat(service.listerParNiveau(TENANT, Acteur.NiveauKyc.NIVEAU_1))
                .extracting(KycAdminDto.ActeurSummary::acteurId)
                .containsExactly(profilSansPieceId);
    }

    @Test
    void valide_passe_au_niveau_2() {
        Acteur acteur = chauffeurNiveau1();
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));
        when(pieceJustificativeRepository.findByActeurId(acteurId))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k1").build()));

        var request = new KycAdminDto.DecisionRequest();
        request.setDecision("VALIDE");

        KycAdminDto.ActeurSummary resultat = service.prendreDecision(acteurId, TENANT, request);

        assertThat(resultat.niveauKyc()).isEqualTo("NIVEAU_2");
        verify(acteurRepository).save(any(Acteur.class));
    }

    @Test
    void rejet_avec_pieces_retourne_au_niveau_1() {
        Acteur acteur = chauffeurNiveau1();
        acteur.setNiveauKyc(Acteur.NiveauKyc.NIVEAU_2);
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));
        when(pieceJustificativeRepository.findByActeurId(acteurId))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k1").build()));

        var request = new KycAdminDto.DecisionRequest();
        request.setDecision("REJETE");
        request.setMotif("Pièce illisible");

        KycAdminDto.ActeurSummary resultat = service.prendreDecision(acteurId, TENANT, request);

        assertThat(resultat.niveauKyc()).isEqualTo("NIVEAU_1");
    }

    @Test
    void rejet_sans_pieces_retourne_au_niveau_0() {
        Acteur acteur = chauffeurNiveau1();
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));
        when(pieceJustificativeRepository.findByActeurId(acteurId)).thenReturn(List.of());

        var request = new KycAdminDto.DecisionRequest();
        request.setDecision("REJETE");

        KycAdminDto.ActeurSummary resultat = service.prendreDecision(acteurId, TENANT, request);

        assertThat(resultat.niveauKyc()).isEqualTo("NIVEAU_0");
    }

    @Test
    void decision_invalide_est_refusee() {
        Acteur acteur = chauffeurNiveau1();
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));

        var request = new KycAdminDto.DecisionRequest();
        request.setDecision("PEUT_ETRE");

        assertThatThrownBy(() -> service.prendreDecision(acteurId, TENANT, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DECISION_INVALIDE");
    }

    @Test
    void acteur_inexistant_est_refuse() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.empty());

        var request = new KycAdminDto.DecisionRequest();
        request.setDecision("VALIDE");

        assertThatThrownBy(() -> service.prendreDecision(acteurId, TENANT, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("KYC_ACTEUR_INTROUVABLE");
    }

    @Test
    void acteur_d_un_autre_tenant_est_refuse_comme_introuvable() {
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(chauffeurNiveau1()));

        var request = new KycAdminDto.DecisionRequest();
        request.setDecision("VALIDE");

        assertThatThrownBy(() -> service.prendreDecision(acteurId, "tenant-bnft-ndjamena", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("KYC_ACTEUR_INTROUVABLE");
    }

    @Test
    void detail_retourne_le_profil_avec_urls_presignees() {
        Acteur acteur = chauffeurNiveau1();
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));
        when(pieceJustificativeRepository.findByActeurId(acteurId))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k1").build()));
        when(documentStorageService.urlAcces("k1")).thenReturn("https://minio/presigned");

        KycAdminDto.ActeurDetail detail = service.getDetail(acteurId, TENANT);

        assertThat(detail.acteurId()).isEqualTo(acteurId);
        assertThat(detail.telephone()).isEqualTo(acteur.getTelephone());
        assertThat(detail.pieces()).hasSize(1);
        assertThat(detail.pieces().get(0).getUrl()).isEqualTo("https://minio/presigned");
    }
}

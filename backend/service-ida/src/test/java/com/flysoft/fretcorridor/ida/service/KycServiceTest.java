package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.KycDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.PieceJustificative;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.repository.OrganisationRepository;
import com.flysoft.fretcorridor.ida.repository.PieceJustificativeRepository;
import com.flysoft.fretcorridor.ida.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RG-011 : le niveau 1 exige l'identité déclarée ET au moins une pièce
 * déposée — les deux conditions dans n'importe quel ordre.
 */
class KycServiceTest {

    @Mock private ActeurRepository acteurRepository;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private PieceJustificativeRepository pieceJustificativeRepository;
    @Mock private DocumentStorageService documentStorageService;
    @Mock private JwtService jwtService;

    private KycService kycService;
    private UUID acteurId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kycService = new KycService(acteurRepository, organisationRepository,
                pieceJustificativeRepository, documentStorageService, jwtService);
        acteurId = UUID.randomUUID();
        when(acteurRepository.save(any(Acteur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.genererAccessToken(any())).thenReturn("access");
        when(jwtService.genererRefreshToken(any())).thenReturn("refresh");
    }

    private Acteur acteurNiveau0() {
        return Acteur.builder().id(acteurId).telephone("+237600000001")
                .tenantId("tenant-bgft-douala").niveauKyc(Acteur.NiveauKyc.NIVEAU_0).build();
    }

    @Test
    void declaring_identity_alone_does_not_reach_level_1() {
        Acteur acteur = acteurNiveau0();
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));
        when(pieceJustificativeRepository.findByActeurId(acteurId)).thenReturn(List.of());

        var request = new KycDto.CompleterProfilParticulierRequest();
        request.setNom("Ngono");
        request.setPrenom("Awa");

        KycDto.CompletionResponse response = kycService.completerProfilParticulier(acteurId, request);

        assertThat(response.getProfil().getNiveauKyc()).isEqualTo("NIVEAU_0");
    }

    @Test
    void depositing_a_document_alone_does_not_reach_level_1() {
        Acteur acteur = acteurNiveau0();
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));
        when(pieceJustificativeRepository.findByActeurId(acteurId)).thenReturn(List.of());
        when(documentStorageService.deposer(anyString(), any(), anyString(), any())).thenReturn("object-key-1");

        MultipartFile fichier = new MockMultipartFile("fichier", "cni.jpg", "image/jpeg", new byte[]{1, 2, 3});
        KycDto.CompletionResponse response = kycService.deposerPiece(acteurId, "CNI", fichier);

        assertThat(response.getProfil().getNiveauKyc()).isEqualTo("NIVEAU_0");
        verify(pieceJustificativeRepository).save(any(PieceJustificative.class));
    }

    @Test
    void identity_then_document_reaches_level_1() {
        Acteur acteur = acteurNiveau0();
        acteur.setNom("Ngono");
        acteur.setPrenom("Awa");
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));
        when(pieceJustificativeRepository.findByActeurId(acteurId))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k").build()));
        when(documentStorageService.deposer(anyString(), any(), anyString(), any())).thenReturn("object-key-1");

        MultipartFile fichier = new MockMultipartFile("fichier", "cni.jpg", "image/jpeg", new byte[]{1, 2, 3});
        KycDto.CompletionResponse response = kycService.deposerPiece(acteurId, "CNI", fichier);

        assertThat(response.getProfil().getNiveauKyc()).isEqualTo("NIVEAU_1");
    }

    @Test
    void document_then_identity_reaches_level_1() {
        Acteur acteur = acteurNiveau0();
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));
        when(pieceJustificativeRepository.findByActeurId(acteurId))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k").build()));

        var request = new KycDto.CompleterProfilParticulierRequest();
        request.setNom("Ngono");
        request.setPrenom("Awa");

        KycDto.CompletionResponse response = kycService.completerProfilParticulier(acteurId, request);

        assertThat(response.getProfil().getNiveauKyc()).isEqualTo("NIVEAU_1");
    }

    @Test
    void depositing_an_empty_file_is_rejected() {
        MultipartFile vide = new MockMultipartFile("fichier", "vide.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> kycService.deposerPiece(acteurId, "CNI", vide))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("FICHIER_VIDE");

        verifyNoInteractions(acteurRepository);
    }

    @Test
    void getting_the_profile_resolves_a_presigned_url_per_piece() {
        Acteur acteur = acteurNiveau0();
        when(acteurRepository.findById(acteurId)).thenReturn(Optional.of(acteur));
        when(pieceJustificativeRepository.findByActeurId(acteurId))
                .thenReturn(List.of(PieceJustificative.builder().typeDocument("CNI").objectKey("k1").build()));
        when(documentStorageService.urlAcces("k1")).thenReturn("https://minio/presigned-url");

        KycDto.ProfilResponse profil = kycService.getProfil(acteurId);

        assertThat(profil.getPieces()).hasSize(1);
        assertThat(profil.getPieces().get(0).getUrl()).isEqualTo("https://minio/presigned-url");
    }
}

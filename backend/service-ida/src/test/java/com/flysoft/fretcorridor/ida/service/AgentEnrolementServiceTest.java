package com.flysoft.fretcorridor.ida.service;

import com.flysoft.fretcorridor.ida.dto.EnrolementDto;
import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.EnrolementAgent;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import com.flysoft.fretcorridor.ida.repository.ActeurRepository;
import com.flysoft.fretcorridor.ida.repository.EnrolementAgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UC-IDA-03/RG-019 : l'OTP n'est jamais exposé à l'agent (seulement envoyé
 * par SMS), et l'activation ne renvoie jamais de jeton pour le compte créé.
 */
class AgentEnrolementServiceTest {

    @Mock private ActeurRepository acteurRepository;
    @Mock private EnrolementAgentRepository enrolementAgentRepository;
    @Mock private SmsPort smsPort;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AgentEnrolementService service;
    private UUID agentId;
    private Acteur agent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AgentEnrolementService(acteurRepository, enrolementAgentRepository, passwordEncoder, smsPort);
        agentId = UUID.randomUUID();
        agent = Acteur.builder().id(agentId).telephone("+237600000099")
                .tenantId("tenant-bgft-douala").roles(Set.of(RoleActeur.AGENT)).build();
        when(enrolementAgentRepository.save(any(EnrolementAgent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(acteurRepository.save(any(Acteur.class))).thenAnswer(inv -> {
            Acteur a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });
    }

    private EnrolementDto.InitierRequest requeteInitiale() {
        var r = new EnrolementDto.InitierRequest();
        r.setTelephone("+237600000010");
        r.setTypeActeur("CHAUFFEUR");
        r.setLatitude(4.05);
        r.setLongitude(9.7);
        r.setIdempotencyKey("idem-1");
        return r;
    }

    @Test
    void an_agent_can_initiate_an_enrolment_and_the_otp_is_sent_by_sms_never_returned() {
        when(acteurRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(enrolementAgentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(acteurRepository.existsByTelephone("+237600000010")).thenReturn(false);

        var response = service.initier(agentId, requeteInitiale());

        assertThat(response.getStatut()).isEqualTo("EN_ATTENTE");
        assertThat(response.getTelephone()).isEqualTo("+237600000010");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsPort).envoyer(eq("+237600000010"), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).matches(".*\\b\\d{6}\\b.*");
    }

    @Test
    void a_non_agent_actor_cannot_initiate_an_enrolment() {
        Acteur nonAgent = Acteur.builder().id(agentId).telephone("+237600000098")
                .tenantId("tenant-bgft-douala").roles(Set.of(RoleActeur.TRANSPORTEUR)).build();
        when(acteurRepository.findById(agentId)).thenReturn(Optional.of(nonAgent));

        assertThatThrownBy(() -> service.initier(agentId, requeteInitiale()))
                .hasMessage("ACCES_REFUSE");

        verifyNoInteractions(smsPort);
    }

    @Test
    void replaying_the_same_idempotency_key_does_not_create_a_second_enrolment() {
        when(acteurRepository.findById(agentId)).thenReturn(Optional.of(agent));
        EnrolementAgent existant = EnrolementAgent.builder()
                .id(UUID.randomUUID()).agent(agent).telephoneEnrole("+237600000010")
                .typeActeur(RoleActeur.CHAUFFEUR).codeOtpHash("x")
                .otpExpiration(LocalDateTime.now().plusMinutes(5))
                .idempotencyKey("idem-1").tenantId("tenant-bgft-douala").build();
        when(enrolementAgentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existant));

        service.initier(agentId, requeteInitiale());

        verify(enrolementAgentRepository, never()).save(any());
        verifyNoInteractions(smsPort);
    }

    @Test
    void enrolling_an_already_registered_phone_is_rejected() {
        when(acteurRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(enrolementAgentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(acteurRepository.existsByTelephone("+237600000010")).thenReturn(true);

        assertThatThrownBy(() -> service.initier(agentId, requeteInitiale()))
                .hasMessage("TELEPHONE_DEJA_UTILISE");
    }

    @Test
    void activating_with_the_correct_otp_creates_the_account_without_returning_its_tokens() {
        String otpEnClair = "654321";
        EnrolementAgent enrolement = EnrolementAgent.builder()
                .id(UUID.randomUUID()).agent(agent).telephoneEnrole("+237600000010")
                .typeActeur(RoleActeur.CHAUFFEUR).codeOtpHash(passwordEncoder.encode(otpEnClair))
                .otpExpiration(LocalDateTime.now().plusMinutes(5))
                .idempotencyKey("idem-1").tenantId("tenant-bgft-douala").build();
        when(enrolementAgentRepository.findById(enrolement.getId())).thenReturn(Optional.of(enrolement));

        var request = new EnrolementDto.ActiverRequest();
        request.setOtp(otpEnClair);
        request.setCodePin("1234");

        var response = service.activer(enrolement.getId(), request);

        assertThat(response.getStatut()).isEqualTo("ACTIVE");

        ArgumentCaptor<Acteur> acteurCaptor = ArgumentCaptor.forClass(Acteur.class);
        verify(acteurRepository).save(acteurCaptor.capture());
        assertThat(acteurCaptor.getValue().getTelephone()).isEqualTo("+237600000010");
        assertThat(acteurCaptor.getValue().getRoles()).containsExactly(RoleActeur.CHAUFFEUR);
        assertThat(passwordEncoder.matches("1234", acteurCaptor.getValue().getCodePin())).isTrue();
    }

    @Test
    void activating_with_the_wrong_otp_is_rejected() {
        EnrolementAgent enrolement = EnrolementAgent.builder()
                .id(UUID.randomUUID()).agent(agent).telephoneEnrole("+237600000010")
                .typeActeur(RoleActeur.CHAUFFEUR).codeOtpHash(passwordEncoder.encode("654321"))
                .otpExpiration(LocalDateTime.now().plusMinutes(5))
                .idempotencyKey("idem-1").tenantId("tenant-bgft-douala").build();
        when(enrolementAgentRepository.findById(enrolement.getId())).thenReturn(Optional.of(enrolement));

        var request = new EnrolementDto.ActiverRequest();
        request.setOtp("000000");
        request.setCodePin("1234");

        assertThatThrownBy(() -> service.activer(enrolement.getId(), request))
                .hasMessage("OTP_INCORRECT");

        verify(acteurRepository, never()).save(any());
    }

    @Test
    void activating_an_expired_otp_is_rejected() {
        EnrolementAgent enrolement = EnrolementAgent.builder()
                .id(UUID.randomUUID()).agent(agent).telephoneEnrole("+237600000010")
                .typeActeur(RoleActeur.CHAUFFEUR).codeOtpHash(passwordEncoder.encode("654321"))
                .otpExpiration(LocalDateTime.now().minusMinutes(1))
                .idempotencyKey("idem-1").tenantId("tenant-bgft-douala").build();
        when(enrolementAgentRepository.findById(enrolement.getId())).thenReturn(Optional.of(enrolement));

        var request = new EnrolementDto.ActiverRequest();
        request.setOtp("654321");
        request.setCodePin("1234");

        assertThatThrownBy(() -> service.activer(enrolement.getId(), request))
                .hasMessage("OTP_EXPIRE");
    }
}

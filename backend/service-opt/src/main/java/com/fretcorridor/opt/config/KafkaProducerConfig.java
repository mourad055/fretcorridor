package com.fretcorridor.opt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Remplace le KafkaTemplate<String, Object> auto-configure par Spring Boot,
 * dont le JsonSerializer instancie en interne un ObjectMapper() nu - sans
 * JavaTimeModule ni WRITE_DATES_AS_TIMESTAMPS desactive.
 *
 * Consequence observee en test manuel (2026-08-17) : tout Instant serialise
 * dans un evenement Kafka publie par OPT (PropositionEmise, AffectationConfirmee,
 * PropositionRetourAVide) sortait en epoch flottant (ex. 1786941572.685815607)
 * au lieu du ISO-8601 documente dans shared-contracts/asyncapi/ - violation de
 * contrat non detectee jusqu'ici car aucun test n'inspectait le JSON reellement
 * public sur le broker, seulement les logs applicatifs de succes/echec.
 *
 * Ce bean s'applique a TOUS les evenements sortants d'OPT (KafkaTemplate est
 * injecte generiquement dans OptEventPublisher) - un seul correctif centralise
 * plutot qu'un ObjectMapper par type d'evenement.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Pas d'en-tete __TypeId__ : coherent avec KafkaConsumerConfig (cote
        // consommateur, USE_TYPE_INFO_HEADERS=false) - le producteur Mobile et
        // les outils de test (kafka-console-producer) n'en emettent jamais, on
        // aligne le comportement dans les deux sens plutot que de deposer un
        // en-tete qu'aucun consommateur reel n'exploite.
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props, null, jsonSerializer());
    }

    private JsonSerializer<Object> jsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        // JavaTimeModule : seule facon de serialiser Instant/LocalDate/etc.
        // Sans lui, Jackson se rabat sur une reflexion generique qui expose
        // les champs internes de Instant (secondes epoch + nanos) plutot que
        // d'utiliser le format ISO-8601 attendu par shared-contracts/asyncapi/.
        objectMapper.registerModule(new JavaTimeModule());
        // Desactive explicitement (le defaut Jackson serialise les dates comme
        // timestamp numerique meme avec JavaTimeModule si ce flag reste actif) :
        // c'est la cause directe du bug observe, pas seulement l'absence du module.
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new JsonSerializer<>(objectMapper);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}

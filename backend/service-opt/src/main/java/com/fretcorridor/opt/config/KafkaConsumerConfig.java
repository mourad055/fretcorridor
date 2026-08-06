package com.fretcorridor.opt.config;

import com.fretcorridor.opt.messaging.CapaciteDeclareeEvent;
import com.fretcorridor.opt.messaging.DemandePublieeEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Deux factories Kafka dediees (une par type d'evenement entrant), plutot que
 * la factory generique auto-configuree par Spring Boot.
 *
 * RAISON : JsonDeserializer exige par defaut un en-tete __TypeId__ pour savoir
 * vers quel type Java desserialiser un message - en-tete que le producteur
 * Mobile (pas encore implemente/valide, cf BROUILLON sur les Event) n'ajoutera
 * pas forcement, et qu'un producteur de test brut (kafka-console-producer)
 * n'ajoute jamais. On fixe le type cible par factory (USE_TYPE_INFO_HEADERS=
 * false) plutot que de dependre d'un en-tete fragile.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> proprietesBase() {
        // TRUSTED_PACKAGES retire de cette map volontairement : il est
        // configure directement sur chaque instance JsonDeserializer via
        // addTrustedPackages() ci-dessous. Les deux deserializers sont deja
        // configures par setter (constructeur + setUseTypeHeaders) - Spring
        // Kafka refuse un deserializer configure a la fois par setter ET par
        // configure(props) avec des cles de deserializer dans la map
        // (IllegalStateException au demarrage : "must be configured with
        // property setters, or via configuration properties; not both").
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "service-opt");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Bean
    public ConsumerFactory<String, CapaciteDeclareeEvent> capaciteDeclareeConsumerFactory() {
        JsonDeserializer<CapaciteDeclareeEvent> deserializer =
                new JsonDeserializer<>(CapaciteDeclareeEvent.class, false);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("com.fretcorridor.*");
        return new DefaultKafkaConsumerFactory<>(proprietesBase(), new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CapaciteDeclareeEvent>
            capaciteDeclareeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CapaciteDeclareeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(capaciteDeclareeConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, DemandePublieeEvent> demandePublieeConsumerFactory() {
        JsonDeserializer<DemandePublieeEvent> deserializer =
                new JsonDeserializer<>(DemandePublieeEvent.class, false);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("com.fretcorridor.*");
        return new DefaultKafkaConsumerFactory<>(proprietesBase(), new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DemandePublieeEvent>
            demandePublieeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DemandePublieeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(demandePublieeConsumerFactory());
        return factory;
    }
}

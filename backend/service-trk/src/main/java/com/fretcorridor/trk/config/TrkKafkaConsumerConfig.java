package com.fretcorridor.trk.config;

import com.fretcorridor.trk.messaging.EtapeExecuteeEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabriques de listeners Kafka dediees pour TRK (point 6 du plan de
 * reorientation : consommation de l'evenement EtapeExecutee pour basculer
 * "position estimee du colis" vers "position GPS temps reel du chauffeur").
 *
 * RAISON d'une fabrique dediee : application.yml configure un unique
 * JsonDeserializer branche sur PositionBruteEvent (spring.json.value.default.type
 * + spring.json.use.type.headers=false, car service-flt ne pose pas l'en-tete
 * de type Spring). Consommer EtapeExecutee avec cette meme fabrique par defaut
 * deserialiserait chaque message comme un PositionBruteEvent - incoherent.
 * La fabrique dediee force le type cible (style explicite, same que
 * KafkaConsumerConfig cote service-opt) : independante de l'en-tete __TypeId__.
 */
@Configuration
public class TrkKafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EtapeExecuteeEvent>
            etapeExecuteeKafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        JsonDeserializer<EtapeExecuteeEvent> deserializer =
                new JsonDeserializer<>(EtapeExecuteeEvent.class, false);
        deserializer.addTrustedPackages("com.fretcorridor.trk.messaging");

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "service-trk");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        ConsumerFactory<String, EtapeExecuteeEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);

        ConcurrentKafkaListenerContainerFactory<String, EtapeExecuteeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}

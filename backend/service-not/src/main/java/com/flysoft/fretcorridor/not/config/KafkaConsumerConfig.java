package com.flysoft.fretcorridor.not.config;

import com.flysoft.fretcorridor.not.messaging.AlerteEcartEvent;
import com.flysoft.fretcorridor.not.messaging.PropositionRetourAVideEvent;
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
 * Même principe que KafkaConsumerConfig côté service-exe : factory dédiée
 * avec USE_TYPE_INFO_HEADERS=false, pour ne pas dépendre d'un en-tête
 * __TypeId__ que le producteur (service-opt, Moteur) n'ajoute pas forcément.
 * Première consommation Kafka de service-not (S12).
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, PropositionRetourAVideEvent> propositionRetourAVideConsumerFactory() {
        JsonDeserializer<PropositionRetourAVideEvent> deserializer =
                new JsonDeserializer<>(PropositionRetourAVideEvent.class, false);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("com.fretcorridor.*", "com.flysoft.fretcorridor.*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "service-not");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PropositionRetourAVideEvent>
            propositionRetourAVideKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PropositionRetourAVideEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(propositionRetourAVideConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, AlerteEcartEvent> alerteEcartConsumerFactory() {
        JsonDeserializer<AlerteEcartEvent> deserializer =
                new JsonDeserializer<>(AlerteEcartEvent.class, false);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("com.fretcorridor.*", "com.flysoft.fretcorridor.*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "service-not");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AlerteEcartEvent>
            alerteEcartKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AlerteEcartEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(alerteEcartConsumerFactory());
        return factory;
    }
}

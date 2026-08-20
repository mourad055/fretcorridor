package com.flysoft.fretcorridor.exe.config;

import com.flysoft.fretcorridor.exe.messaging.AffectationConfirmeeEvent;
import com.flysoft.fretcorridor.exe.messaging.TourneeConstitueeEvent;
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
 * Même principe que KafkaConsumerConfig côté service-bur : factory dédiée
 * avec USE_TYPE_INFO_HEADERS=false, pour ne pas dépendre d'un en-tête
 * __TypeId__ que le producteur (service-opt, Moteur) n'ajoute pas forcément.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, AffectationConfirmeeEvent> affectationConfirmeeConsumerFactory() {
        JsonDeserializer<AffectationConfirmeeEvent> deserializer =
                new JsonDeserializer<>(AffectationConfirmeeEvent.class, false);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("com.fretcorridor.*", "com.flysoft.fretcorridor.*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "service-exe");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AffectationConfirmeeEvent>
            affectationConfirmeeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AffectationConfirmeeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(affectationConfirmeeConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, TourneeConstitueeEvent> tourneeConstitueeConsumerFactory() {
        JsonDeserializer<TourneeConstitueeEvent> deserializer =
                new JsonDeserializer<>(TourneeConstitueeEvent.class, false);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("com.fretcorridor.*", "com.flysoft.fretcorridor.*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "service-exe");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TourneeConstitueeEvent>
            tourneeConstitueeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TourneeConstitueeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(tourneeConstitueeConsumerFactory());
        return factory;
    }
}

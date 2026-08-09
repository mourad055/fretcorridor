package com.flysoft.fretcorridor.mkt.config;

import com.flysoft.fretcorridor.mkt.messaging.PropositionEmiseEvent;
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

// Meme raison que cote service-opt : JsonDeserializer exige par defaut un
// en-tete __TypeId__ absent des producteurs de test/autres services - on
// fixe le type cible par factory plutot que de dependre de cet en-tete.
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, PropositionEmiseEvent> propositionEmiseConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "service-mkt");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.flysoft.fretcorridor.*,com.fretcorridor.*");

        JsonDeserializer<PropositionEmiseEvent> deserializer =
                new JsonDeserializer<>(PropositionEmiseEvent.class, false);
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PropositionEmiseEvent>
            propositionEmiseKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PropositionEmiseEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(propositionEmiseConsumerFactory());
        return factory;
    }
}

package com.reactivecommerce.review.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configura los beans Reactor Kafka del Review Service.
 *
 * KafkaSender:
 *   Publica review.created, review.hidden, review.restored.
 *
 * KafkaReceiver — tópicos consumidos:
 *   order.completed  → verifica que el buyer tiene una orden completada del asset
 *                      antes de crear la reseña. Actualmente la verificación se
 *                      hace en CreateReviewUseCaseImpl; este consumer registra el
 *                      evento para desacoplar futuras validaciones.
 *   order.refunded   → si se reembolsa una orden, la reseña del asset no se elimina
 *                      (decisión de negocio: la experiencia del buyer fue real).
 *                      El consumer puede ocultarla automáticamente si la plataforma
 *                      lo decide en el futuro.
 */
@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaSender<String, String> kafkaSender() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,      bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG,                   "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,     true);
        props.put(ProducerConfig.RETRIES_CONFIG,                3);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        return KafkaSender.create(SenderOptions.create(props));
    }

    @Bean
    public KafkaReceiver<String, String> kafkaReceiver() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,                 "review-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,        "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,       false);

        ReceiverOptions<String, String> options = ReceiverOptions
            .<String, String>create(props)
            .subscription(List.of(
                "order.completed",  // buyer completó compra → puede dejar reseña
                "order.refunded"    // reembolso → posible acción sobre la reseña
            ));
        return KafkaReceiver.create(options);
    }
}

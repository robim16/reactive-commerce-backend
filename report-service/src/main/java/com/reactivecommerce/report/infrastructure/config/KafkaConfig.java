package com.reactivecommerce.report.infrastructure.config;

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
 * Configura los beans Reactor Kafka del Report Service.
 *
 * KafkaSender:
 *   Publica report.ready cuando un informe termina de generarse.
 *   El Notification Service consume este evento para alertar al solicitante.
 *
 * KafkaReceiver — tópicos consumidos:
 *   download.requested  → registra cada descarga en las estadísticas del creator.
 *   order.completed     → registra cada venta para los informes de ventas.
 *   report.requested    → self-consume: dispara la generación asíncrona del informe
 *                         (permite desacoplar la solicitud del procesamiento).
 *
 * Todos los consumers usan ack manual para garantizar que el evento se procese
 * correctamente antes de confirmar el offset.
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
        props.put(ConsumerConfig.GROUP_ID_CONFIG,                 "report-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,        "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,       false);

        ReceiverOptions<String, String> options = ReceiverOptions
            .<String, String>create(props)
            .subscription(List.of(
                "download.requested",   // registra descarga en estadísticas
                "order.completed",      // registra venta en estadísticas
                "report.requested"      // self-consume: dispara generación
            ));
        return KafkaReceiver.create(options);
    }
}

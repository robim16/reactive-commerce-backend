package com.reactivecommerce.notification.infrastructure.adapter.aws;

import com.reactivecommerce.notification.domain.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SesEmailAdapter implements EmailPort {

    private final SesAsyncClient sesClient;

    @Value("${aws.ses.from-address}")
    private String fromAddress;

    @Override
    public Mono<Void> send(String to, String templateName, Map<String, Object> variables) {
        // In production: render template from S3 via Lambda
        String subject = buildSubject(templateName);
        String body = buildBody(templateName, variables);

        SendEmailRequest request = SendEmailRequest.builder()
            .source(fromAddress)
            .destination(Destination.builder().toAddresses(to).build())
            .message(Message.builder()
                .subject(Content.builder().data(subject).charset("UTF-8").build())
                .body(Body.builder()
                    .html(Content.builder().data(body).charset("UTF-8").build())
                    .build())
                .build())
            .build();

        return Mono.fromFuture(sesClient.sendEmail(request))
            .doOnSuccess(r -> log.info("Email sent to {} via template {}", to, templateName))
            .doOnError(e -> log.error("Failed to send email to {}: {}", to, e.getMessage()))
            .then();
    }

    private String buildSubject(String template) {
        return switch (template) {
            case "order-completed"  -> "¡Tu compra fue exitosa!";
            case "asset-approved"   -> "Tu asset fue aprobado";
            case "asset-rejected"   -> "Tu asset necesita revisión";
            case "order-refunded"   -> "Reembolso procesado";
            case "user-registered"  -> "Bienvenido a ReactiveCommerce";
            default -> "Notificación de ReactiveCommerce";
        };
    }

    private String buildBody(String template, Map<String, Object> vars) {
        return "<html><body><p>Notificación: " + template + "</p></body></html>";
    }
}

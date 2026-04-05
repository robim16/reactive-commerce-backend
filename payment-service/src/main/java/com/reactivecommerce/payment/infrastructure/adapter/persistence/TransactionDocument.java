package com.reactivecommerce.payment.infrastructure.adapter.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Documento MongoDB para Transaction.
 *
 * Colección: transactions
 * Índices:
 *   orderId  → único, consulta frecuente desde PaymentHandler
 *   status   → para reportes y auditoría regulatoria
 *   createdAt (TTL no aplica — registros de pago se retienen indefinidamente)
 *
 * Los datos de tarjeta nunca se almacenan aquí (delegados al gateway).
 * Solo se persiste el gatewayTransactionId (token opaco del gateway).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class TransactionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderId;

    private BigDecimal amount;
    private BigDecimal platformCommission;
    private BigDecimal creatorAmount;
    private String status;
    private String gatewayTransactionId;
    private String failureCode;
    private String failureMessage;
    private Instant createdAt;
}

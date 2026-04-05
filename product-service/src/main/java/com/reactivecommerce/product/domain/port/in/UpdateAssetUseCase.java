package com.reactivecommerce.product.domain.port.in;

import com.reactivecommerce.product.domain.model.Asset;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso de actualización de metadatos de un asset (HU-PRO-03).
 *
 * Campos actualizables por el creator en cualquier estado post-creación:
 *   title, description, price, tags, license.
 *
 * Restricciones del dominio:
 *   - Solo el creador del asset puede actualizarlo.
 *   - El cambio de precio NO afecta pedidos en curso (el precio se captura
 *     en Order.amount al crear el pedido).
 *   - No se puede cambiar categoría ni formato (afectaría la moderación previa).
 *   - Campos nulos en el command significan "no cambiar".
 */
public interface UpdateAssetUseCase {

    record Command(
        UUID        assetId,
        UUID        requesterId,   // debe coincidir con creatorId
        String      title,         // null = no cambiar
        String      description,   // null = no cambiar
        BigDecimal  price,         // null = no cambiar
        List<String> tags,         // null = no cambiar
        String      license        // null = no cambiar
    ) {}

    Mono<Asset> execute(Command command);
}

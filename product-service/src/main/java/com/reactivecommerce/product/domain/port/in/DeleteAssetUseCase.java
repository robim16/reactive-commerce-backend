package com.reactivecommerce.product.domain.port.in;

import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Caso de uso de eliminación de asset (HU-PRO-03).
 *
 * Reglas de negocio:
 *   - Solo el creador puede eliminar su asset.
 *   - Si el asset tiene ventas (totalSales > 0) la eliminación se rechaza;
 *     el creator solo puede despublicarlo (UNPUBLISHED).
 *   - Al eliminar: se borra el registro de la base de datos y el objeto en S3
 *     (archivo original + miniatura si existe).
 *   - Assets en estado PENDING_MODERATION o REJECTED sí se pueden eliminar
 *     aunque no tengan ventas (el creator corrige y vuelve a subir).
 */
public interface DeleteAssetUseCase {

    record Command(UUID assetId, UUID requesterId) {}

    Mono<Void> execute(Command command);
}

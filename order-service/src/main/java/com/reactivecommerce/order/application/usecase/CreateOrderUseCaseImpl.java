package com.reactivecommerce.order.application.usecase;

import com.reactivecommerce.order.domain.model.Order;
import com.reactivecommerce.order.domain.port.in.CreateOrderUseCase;
import com.reactivecommerce.order.domain.port.out.AssetPricePort;
import com.reactivecommerce.order.domain.port.out.DomainEventPublisher;
import com.reactivecommerce.order.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final AssetPricePort assetPricePort;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Mono<Order> execute(Command command) {
        return orderRepository.existsByBuyerIdAndAssetId(command.buyerId(), command.assetId())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new IllegalStateException(
                        "Ya tienes este asset. Ve a tu biblioteca."));
                }
                return assetPricePort.isAvailable(command.assetId());
            })
            .flatMap(available -> {
                if (!available) {
                    return Mono.error(new IllegalStateException("Asset not available for purchase"));
                }
                return assetPricePort.getPrice(command.assetId());
            })
            .map(price -> Order.create(command.buyerId(), command.assetId(), price))
            .flatMap(orderRepository::save)
            .flatMap(order -> eventPublisher.publish("order.created", order.id().toString(),
                    Map.of("orderId", order.id(), "buyerId", order.buyerId(),
                           "assetId", order.assetId(), "amount", order.amount()))
                .thenReturn(order))
            .doOnSuccess(o -> log.info("Order created: {}", o.id()));
    }
}

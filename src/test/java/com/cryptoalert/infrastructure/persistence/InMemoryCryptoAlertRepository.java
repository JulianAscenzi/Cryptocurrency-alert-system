package com.cryptoalert.infrastructure.persistence;

import com.cryptoalert.domain.model.AlertStatus;
import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.domain.repository.CryptoAlertRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCryptoAlertRepository implements CryptoAlertRepository {

    private final ConcurrentHashMap<UUID, CryptoAlert> store = new ConcurrentHashMap<>();

    @Override
    public Uni<CryptoAlert> save(CryptoAlert alert) {
        store.put(alert.getId(), alert);
        return Uni.createFrom().item(alert);
    }

    @Override
    public Uni<CryptoAlert> findById(UUID id) {
        return Uni.createFrom().optional(Optional.ofNullable(store.get(id)));
    }

    @Override
    public Multi<CryptoAlert> findAllActive() {
        return Multi.createFrom().iterable(store.values().stream()
                .filter(alert -> alert.getStatus() == AlertStatus.ACTIVE)
                .toList());
    }

    @Override
    public Multi<CryptoAlert> findActiveBySymbol(String symbol) {
        return Multi.createFrom().iterable(store.values().stream()
                .filter(alert -> alert.getStatus() == AlertStatus.ACTIVE)
                .filter(alert -> alert.getSymbol().equalsIgnoreCase(symbol))
                .toList());
    }

    @Override
    public Uni<Void> update(CryptoAlert alert) {
        store.put(alert.getId(), alert);
        return Uni.createFrom().voidItem();
    }
}

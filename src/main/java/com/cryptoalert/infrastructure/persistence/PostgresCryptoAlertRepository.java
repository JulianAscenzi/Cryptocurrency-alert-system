package com.cryptoalert.infrastructure.persistence;

import com.cryptoalert.domain.model.AlertStatus;
import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.domain.repository.CryptoAlertRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class PostgresCryptoAlertRepository implements CryptoAlertRepository {

    @Override
    public Uni<CryptoAlert> save(CryptoAlert alert) {
        CryptoAlertEntity entity = CryptoAlertEntity.fromDomain(alert);
        return Panache.withTransaction(entity::persist)
                .replaceWith(alert);
    }

    @Override
    public Uni<CryptoAlert> findById(UUID id) {
        return Panache.withSession(() -> CryptoAlertEntity.<CryptoAlertEntity>findById(id))
                .map(entity -> entity == null ? null : entity.toDomain());
    }

    @Override
    public Multi<CryptoAlert> findAllActive() {
        return Panache.withSession(() -> CryptoAlertEntity.<CryptoAlertEntity>find("status", AlertStatus.ACTIVE).list())
                .onItem().transformToMulti(entities -> Multi.createFrom().iterable(entities))
                .map(CryptoAlertEntity::toDomain);
    }

    @Override
    public Multi<CryptoAlert> findActiveBySymbol(String symbol) {
        String normalizedSymbol = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        return Panache.withSession(() -> CryptoAlertEntity.<CryptoAlertEntity>find(
                        "status = ?1 and symbol = ?2",
                        AlertStatus.ACTIVE,
                        normalizedSymbol
                ).list())
                .onItem().transformToMulti(entities -> Multi.createFrom().iterable(entities))
                .map(CryptoAlertEntity::toDomain);
    }

    @Override
    public Uni<Void> update(CryptoAlert alert) {
        return Panache.withTransaction(() -> CryptoAlertEntity.<CryptoAlertEntity>findById(alert.getId())
                .onItem().ifNull().failWith(() -> new IllegalArgumentException("Alert not found"))
                .invoke(entity -> entity.updateFrom(alert)))
                .replaceWithVoid();
    }
}

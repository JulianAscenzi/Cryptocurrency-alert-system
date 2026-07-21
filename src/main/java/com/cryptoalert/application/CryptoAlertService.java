package com.cryptoalert.application;

import com.cryptoalert.domain.model.AlertCondition;
import com.cryptoalert.domain.model.AlertStatus;
import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.domain.repository.CryptoAlertRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class CryptoAlertService {

    private final CryptoAlertRepository repository;

    @Inject
    public CryptoAlertService(CryptoAlertRepository repository) {
        this.repository = repository;
    }

    public Uni<CryptoAlert> createAlert(String symbol, BigDecimal targetPrice, AlertCondition condition) {
        if (symbol == null || symbol.trim().isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("symbol is required"));
        }
        if (targetPrice == null || targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("targetPrice must be positive"));
        }
        if (condition == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("condition is required"));
        }

        CryptoAlert alert = CryptoAlert.createNew(symbol.trim().toUpperCase(Locale.ROOT), targetPrice, condition);
        return repository.save(alert);
    }

    public Uni<CryptoAlert> getAlert(UUID id) {
        return repository.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Alert not found"));
    }

    public Uni<java.util.List<CryptoAlert>> listActiveAlerts() {
        return repository.findAllActive()
                .collect().asList();
    }

    public Uni<CryptoAlert> cancelAlert(UUID id) {
        return repository.findById(id)
                .onItem().ifNull().failWith(new NotFoundException("Alert not found"))
                .flatMap(alert -> {
                    if (alert.getStatus() == AlertStatus.TRIGGERED) {
                        return Uni.createFrom().failure(new IllegalStateException("Triggered alerts cannot be cancelled"));
                    }
                    alert.cancel();
                    return repository.update(alert).replaceWith(alert);
                });
    }
}

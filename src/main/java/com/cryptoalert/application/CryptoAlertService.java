package com.cryptoalert.application;

import com.cryptoalert.domain.model.AlertCondition;
import com.cryptoalert.domain.model.AlertStatus;
import com.cryptoalert.domain.model.CryptoAlert;
import com.cryptoalert.domain.repository.CryptoAlertRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.util.UUID;

@ApplicationScoped
public class CryptoAlertService {

    private final CryptoAlertRepository repository;

    @Inject
    public CryptoAlertService(CryptoAlertRepository repository) {
        this.repository = repository;
    }

    public Uni<CryptoAlert> createAlert(String symbol, BigDecimal targetPrice, AlertCondition condition) {
        CryptoAlert alert = CryptoAlert.createNew(symbol, targetPrice, condition);
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

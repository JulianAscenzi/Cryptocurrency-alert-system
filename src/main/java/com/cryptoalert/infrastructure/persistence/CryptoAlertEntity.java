package com.cryptoalert.infrastructure.persistence;

import com.cryptoalert.domain.model.AlertCondition;
import com.cryptoalert.domain.model.AlertStatus;
import com.cryptoalert.domain.model.CryptoAlert;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crypto_alerts")
public class CryptoAlertEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String symbol;

    @Column(nullable = false, precision = 38, scale = 18)
    public BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AlertCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AlertStatus status;

    @Column(nullable = false)
    public Instant createdAt;

    public Instant triggeredAt;

    static CryptoAlertEntity fromDomain(CryptoAlert alert) {
        CryptoAlertEntity entity = new CryptoAlertEntity();
        entity.id = alert.getId();
        entity.symbol = alert.getSymbol();
        entity.targetPrice = alert.getTargetPrice();
        entity.condition = alert.getCondition();
        entity.status = alert.getStatus();
        entity.createdAt = alert.getCreatedAt();
        entity.triggeredAt = alert.getTriggeredAt();
        return entity;
    }

    CryptoAlert toDomain() {
        return new CryptoAlert(id, symbol, targetPrice, condition, status, createdAt, triggeredAt);
    }

    void updateFrom(CryptoAlert alert) {
        symbol = alert.getSymbol();
        targetPrice = alert.getTargetPrice();
        condition = alert.getCondition();
        status = alert.getStatus();
        createdAt = alert.getCreatedAt();
        triggeredAt = alert.getTriggeredAt();
    }
}

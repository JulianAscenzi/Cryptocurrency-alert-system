package com.cryptoalert.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Entity representing a price alert configured by a user.
 */
public class CryptoAlert {
    private final UUID id;
    private final String symbol;
    private final BigDecimal targetPrice;
    private final AlertCondition condition;
    private AlertStatus status;
    private final Instant createdAt;
    private Instant triggeredAt;

    public CryptoAlert(UUID id, String symbol, BigDecimal targetPrice, AlertCondition condition, AlertStatus status, Instant createdAt, Instant triggeredAt) {
        this.id = Objects.requireNonNull(id, "Alert ID cannot be null");
        this.symbol = normalizeSymbol(symbol);
        this.targetPrice = validateTargetPrice(targetPrice);
        this.condition = Objects.requireNonNull(condition, "Alert condition cannot be null");
        this.status = Objects.requireNonNull(status, "Alert status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation timestamp cannot be null");
        this.triggeredAt = triggeredAt;
    }

    /**
     * Factory method to create a new, active alert.
     */
    public static CryptoAlert createNew(String symbol, BigDecimal targetPrice, AlertCondition condition) {
        return new CryptoAlert(
            UUID.randomUUID(),
            symbol,
            targetPrice,
            condition,
            AlertStatus.ACTIVE,
            Instant.now(),
            null
        );
    }

    private static String normalizeSymbol(String symbol) {
        String normalized = Objects.requireNonNull(symbol, "Crypto symbol cannot be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Crypto symbol cannot be blank");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static BigDecimal validateTargetPrice(BigDecimal targetPrice) {
        BigDecimal value = Objects.requireNonNull(targetPrice, "Target price cannot be null");
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target price must be positive");
        }
        return value;
    }

    /**
     * Evaluates whether the incoming price record satisfies this alert's trigger conditions.
     */
    public boolean checkTriggerCondition(PriceRecord priceRecord) {
        if (this.status != AlertStatus.ACTIVE) {
            return false;
        }

        if (!this.symbol.equalsIgnoreCase(priceRecord.symbol())) {
            return false;
        }

        BigDecimal currentPrice = priceRecord.price();
        return switch (this.condition) {
            case ABOVE -> currentPrice.compareTo(this.targetPrice) >= 0;
            case BELOW -> currentPrice.compareTo(this.targetPrice) <= 0;
        };
    }

    /**
     * Triggers the alert, modifying its state and recording the trigger timestamp.
     */
    public void trigger(Instant triggeredTime) {
        if (this.status != AlertStatus.ACTIVE) {
            throw new IllegalStateException("Only active alerts can be triggered");
        }
        this.status = AlertStatus.TRIGGERED;
        this.triggeredAt = Objects.requireNonNull(triggeredTime, "Trigger timestamp cannot be null");
    }

    /**
     * Cancels the alert.
     */
    public void cancel() {
        if (this.status == AlertStatus.TRIGGERED) {
            throw new IllegalStateException("Triggered alerts cannot be cancelled");
        }
        this.status = AlertStatus.CANCELLED;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public AlertCondition getCondition() {
        return condition;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CryptoAlert that = (CryptoAlert) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CryptoAlert{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", targetPrice=" + targetPrice +
                ", condition=" + condition +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", triggeredAt=" + triggeredAt +
                '}';
    }
}

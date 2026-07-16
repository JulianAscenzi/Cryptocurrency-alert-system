package com.cryptoalert.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CryptoAlertTest {

    @Test
    void createNewInitializesActiveAlertWithGeneratedId() {
        CryptoAlert alert = CryptoAlert.createNew("btc", new BigDecimal("50000"), AlertCondition.ABOVE);

        assertNotNull(alert.getId());
        assertEquals("BTC", alert.getSymbol());
        assertEquals(new BigDecimal("50000"), alert.getTargetPrice());
        assertEquals(AlertCondition.ABOVE, alert.getCondition());
        assertEquals(AlertStatus.ACTIVE, alert.getStatus());
        assertNotNull(alert.getCreatedAt());
        assertNull(alert.getTriggeredAt());
    }

    @Test
    void checkTriggerConditionReturnsTrueForMatchingAboveAlert() {
        CryptoAlert alert = new CryptoAlert(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "btc",
                new BigDecimal("40000"),
                AlertCondition.ABOVE,
                AlertStatus.ACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );

        boolean triggered = alert.checkTriggerCondition(new PriceRecord("BTC", new BigDecimal("40000"), Instant.now()));

        assertTrue(triggered);
    }

    @Test
    void checkTriggerConditionReturnsFalseForInactiveAlert() {
        CryptoAlert alert = new CryptoAlert(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174001"),
                "eth",
                new BigDecimal("3000"),
                AlertCondition.ABOVE,
                AlertStatus.TRIGGERED,
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );

        boolean triggered = alert.checkTriggerCondition(new PriceRecord("ETH", new BigDecimal("4000"), Instant.now()));

        assertFalse(triggered);
    }

    @Test
    void checkTriggerConditionReturnsFalseForDifferentSymbol() {
        CryptoAlert alert = new CryptoAlert(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174002"),
                "btc",
                new BigDecimal("40000"),
                AlertCondition.ABOVE,
                AlertStatus.ACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );

        boolean triggered = alert.checkTriggerCondition(new PriceRecord("eth", new BigDecimal("50000"), Instant.now()));

        assertFalse(triggered);
    }

    @Test
    void triggerSetsStatusAndTimestamp() {
        CryptoAlert alert = new CryptoAlert(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174003"),
                "btc",
                new BigDecimal("40000"),
                AlertCondition.BELOW,
                AlertStatus.ACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );
        Instant triggerTime = Instant.parse("2024-02-01T10:15:30Z");

        alert.trigger(triggerTime);

        assertEquals(AlertStatus.TRIGGERED, alert.getStatus());
        assertEquals(triggerTime, alert.getTriggeredAt());
    }

    @Test
    void triggerThrowsForNonActiveAlert() {
        CryptoAlert alert = new CryptoAlert(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174004"),
                "btc",
                new BigDecimal("40000"),
                AlertCondition.BELOW,
                AlertStatus.TRIGGERED,
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> alert.trigger(Instant.now()));
        assertEquals("Only active alerts can be triggered", exception.getMessage());
    }

    @Test
    void cancelThrowsForTriggeredAlert() {
        CryptoAlert alert = new CryptoAlert(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174005"),
                "btc",
                new BigDecimal("40000"),
                AlertCondition.BELOW,
                AlertStatus.TRIGGERED,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z")
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, alert::cancel);
        assertEquals("Triggered alerts cannot be cancelled", exception.getMessage());
    }

    @Test
    void cancelTransitionsActiveAlertToCancelled() {
        CryptoAlert alert = new CryptoAlert(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174006"),
                "btc",
                new BigDecimal("40000"),
                AlertCondition.BELOW,
                AlertStatus.ACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );

        alert.cancel();

        assertEquals(AlertStatus.CANCELLED, alert.getStatus());
    }

    @Test
    void equalsAndHashCodeUseIdOnly() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174007");
        CryptoAlert first = new CryptoAlert(id, "btc", new BigDecimal("40000"), AlertCondition.BELOW, AlertStatus.ACTIVE, Instant.parse("2024-01-01T00:00:00Z"), null);
        CryptoAlert second = new CryptoAlert(id, "eth", new BigDecimal("50000"), AlertCondition.ABOVE, AlertStatus.TRIGGERED, Instant.parse("2024-01-02T00:00:00Z"), Instant.now());

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}

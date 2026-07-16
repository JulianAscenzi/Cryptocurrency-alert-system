package com.cryptoalert.infrastructure.persistence;

import com.cryptoalert.domain.model.AlertCondition;
import com.cryptoalert.domain.model.AlertStatus;
import com.cryptoalert.domain.model.CryptoAlert;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCryptoAlertRepositoryTest {

    private InMemoryCryptoAlertRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCryptoAlertRepository();
    }

    @Test
    void saveShouldPersistAlertAndReturnIt() {
        CryptoAlert alert = CryptoAlert.createNew("btc", new BigDecimal("42000"), AlertCondition.ABOVE);

        CryptoAlert saved = repository.save(alert).await().indefinitely();

        assertSame(alert, saved);
    }

    @Test
    void findByIdShouldReturnSavedAlert() {
        CryptoAlert alert = CryptoAlert.createNew("eth", new BigDecimal("3000"), AlertCondition.BELOW);
        repository.save(alert).await().indefinitely();

        CryptoAlert found = repository.findById(alert.getId()).await().indefinitely();

        assertEquals(alert, found);
    }

    @Test
    void findByIdShouldCompleteEmptyWhenNotFound() {
        Uni<CryptoAlert> result = repository.findById(UUID.randomUUID());

        assertNull(result.await().indefinitely());
    }

    @Test
    void findAllActiveShouldReturnOnlyActiveAlerts() {
        CryptoAlert active = CryptoAlert.createNew("btc", new BigDecimal("25000"), AlertCondition.ABOVE);
        CryptoAlert cancelled = new CryptoAlert(
                UUID.randomUUID(),
                "btc",
                new BigDecimal("25000"),
                AlertCondition.ABOVE,
                AlertStatus.CANCELLED,
                Instant.now(),
                null
        );

        repository.save(active).await().indefinitely();
        repository.save(cancelled).await().indefinitely();

        Multi<CryptoAlert> activeAlerts = repository.findAllActive();
        assertEquals(1, activeAlerts.collect().asList().await().indefinitely().size());
        assertEquals(active, activeAlerts.collect().asList().await().indefinitely().get(0));
    }

    @Test
    void findActiveBySymbolShouldReturnActiveAlertsForSymbol() {
        CryptoAlert activeBtc = CryptoAlert.createNew("btc", new BigDecimal("25000"), AlertCondition.ABOVE);
        CryptoAlert activeEth = CryptoAlert.createNew("eth", new BigDecimal("2500"), AlertCondition.ABOVE);

        repository.save(activeBtc).await().indefinitely();
        repository.save(activeEth).await().indefinitely();

        Multi<CryptoAlert> btcAlerts = repository.findActiveBySymbol("BTC");
        assertEquals(1, btcAlerts.collect().asList().await().indefinitely().size());
        assertEquals(activeBtc, btcAlerts.collect().asList().await().indefinitely().get(0));
    }

    @Test
    void updateShouldReplaceExistingAlert() {
        CryptoAlert alert = CryptoAlert.createNew("btc", new BigDecimal("20000"), AlertCondition.ABOVE);
        repository.save(alert).await().indefinitely();

        alert.cancel();
        repository.update(alert).await().indefinitely();

        CryptoAlert updated = repository.findById(alert.getId()).await().indefinitely();
        assertEquals(AlertStatus.CANCELLED, updated.getStatus());
    }
}

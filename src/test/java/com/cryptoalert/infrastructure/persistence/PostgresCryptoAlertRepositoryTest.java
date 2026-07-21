package com.cryptoalert.infrastructure.persistence;

import com.cryptoalert.domain.model.AlertCondition;
import com.cryptoalert.domain.model.AlertStatus;
import com.cryptoalert.domain.model.CryptoAlert;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class PostgresCryptoAlertRepositoryTest {

    @Inject
    PostgresCryptoAlertRepository repository;

    @Test
    void saveAndFindByIdRoundTripsAlert() {
        CryptoAlert alert = CryptoAlert.createNew("btcusdt", new BigDecimal("42000"), AlertCondition.ABOVE);

        CryptoAlert saved = repository.save(alert).await().indefinitely();
        CryptoAlert found = repository.findById(saved.getId()).await().indefinitely();

        assertEquals(alert.getId(), found.getId());
        assertEquals("BTCUSDT", found.getSymbol());
        assertEquals(AlertStatus.ACTIVE, found.getStatus());
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        assertNull(repository.findById(java.util.UUID.randomUUID()).await().indefinitely());
    }

    @Test
    void findAllActiveReturnsOnlyActiveAlerts() {
        CryptoAlert active = repository.save(CryptoAlert.createNew("adausdt", new BigDecimal("1.5"), AlertCondition.ABOVE))
                .await().indefinitely();
        CryptoAlert cancelled = CryptoAlert.createNew("solusdt", new BigDecimal("120"), AlertCondition.BELOW);
        repository.save(cancelled).await().indefinitely();
        cancelled.cancel();
        repository.update(cancelled).await().indefinitely();

        var activeAlerts = repository.findAllActive().collect().asList().await().indefinitely();

        assertNotNull(activeAlerts.stream().filter(alert -> alert.getId().equals(active.getId())).findFirst().orElse(null));
        assertNull(activeAlerts.stream().filter(alert -> alert.getId().equals(cancelled.getId())).findFirst().orElse(null));
    }

    @Test
    void findActiveBySymbolReturnsOnlyMatchingActiveAlerts() {
        CryptoAlert btc = repository.save(CryptoAlert.createNew("btcusdt", new BigDecimal("42000"), AlertCondition.ABOVE))
                .await().indefinitely();
        repository.save(CryptoAlert.createNew("ethusdt", new BigDecimal("3000"), AlertCondition.ABOVE))
                .await().indefinitely();

        var btcAlerts = repository.findActiveBySymbol(" btcusdt ").collect().asList().await().indefinitely();

        assertEquals(1, btcAlerts.stream().filter(alert -> alert.getId().equals(btc.getId())).count());
    }

    @Test
    void updatePersistsChangedStatus() {
        CryptoAlert alert = repository.save(CryptoAlert.createNew("xrpusdt", new BigDecimal("0.5"), AlertCondition.BELOW))
                .await().indefinitely();

        alert.cancel();
        repository.update(alert).await().indefinitely();

        CryptoAlert updated = repository.findById(alert.getId()).await().indefinitely();
        assertEquals(AlertStatus.CANCELLED, updated.getStatus());
    }
}
